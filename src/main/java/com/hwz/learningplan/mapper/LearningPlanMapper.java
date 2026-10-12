package com.hwz.learningplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hwz.learningplan.entity.LearningPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;

@Mapper
public interface LearningPlanMapper extends BaseMapper<LearningPlan> {
    /** 仅锁本模块计划行；两台设备同时修改时必须重新检查版本。 */
    @Select("SELECT * FROM learning_plan WHERE plan_id = #{id} FOR UPDATE")
    LearningPlan selectForUpdate(@Param("id") Long id);
    /** 完成同步仅改变状态，且匹配版本；不能覆盖另一设备修改的名称/任务安排。 */
    @Update("UPDATE learning_plan SET status='COMPLETED',active_marker=NULL,version=version+1,updated_at=#{time} WHERE plan_id=#{id} AND status='ACTIVE' AND version=#{version}")
    int markCompleted(@Param("id") Long id,@Param("version") Integer version,@Param("time") LocalDateTime time);
}
//条目表数据访问
