package com.hwz.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学习资源与知识点关联实体。
 */
@TableName("learning_item_knowledge_point")
public class LearningItemKnowledgePoint {

    @TableId(
            value = "id",
            type = IdType.AUTO
    )
    private Long id;

    @TableField("item_pk")
    private Long itemPk;

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

    public Long getItemPk() {
        return itemPk;
    }

    public void setItemPk(Long itemPk) {
        this.itemPk = itemPk;
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