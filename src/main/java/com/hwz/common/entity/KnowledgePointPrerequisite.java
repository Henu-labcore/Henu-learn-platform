package com.hwz.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 知识点前置关系实体。
 */
@TableName("knowledge_point_prerequisite")
public class KnowledgePointPrerequisite {

    @TableId(
            value = "id",
            type = IdType.AUTO
    )
    private Long id;

    @TableField("knowledge_point_id")
    private Long knowledgePointId;

    @TableField("prerequisite_id")
    private Long prerequisiteId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKnowledgePointId() {
        return knowledgePointId;
    }

    public void setKnowledgePointId(
            Long knowledgePointId) {
        this.knowledgePointId =
                knowledgePointId;
    }

    public Long getPrerequisiteId() {
        return prerequisiteId;
    }

    public void setPrerequisiteId(
            Long prerequisiteId) {
        this.prerequisiteId =
                prerequisiteId;
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