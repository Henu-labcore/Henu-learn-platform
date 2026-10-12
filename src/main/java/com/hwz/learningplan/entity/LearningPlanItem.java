package com.hwz.learningplan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("learning_plan_item")
public class LearningPlanItem {
    @TableId(value = "plan_item_id", type = IdType.AUTO)
    private Long planItemId;
    @TableField("plan_id")
    private Long planId;
    @TableField("item_pk")
    private Long itemPk;
    @TableField("target_date")
    private LocalDate targetDate;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
//计划条目实体
