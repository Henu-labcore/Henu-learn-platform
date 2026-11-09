package com.hwz.learningplan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("learning_plan")
public class LearningPlan {
    /** 新模块版本号，用于阻止跨设备覆盖旧版本的计划。 */
    private Integer version;
    @TableId(value = "plan_id", type = IdType.AUTO)
    private Long planId;
    @TableField("user_id")
    private Long userId;
    @TableField("plan_name")
    private String planName;
    @TableField("start_date")
    private LocalDate startDate;
    @TableField("end_date")
    private LocalDate endDate;
    @TableField("status")
    private String status;
    @TableField(value = "active_marker", updateStrategy = FieldStrategy.ALWAYS)
    private Integer activeMarker;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
//计划主表实体
