package com.hwz.learningplan.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class LearningPlanRiskEvaluator {
    public enum RiskLevel { NORMAL, STALLED, DUE_SOON, OVERDUE }

    private final int dueSoonDays;
    private final int stalledDays;

    public LearningPlanRiskEvaluator(
            @Value("${labcore.learning-plan.due-soon-days:3}") int dueSoonDays,
            @Value("${labcore.learning-plan.stalled-days:7}") int stalledDays) {
        this.dueSoonDays = dueSoonDays;
        this.stalledDays = stalledDays;
    }

    public RiskLevel evaluate(LocalDate today,
                              LocalDate targetDate,
                              boolean completed,
                              LocalDateTime lastActivityTime,
                              LocalDate fallbackActivityDate) {
        if (completed) return RiskLevel.NORMAL;
        if (targetDate.isBefore(today)) return RiskLevel.OVERDUE;
        if (!targetDate.isAfter(today.plusDays(dueSoonDays))) return RiskLevel.DUE_SOON;
        LocalDate activityDate = lastActivityTime == null ? fallbackActivityDate : lastActivityTime.toLocalDate();
        if (activityDate != null && !today.isBefore(activityDate.plusDays(stalledDays))) {
            return RiskLevel.STALLED;
        }
        return RiskLevel.NORMAL;
    }

    public RiskLevel max(RiskLevel left, RiskLevel right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}
// 业务逻辑（创建/更新/取消/风险）
