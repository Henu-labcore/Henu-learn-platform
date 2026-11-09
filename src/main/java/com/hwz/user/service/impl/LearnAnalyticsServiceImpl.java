package com.hwz.user.service.impl;

import com.hwz.user.dto.CategoryProgressDTO;
import com.hwz.user.dto.DashboardDTO;
import com.hwz.user.dto.DifficultyProgressDTO;
import com.hwz.user.dto.RecentItemDTO;
import com.hwz.user.dto.RecommendationDTO;
import com.hwz.user.dto.SummaryDTO;
import com.hwz.user.dto.TrendPointDTO;
import com.hwz.user.mapper.LearnAnalyticsMapper;
import com.hwz.user.service.LearnAnalyticsService;
import com.hwz.user.util.DurationTextParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Service
public class LearnAnalyticsServiceImpl implements LearnAnalyticsService {

    private static final int DEFAULT_DAYS = 30;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_DAYS = 365;
    private static final int MAX_LIMIT = 100;

    private final LearnAnalyticsMapper learnAnalyticsMapper;

    public LearnAnalyticsServiceImpl(LearnAnalyticsMapper learnAnalyticsMapper) {
        this.learnAnalyticsMapper = learnAnalyticsMapper;
    }

    @Override
    public DashboardDTO getDashboard(Long userId) {
        SummaryDTO summary = getSummary(userId);
        List<CategoryProgressDTO> byCategory = getByCategory(userId);
        List<DifficultyProgressDTO> byDifficulty = getByDifficulty(userId);
        List<TrendPointDTO> completionTrend = getCompletionTrend(userId, DEFAULT_DAYS);
        List<RecentItemDTO> recent = getRecent(userId, DEFAULT_LIMIT);
        RecommendationDTO recommendation = getRecommendation(userId);

        return DashboardDTO.builder()
                .summary(summary)
                .byCategory(byCategory)
                .byDifficulty(byDifficulty)
                .completionTrend(completionTrend)
                .recent(recent)
                .recommendation(recommendation)
                .build();
    }

    @Override
    public SummaryDTO getSummary(Long userId) {
        return learnAnalyticsMapper.selectSummary(userId);
    }

    @Override
    public List<CategoryProgressDTO> getByCategory(Long userId) {
        List<CategoryProgressDTO> result = learnAnalyticsMapper.selectByCategory(userId);
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public List<DifficultyProgressDTO> getByDifficulty(Long userId) {
        List<DifficultyProgressDTO> result = learnAnalyticsMapper.selectByDifficulty(userId);
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public List<TrendPointDTO> getCompletionTrend(Long userId, Integer days) {
        int resolvedDays = normalizeDays(days);
        java.time.LocalDate end = java.time.LocalDate.now();
        java.time.LocalDate start = end.minusDays(resolvedDays - 1L);
        List<TrendPointDTO> raw = learnAnalyticsMapper.selectCompletionTrend(userId, start);
        if (raw == null) {
            raw = Collections.emptyList();
        }
        return fillTrendPoints(raw, resolvedDays);
    }

    @Override
    public List<RecentItemDTO> getRecent(Long userId, Integer limit) {
        int resolvedLimit = normalizeLimit(limit);
        List<RecentItemDTO> result = learnAnalyticsMapper.selectRecent(userId, resolvedLimit);
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public RecommendationDTO getRecommendation(Long userId) {
        RecommendationDTO recommendation = learnAnalyticsMapper.selectRecommendationInProgress(userId);
        if (recommendation != null) {
            return enrichRecommendation(recommendation);
        }
        return enrichRecommendation(learnAnalyticsMapper.selectRecommendationRecent(userId));
    }

    private int normalizeDays(Integer days) {
        if (days == null) {
            return DEFAULT_DAYS;
        }
        if (days <= 0 || days > MAX_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid days/limit");
        }
        return days;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0 || limit > MAX_LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid days/limit");
        }
        return limit;
    }

    private List<TrendPointDTO> fillTrendPoints(List<TrendPointDTO> raw, int days) {
        if (days <= 0) {
            return Collections.emptyList();
        }
        java.util.Map<java.time.LocalDate, Long> lookup = new java.util.HashMap<>();
        for (TrendPointDTO point : raw) {
            if (point != null && point.getDate() != null) {
                lookup.put(point.getDate(), point.getCompletedCount());
            }
        }
        java.time.LocalDate end = java.time.LocalDate.now();
        java.time.LocalDate start = end.minusDays(days - 1L);
        List<TrendPointDTO> filled = new java.util.ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            java.time.LocalDate date = start.plusDays(i);
            Long count = lookup.getOrDefault(date, 0L);
            TrendPointDTO point = new TrendPointDTO();
            point.setDate(date);
            point.setCompletedCount(count);
            filled.add(point);
        }
        return filled;
    }

    private RecommendationDTO enrichRecommendation(RecommendationDTO recommendation) {
        if (recommendation == null) {
            return null;
        }
        if (recommendation.getExpectedDurationSec() == null) {
            DurationTextParser.parseSeconds(recommendation.getDuration())
                    .ifPresent(recommendation::setExpectedDurationSec);
        }
        return recommendation;
    }
}
