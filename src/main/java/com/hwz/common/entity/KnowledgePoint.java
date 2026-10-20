package com.hwz.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识点实体。
 */
@TableName("knowledge_point")
public class KnowledgePoint {

    @TableId(
            value = "knowledge_point_id",
            type = IdType.AUTO
    )
    private Long knowledgePointId;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("category")
    private String category;

    @TableField("difficulty")
    private String difficulty;

    @TableField("status")
    private String status;

    @TableField("warning_threshold")
    private BigDecimal warningThreshold;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getKnowledgePointId() {
        return knowledgePointId;
    }

    public void setKnowledgePointId(
            Long knowledgePointId) {
        this.knowledgePointId =
                knowledgePointId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(
            String description) {
        this.description =
                description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(
            String category) {
        this.category =
                category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(
            String difficulty) {
        this.difficulty =
                difficulty;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status) {
        this.status = status;
    }

    public BigDecimal getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(
            BigDecimal warningThreshold) {
        this.warningThreshold =
                warningThreshold;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(
            Integer sortOrder) {
        this.sortOrder =
                sortOrder;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(
            Long createdBy) {
        this.createdBy =
                createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {
        this.createdAt =
                createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt) {
        this.updatedAt =
                updatedAt;
    }
}