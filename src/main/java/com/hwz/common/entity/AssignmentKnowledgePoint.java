package com.hwz.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考核与知识点关联实体。
 */
@TableName("assignment_knowledge_point")
public class AssignmentKnowledgePoint {

    @TableId(
            value = "id",
            type = IdType.AUTO
    )
    private Long id;

    @TableField("assignment_id")
    private Long assignmentId;

    @TableField("knowledge_point_id")
    private Long knowledgePointId;

    @TableField("weight")
    private BigDecimal weight;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(
            Long assignmentId) {
        this.assignmentId =
                assignmentId;
    }

    public Long getKnowledgePointId() {
        return knowledgePointId;
    }

    public void setKnowledgePointId(
            Long knowledgePointId) {
        this.knowledgePointId =
                knowledgePointId;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(
            BigDecimal weight) {
        this.weight = weight;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {
        this.createdAt =
                createdAt;
    }
}