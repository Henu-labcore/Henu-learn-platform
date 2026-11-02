package com.hwz.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生知识点掌握度历史快照。
 *
 * <p>与 {@link StudentKnowledgeMastery} 的区别：
 * 后者是"当前值"（每人每知识点一行，重算覆盖），
 * 本表按天保留，用于掌握度趋势图与热力图。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("student_mastery_history")
public class StudentMasteryHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "history_id", type = IdType.AUTO)
    private Long historyId;

    @TableField("student_id")
    private Long studentId;

    @TableField("knowledge_point_id")
    private Long knowledgePointId;

    @TableField("learning_score")
    private BigDecimal learningScore;

    @TableField("assessment_score")
    private BigDecimal assessmentScore;

    @TableField("mastery_score")
    private BigDecimal masteryScore;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("snapshot_date")
    private LocalDate snapshotDate;

    @TableField("calculated_at")
    private LocalDateTime calculatedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
