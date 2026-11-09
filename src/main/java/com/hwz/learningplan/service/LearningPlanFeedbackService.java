package com.hwz.learningplan.service;

import com.hwz.learningplan.dto.LearningPlanFeedbackRequest;
import com.hwz.learningplan.dto.LearningPlanDtos.DetailResponse;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

/** 学习计划完成反馈：读取原进度，只向本模块反馈表写入。 */
@Service
public class LearningPlanFeedbackService {
    private final LearningPlanService plans;private final JdbcTemplate db;
    public LearningPlanFeedbackService(LearningPlanService plans,JdbcTemplate db){this.plans=plans;this.db=db;}
    public List<Map<String,Object>> list(Long user,Long plan){plans.getById(user,plan);return db.queryForList("SELECT item_pk AS itemPk,reflection,difficulty,next_step AS nextStep,rating,version,updated_at AS updatedAt FROM learning_plan_feedback WHERE user_id=? AND plan_id=? ORDER BY item_pk",user,plan);}
    @Transactional public List<Map<String,Object>> save(Long user,Long plan,Long item,LearningPlanFeedbackRequest req){
        DetailResponse detail=plans.getById(user,plan);
        // itemPk=0 表示整份计划总结；其余必须是该计划中已经完成的任务。
        boolean completed=item!=null&&(item==0?detail.getTotalCount()>0&&detail.getCompletedCount()==detail.getTotalCount():detail.getItems().stream().anyMatch(i->item.equals(i.getItemPk())&&i.isCompleted()));
        if(!completed)throw new ResponseStatusException(HttpStatus.CONFLICT,"完成任务或计划后才能提交完成反馈");
        if(req==null||req.getVersion()==null||req.getRating()==null||req.getRating()<1||req.getRating()>5)bad("请填写1至5分自评及反馈版本");
        String reflection=text(req.getReflection(),2000),difficulty=text(req.getDifficulty(),1000),next=text(req.getNextStep(),1000);
        if(reflection.trim().isEmpty())bad("请填写学习心得");
        if(req.getVersion()==0){try{db.update("INSERT INTO learning_plan_feedback(user_id,plan_id,item_pk,reflection,difficulty,next_step,rating,version) VALUES(?,?,?,?,?,?,?,1)",user,plan,item,reflection,difficulty,next,req.getRating());}catch(DuplicateKeyException ex){conflict();}}
        else if(db.update("UPDATE learning_plan_feedback SET reflection=?,difficulty=?,next_step=?,rating=?,version=version+1 WHERE user_id=? AND plan_id=? AND item_pk=? AND version=?",reflection,difficulty,next,req.getRating(),user,plan,item,req.getVersion())!=1)conflict();
        return list(user,plan);
    }
    private static String text(String value,int max){String t=value==null?"":value.trim();if(t.length()>max)bad("反馈内容超过字数限制");return t;}
    private static void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private static void conflict(){throw new ResponseStatusException(HttpStatus.CONFLICT,"反馈已在其他设备更新，请重新打开后再保存");}
}
