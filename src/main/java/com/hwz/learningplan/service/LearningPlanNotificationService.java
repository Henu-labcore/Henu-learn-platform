package com.hwz.learningplan.service;

import com.hwz.learningplan.dto.LearningPlanDtos.DetailResponse;
import com.hwz.learningplan.dto.LearningPlanDtos.ItemResponse;
import com.hwz.learningplan.dto.LearningPlanReminderSetting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** 站内提醒独立执行器，异常隔离，不占用原考核清理任务的调度线程。 */
@Service
public class LearningPlanNotificationService {
    private static final Logger LOG=LoggerFactory.getLogger(LearningPlanNotificationService.class);
    private final JdbcTemplate db;private final LearningPlanService plans;private final Clock clock;
    private final boolean backgroundEnabled;private final ScheduledExecutorService executor;
    private long cursor=0;
    public LearningPlanNotificationService(JdbcTemplate db,LearningPlanService plans,@Qualifier("learningPlanClock") Clock clock,@Value("${labcore.learning-plan.notifications-enabled:true}") boolean enabled){
        this.db=db;this.plans=plans;this.clock=clock;this.backgroundEnabled=enabled;
        this.executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"learning-plan-notifications");t.setDaemon(true);return t;});
    }
    @PostConstruct void start(){if(backgroundEnabled)executor.scheduleWithFixedDelay(()->{try{scanOnce();}catch(Exception ex){LOG.warn("学习计划提醒扫描失败，原业务不受影响",ex);}},30,60,TimeUnit.SECONDS);}
    @PreDestroy void stop(){executor.shutdownNow();}
    public LearningPlanReminderSetting setting(Long user){
        List<Map<String,Object>> rows=db.queryForList("SELECT enabled,due_days FROM learning_plan_reminder_setting WHERE user_id=?",user);
        LearningPlanReminderSetting result=new LearningPlanReminderSetting();result.setEnabled(true);result.setDueDays(1);
        if(!rows.isEmpty()){result.setEnabled(((Number)rows.get(0).get("enabled")).intValue()!=0);result.setDueDays(((Number)rows.get(0).get("due_days")).intValue());}return result;
    }
    public LearningPlanReminderSetting saveSetting(Long user,LearningPlanReminderSetting req){
        if(req==null||req.getEnabled()==null||req.getDueDays()==null||req.getDueDays()<1||req.getDueDays()>7)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"提醒提前天数应为1至7天");
        db.update("INSERT INTO learning_plan_reminder_setting(user_id,enabled,due_days) VALUES(?,?,?) ON DUPLICATE KEY UPDATE enabled=VALUES(enabled),due_days=VALUES(due_days)",user,req.getEnabled()?1:0,req.getDueDays());return setting(user);
    }
    /** 分页游标轮转，每轮最多100份计划，防止一次扫描整个系统。唯一键负责跨实例去重。 */
    public synchronized void scanOnce(){
        List<Map<String,Object>> batch=db.queryForList("SELECT p.plan_id,p.user_id FROM learning_plan p JOIN sys_user u ON u.user_id=p.user_id AND u.status='ACTIVE' WHERE p.status IN ('ACTIVE','COMPLETED') AND p.plan_id>? ORDER BY p.plan_id LIMIT 100",cursor);
        if(batch.isEmpty()){cursor=0;return;}
        for(Map<String,Object> row:batch){long id=((Number)row.get("plan_id")).longValue(),user=((Number)row.get("user_id")).longValue();cursor=id;
            try{plans.synchronizeActivePlan(user);generate(user,plans.getById(user,id));}catch(Exception ex){LOG.warn("学习计划 {} 提醒处理失败",id,ex);}
        }
        if(batch.size()<100)cursor=0;
    }
    private void generate(Long user,DetailResponse plan){
        if("CANCELLED".equals(plan.getStatus()))return;
        LearningPlanReminderSetting pref=setting(user);LocalDate today=LocalDate.now(clock);
        for(ItemResponse item:plan.getItems()){
            if(item.isCompleted()){
                insert(user,plan.getPlanId(),item.getItemPk(),"ITEM_COMPLETED",item.getCompleteTime().toLocalDate(),"任务完成："+item.getTitle(),"已完成，可填写学习心得和下一步安排。");continue;
            }
            if(!Boolean.TRUE.equals(pref.getEnabled())||!"ACTIVE".equals(plan.getStatus())||Boolean.FALSE.equals(item.getAvailable()))continue;
            if(today.isAfter(item.getTargetDate()))insert(user,plan.getPlanId(),item.getItemPk(),"OVERDUE",today,"学习任务已逾期："+item.getTitle(),"目标日期："+item.getTargetDate()+"，请调整安排或继续学习。");
            else if(!today.isBefore(plan.getStartDate())&&!item.getTargetDate().isAfter(today.plusDays(pref.getDueDays())))insert(user,plan.getPlanId(),item.getItemPk(),"DUE_SOON",today,"学习任务即将到期："+item.getTitle(),"目标日期："+item.getTargetDate()+"，请及时完成。");
        }
        if(plan.getTotalCount()>0&&plan.getCompletedCount()==plan.getTotalCount())insert(user,plan.getPlanId(),0L,"PLAN_COMPLETED",plan.getUpdatedAt().toLocalDate(),"学习计划已完成："+plan.getPlanName(),"全部任务已完成，可以提交计划总结。");
    }
    private void insert(Long user,Long plan,Long item,String type,LocalDate date,String title,String message){
        db.update("INSERT IGNORE INTO learning_plan_notification(user_id,plan_id,item_pk,type,event_date,title,message,is_read) VALUES(?,?,?,?,?,?,?,0)",user,plan,item,type,date,title,message);
    }
    public List<Map<String,Object>> list(Long user){
        // 查询时再次排除已完成/取消/失效任务的催办通知，扫描间隔内也不会误提醒。
        LocalDate today=LocalDate.now(clock);
        // 日期使用模块时钟，不依赖数据库服务器时区；调整截止日期后旧催办立即失效。
        return db.queryForList("SELECT n.id,n.plan_id AS planId,n.item_pk AS itemPk,n.type,n.title,n.message,n.is_read AS isRead,n.created_at AS createdAt FROM learning_plan_notification n JOIN learning_plan p ON p.plan_id=n.plan_id AND p.user_id=n.user_id LEFT JOIN learning_record lr ON lr.user_id=n.user_id AND lr.item_pk=n.item_pk LEFT JOIN learning_item li ON li.item_pk=n.item_pk LEFT JOIN learning_plan_reminder_setting s ON s.user_id=n.user_id WHERE n.user_id=? AND p.status<>'CANCELLED' AND (n.type IN ('ITEM_COMPLETED','PLAN_COMPLETED') OR (p.status='ACTIVE' AND lr.complete_time IS NULL AND li.status='PUBLISHED' AND n.event_date=? AND COALESCE(s.enabled,1)=1 AND EXISTS(SELECT 1 FROM learning_plan_item pi WHERE pi.plan_id=n.plan_id AND pi.item_pk=n.item_pk AND ((n.type='OVERDUE' AND pi.target_date<?) OR (n.type='DUE_SOON' AND pi.target_date>=? AND pi.target_date<=DATE_ADD(?,INTERVAL COALESCE(s.due_days,1) DAY)))))) ORDER BY n.created_at DESC,n.id DESC LIMIT 100",user,today,today,today,today);
    }
    public void read(Long user,Long id){db.update("UPDATE learning_plan_notification SET is_read=1 WHERE user_id=? AND id=?",user,id);}
    public void readAll(Long user){db.update("UPDATE learning_plan_notification SET is_read=1 WHERE user_id=?",user);}
}
