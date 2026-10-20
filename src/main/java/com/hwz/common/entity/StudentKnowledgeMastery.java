package com.hwz.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生知识点掌握度与薄弱预警实体。
 */
@TableName("student_knowledge_mastery")
public class StudentKnowledgeMastery {

    @TableId(
            value = "mastery_id",
            type = IdType.AUTO
    )
    private Long masteryId;

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

    @TableField("completed_learning_count")
    private Integer completedLearningCount;

    @TableField("total_learning_count")
    private Integer totalLearningCount;

    @TableField("graded_assignment_count")
    private Integer gradedAssignmentCount;

    @TableField("total_assignment_count")
    private Integer totalAssignmentCount;

    @TableField("last_learning_time")
    private LocalDateTime lastLearningTime;

    @TableField("last_assessment_time")
    private LocalDateTime lastAssessmentTime;

    @TableField("calculated_at")
    private LocalDateTime calculatedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getMasteryId() {
        return masteryId;
    }

    public void setMasteryId(
            Long masteryId) {
        this.masteryId = masteryId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(
            Long studentId) {
        this.studentId = studentId;
    }

    public Long getKnowledgePointId() {
        return knowledgePointId;
    }

    public void setKnowledgePointId(
            Long knowledgePointId) {
        this.knowledgePointId =
                knowledgePointId;
    }

    public BigDecimal getLearningScore() {
        return learningScore;
    }

    public void setLearningScore(
            BigDecimal learningScore) {
        this.learningScore =
                learningScore;
    }

    public BigDecimal getAssessmentScore() {
        return assessmentScore;
    }

    public void setAssessmentScore(
            BigDecimal assessmentScore) {
        this.assessmentScore =
                assessmentScore;
    }

    public BigDecimal getMasteryScore() {
        return masteryScore;
    }

    public void setMasteryScore(
            BigDecimal masteryScore) {
        this.masteryScore =
                masteryScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(
            String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Integer getCompletedLearningCount() {
        return completedLearningCount;
    }

    public void setCompletedLearningCount(
            Integer completedLearningCount) {
        this.completedLearningCount =
                completedLearningCount;
    }

    public Integer getTotalLearningCount() {
        return totalLearningCount;
    }

    public void setTotalLearningCount(
            Integer totalLearningCount) {
        this.totalLearningCount =
                totalLearningCount;
    }

    public Integer getGradedAssignmentCount() {
        return gradedAssignmentCount;
    }

    public void setGradedAssignmentCount(
            Integer gradedAssignmentCount) {
        this.gradedAssignmentCount =
                gradedAssignmentCount;
    }

    public Integer getTotalAssignmentCount() {
        return totalAssignmentCount;
    }

    public void setTotalAssignmentCount(
            Integer totalAssignmentCount) {
        this.totalAssignmentCount =
                totalAssignmentCount;
    }

    public LocalDateTime getLastLearningTime() {
        return lastLearningTime;
    }

    public void setLastLearningTime(
            LocalDateTime lastLearningTime) {
        this.lastLearningTime =
                lastLearningTime;
    }

    public LocalDateTime getLastAssessmentTime() {
        return lastAssessmentTime;
    }

    public void setLastAssessmentTime(
            LocalDateTime lastAssessmentTime) {
        this.lastAssessmentTime =
                lastAssessmentTime;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(
            LocalDateTime calculatedAt) {
        this.calculatedAt =
                calculatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}