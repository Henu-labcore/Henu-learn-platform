package com.hwz.learningplan.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearningPlanRiskEvaluatorTest {
    private final LearningPlanRiskEvaluator evaluator = new LearningPlanRiskEvaluator(3, 7);
    private final LocalDate today = LocalDate.of(2026, 7, 17);

    @Test
    void completedItemNeverRaisesRisk() {
        assertEquals(LearningPlanRiskEvaluator.RiskLevel.NORMAL,
                evaluator.evaluate(today, today.minusDays(2), true, null, today.minusDays(20)));
    }

    @Test
    void overdueHasHighestPriority() {
        assertEquals(LearningPlanRiskEvaluator.RiskLevel.OVERDUE,
                evaluator.evaluate(today, today.minusDays(1), false, null, today.minusDays(20)));
    }

    @Test
    void targetTodayAndThirdDayAreDueSoon() {
        assertEquals(LearningPlanRiskEvaluator.RiskLevel.DUE_SOON,
                evaluator.evaluate(today, today, false, null, today));
        assertEquals(LearningPlanRiskEvaluator.RiskLevel.DUE_SOON,
                evaluator.evaluate(today, today.plusDays(3), false, null, today));
    }

    @Test
    void sevenDaysWithoutActivityIsStalled() {
        LocalDateTime lastActivity = today.minusDays(7).atTime(9, 0);
        assertEquals(LearningPlanRiskEvaluator.RiskLevel.STALLED,
                evaluator.evaluate(today, today.plusDays(10), false, lastActivity, today));
    }

    @Test
    void recentActivityAndDistantTargetRemainNormal() {
        assertEquals(LearningPlanRiskEvaluator.RiskLevel.NORMAL,
                evaluator.evaluate(today, today.plusDays(10), false, today.minusDays(2).atStartOfDay(), today));
    }
}

