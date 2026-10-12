package com.hwz.modules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwz.common.auth.AccessTokenService;
import com.hwz.learningplan.service.LearningPlanNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 真实 MySQL + HTTP 验收。只能显式指定独立测试库，不允许对日常库运行写入测试。 */
@EnabledIfEnvironmentVariable(named="LABCORE_MODULE_TEST_DB", matches="labcore_module_check_.*")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties={
    "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/${LABCORE_MODULE_TEST_DB}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
    "labcore.learning-plan.notifications-enabled=false",
    // 仅测试上下文使用；不改生产鉴权配置，不在测试输出中打印令牌。
    "labcore.auth.access-token-secret=learning-modules-integration-test-only"
})
class LearningModulesIntegrationTest {
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate db;
    @Autowired AccessTokenService tokens;
    @Autowired ObjectMapper json;
    @Autowired LearningPlanNotificationService notifications;

    private List<Long> users() {
        return db.queryForList("SELECT user_id FROM sys_user WHERE status='ACTIVE' AND role='USER' ORDER BY user_id LIMIT 2", Long.class);
    }
    private Long availableItem(Long user) {
        return db.queryForObject("SELECT li.item_pk FROM learning_item li LEFT JOIN learning_record lr ON li.item_pk=lr.item_pk AND lr.user_id=? WHERE li.status='PUBLISHED' AND lr.complete_time IS NULL ORDER BY li.item_pk LIMIT 1", Long.class, user);
    }
    private ResponseEntity<String> call(Long user, HttpMethod method, String path, Object body) {
        HttpHeaders headers=new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        if(user!=null) headers.setBearerAuth(tokens.issue(user));
        return http.exchange("/api"+path,method,new HttpEntity<>(body,headers),String.class);
    }
    private JsonNode data(ResponseEntity<String> response) throws Exception {
        assertEquals(HttpStatus.OK,response.getStatusCode(),response.getBody());
        JsonNode root=json.readTree(response.getBody()); assertTrue(root.path("success").asBoolean(),response.getBody());
        return root.has("data") ? root.path("data") : json.nullNode();
    }

    @Test void newPlanRejectsAllCompletedAndKeepsMixedPlanRiskWithoutResettingRecords() throws Exception {
        Long user=users().get(0),pending=availableItem(user);
        Long completed=db.queryForObject("SELECT li.item_pk FROM learning_item li JOIN learning_record lr ON lr.item_pk=li.item_pk AND lr.user_id=? WHERE li.status='PUBLISHED' AND lr.complete_time IS NOT NULL ORDER BY li.item_pk LIMIT 1",Long.class,user);
        // 仅清理克隆库里的模块测试计划，不清理/重置原学习记录。
        db.update("DELETE FROM learning_plan_item WHERE plan_id IN (SELECT plan_id FROM learning_plan WHERE user_id=?)",user);
        db.update("DELETE FROM learning_plan WHERE user_id=?",user);
        List<Map<String,Object>> originalRecords=db.queryForList("SELECT * FROM learning_record WHERE user_id=? ORDER BY item_pk",user);
        LocalDate today=LocalDate.now();Map<String,Object> doneTask=new HashMap<>();doneTask.put("itemPk",completed);doneTask.put("targetDate",today.plusDays(2).toString());
        Map<String,Object> body=new HashMap<>();body.put("planName","创建前完成状态验收");body.put("startDate",today.toString());body.put("endDate",today.plusDays(7).toString());body.put("items",Collections.singletonList(doneTask));
        assertEquals(HttpStatus.CONFLICT,call(user,HttpMethod.POST,"/learning-plans",body).getStatusCode());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM learning_plan WHERE user_id=?",Integer.class,user));
        Map<String,Object> pendingTask=new HashMap<>();pendingTask.put("itemPk",pending);pendingTask.put("targetDate",today.plusDays(2).toString());
        body.put("items",Arrays.asList(doneTask,pendingTask));
        JsonNode created=data(call(user,HttpMethod.POST,"/learning-plans",body));
        JsonNode current=data(call(user,HttpMethod.GET,"/learning-plans/current",null));
        assertEquals(created.path("planId").asLong(),current.path("planId").asLong());
        assertEquals("ACTIVE",current.path("status").asText());assertEquals(1,current.path("completedCount").asInt());assertEquals(50,current.path("progressRate").asInt());
        assertEquals("DUE_SOON",current.path("riskLevel").asText());assertEquals(1,current.path("dueSoonCount").asInt());
        assertEquals(originalRecords,db.queryForList("SELECT * FROM learning_record WHERE user_id=? ORDER BY item_pk",user));
        data(call(user,HttpMethod.POST,"/learning-plans/"+created.path("planId").asLong()+"/cancel",null));
    }

    @Test void favoritePersistenceAndAccountIsolation() throws Exception {
        List<Long> u=users();Long user=u.get(0),other=u.get(1),item=availableItem(user);
        call(user,HttpMethod.DELETE,"/favorites/"+item,null);
        Map<String,Object> body=new HashMap<>();body.put("itemPk",item);
        JsonNode first=data(call(user,HttpMethod.POST,"/favorites",body));
        assertEquals(first.path("id").asLong(),data(call(user,HttpMethod.POST,"/favorites",body)).path("id").asLong());
        // 每次 call 签发独立登录令牌，模拟另一台设备访问同一账号。
        assertTrue(data(call(user,HttpMethod.GET,"/favorites",null)).size()>0);
        assertEquals(HttpStatus.UNAUTHORIZED,call(null,HttpMethod.GET,"/favorites",null).getStatusCode());
        Map<String,Object> edit=new HashMap<>();edit.put("version",1);edit.put("note","隔离检查");
        assertEquals(HttpStatus.NOT_FOUND,call(other,HttpMethod.PUT,"/favorites/"+item,edit).getStatusCode());
        call(other,HttpMethod.DELETE,"/favorites/"+item,null);
        assertTrue(data(call(user,HttpMethod.GET,"/favorites",null)).size()>0);
        Map<String,Object> legacy=new HashMap<>();legacy.put("confirmed",false);legacy.put("itemIds",Collections.singletonList(item));
        assertEquals(HttpStatus.BAD_REQUEST,call(user,HttpMethod.POST,"/favorites/import",legacy).getStatusCode());
        legacy.put("confirmed",true);assertEquals(1,data(call(user,HttpMethod.POST,"/favorites/import",legacy)).path("imported").asInt());
    }

    @Test void favoriteClassificationTagsSearchAndVersionConflict() throws Exception {
        Long user=users().get(0),item=availableItem(user);data(call(user,HttpMethod.POST,"/favorites",Collections.singletonMap("itemPk",item)));
        String name="验收分类-"+System.nanoTime();JsonNode labels=data(call(user,HttpMethod.POST,"/favorites/categories",Collections.singletonMap("name",name)));
        long category=0;for(JsonNode l:labels)if(name.equals(l.path("name").asText()))category=l.path("id").asLong();assertTrue(category>0);
        JsonNode all=data(call(user,HttpMethod.GET,"/favorites",null));JsonNode row=null;for(JsonNode f:all)if(f.path("itemPk").asLong()==item)row=f;assertNotNull(row);
        Map<String,Object> edit=new HashMap<>();edit.put("version",row.path("version").asInt());edit.put("categoryId",category);edit.put("tags",Arrays.asList("标签验收","复习"));edit.put("note","唯一检索备注");
        JsonNode saved=data(call(user,HttpMethod.PUT,"/favorites/"+item,edit));assertEquals(2,saved.path("tags").size());
        assertEquals(HttpStatus.CONFLICT,call(user,HttpMethod.PUT,"/favorites/"+item,edit).getStatusCode());
        assertEquals(1,data(call(user,HttpMethod.GET,"/favorites?keyword=唯一检索备注&categoryId="+category+"&tag=标签验收",null)).size());
        assertEquals(0,data(call(user,HttpMethod.GET,"/favorites?keyword=不存在的检索关键词",null)).size());
        data(call(user,HttpMethod.DELETE,"/favorites/categories/"+category,null));
        assertEquals(1,data(call(user,HttpMethod.GET,"/favorites?keyword=唯一检索备注&categoryId=0",null)).size());
    }

    @Test void remindersDeduplicateAndFeedbackDoesNotOverwriteOriginalRemark() throws Exception {
        Long user=users().get(0),other=users().get(1),item=availableItem(user);
        db.update("DELETE FROM learning_plan_item WHERE plan_id IN (SELECT plan_id FROM learning_plan WHERE user_id=?)",user);
        db.update("DELETE FROM learning_plan WHERE user_id=?",user);
        db.update("DELETE FROM learning_plan_notification WHERE user_id=?",user);
        LocalDate today=LocalDate.now();Map<String,Object> task=new HashMap<>();task.put("itemPk",item);task.put("targetDate",today.minusDays(1).toString());
        Map<String,Object> body=new HashMap<>();body.put("planName","提醒与反馈验收");body.put("startDate",today.minusDays(7).toString());body.put("endDate",today.plusDays(7).toString());body.put("items",Collections.singletonList(task));
        long plan=data(call(user,HttpMethod.POST,"/learning-plans",body)).path("planId").asLong();
        Map<String,Object> preference=new HashMap<>();preference.put("enabled",true);preference.put("dueDays",1);data(call(user,HttpMethod.PUT,"/learning-plans/reminder-setting",preference));
        notifications.scanOnce();JsonNode list=data(call(user,HttpMethod.GET,"/learning-plans/notifications",null));assertEquals(1,list.size());assertEquals("OVERDUE",list.get(0).path("type").asText());
        long notice=list.get(0).path("id").asLong();notifications.scanOnce();assertEquals(1,data(call(user,HttpMethod.GET,"/learning-plans/notifications",null)).size());
        data(call(other,HttpMethod.POST,"/learning-plans/notifications/"+notice+"/read",null));assertEquals(0,data(call(user,HttpMethod.GET,"/learning-plans/notifications",null)).get(0).path("isRead").asInt());
        data(call(user,HttpMethod.POST,"/learning-plans/notifications/"+notice+"/read",null));assertEquals(1,data(call(user,HttpMethod.GET,"/learning-plans/notifications",null)).get(0).path("isRead").asInt());
        preference.put("enabled",false);data(call(user,HttpMethod.PUT,"/learning-plans/reminder-setting",preference));assertEquals(0,data(call(user,HttpMethod.GET,"/learning-plans/notifications",null)).size());
        Map<String,Object> feedback=new HashMap<>();feedback.put("version",0);feedback.put("rating",4);feedback.put("reflection","学习心得验收");feedback.put("difficulty","难点");feedback.put("nextStep","复习");
        assertEquals(HttpStatus.CONFLICT,call(user,HttpMethod.PUT,"/learning-plans/"+plan+"/feedback/"+item,feedback).getStatusCode());
        // 只在克隆测试库模拟完成，不调用或修改日常库里的学习记录。
        db.update("INSERT INTO learning_record(user_id,item_pk,first_learn_time,complete_time,complete_remark,learn_duration_sec) VALUES(?,?,NOW(),NOW(),'原记录备注保留',3600) ON DUPLICATE KEY UPDATE complete_time=NOW(),complete_remark='原记录备注保留'",user,item);
        notifications.scanOnce();assertEquals("COMPLETED",data(call(user,HttpMethod.GET,"/learning-plans/"+plan,null)).path("status").asText());
        JsonNode completed=data(call(user,HttpMethod.GET,"/learning-plans/notifications",null));assertEquals(2,completed.size());for(JsonNode n:completed)assertNotEquals("OVERDUE",n.path("type").asText());
        notifications.scanOnce();assertEquals(2,data(call(user,HttpMethod.GET,"/learning-plans/notifications",null)).size());
        assertEquals(1,data(call(user,HttpMethod.PUT,"/learning-plans/"+plan+"/feedback/"+item,feedback)).size());
        assertEquals(HttpStatus.CONFLICT,call(user,HttpMethod.PUT,"/learning-plans/"+plan+"/feedback/"+item,feedback).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,call(other,HttpMethod.GET,"/learning-plans/"+plan+"/feedback",null).getStatusCode());
        assertEquals("原记录备注保留",db.queryForObject("SELECT complete_remark FROM learning_record WHERE user_id=? AND item_pk=?",String.class,user,item));
        assertEquals("学习心得验收",data(call(user,HttpMethod.GET,"/learning-plans/"+plan+"/feedback",null)).get(0).path("reflection").asText());
        data(call(user,HttpMethod.PUT,"/learning-plans/"+plan+"/feedback/0",feedback));
    }

    @Test void learningPlanPersistsProtectsOwnershipAndPreservesOriginalApis() throws Exception {
        List<Long> users=users(); assertEquals(2,users.size()); Long user=users.get(0), other=users.get(1), item=availableItem(user);
        db.update("DELETE FROM learning_plan_item WHERE plan_id IN (SELECT plan_id FROM learning_plan WHERE user_id=?)",user);
        db.update("DELETE FROM learning_plan WHERE user_id=?",user);
        LocalDate today=LocalDate.now(); Map<String,Object> task=new HashMap<>(); task.put("itemPk",item);task.put("targetDate",today.plusDays(3).toString());
        Map<String,Object> body=new HashMap<>();body.put("planName","集成测试计划");body.put("startDate",today.toString());body.put("endDate",today.plusDays(7).toString());body.put("items",Collections.singletonList(task));
        assertEquals(HttpStatus.UNAUTHORIZED,call(null,HttpMethod.GET,"/learning-plans/current",null).getStatusCode());
        JsonNode created=data(call(user,HttpMethod.POST,"/learning-plans",body)); long id=created.path("planId").asLong(); assertTrue(id>0);
        assertEquals(id,data(call(user,HttpMethod.GET,"/learning-plans/current",null)).path("planId").asLong());
        assertEquals(HttpStatus.FORBIDDEN,call(other,HttpMethod.GET,"/learning-plans/"+id,null).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,call(user,HttpMethod.GET,"/admin/learning-plan-risks",null).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,call(user,HttpMethod.POST,"/learning-plans",body).getStatusCode());
        body.put("version",created.path("version").asInt()); body.put("planName","设备一修改");
        assertEquals(2,data(call(user,HttpMethod.PUT,"/learning-plans/"+id,body)).path("version").asInt());
        assertEquals(HttpStatus.CONFLICT,call(user,HttpMethod.PUT,"/learning-plans/"+id,body).getStatusCode());
        assertTrue(data(call(user,HttpMethod.GET,"/learning-items",null)).isArray());
        assertTrue(data(call(user,HttpMethod.GET,"/learning-progress/user",null)).isArray());
        data(call(user,HttpMethod.POST,"/learning-plans/"+id+"/cancel",null));
        assertTrue(data(call(user,HttpMethod.GET,"/learning-plans/current",null)).isNull());
        assertTrue(data(call(user,HttpMethod.GET,"/learning-plans/history",null)).size()>0);
    }
}
