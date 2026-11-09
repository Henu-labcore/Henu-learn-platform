package com.hwz.user.controller;

import com.hwz.common.context.BaseContext;
import com.hwz.user.dto.CategoryProgressDTO;
import com.hwz.user.dto.DashboardDTO;
import com.hwz.user.dto.DifficultyProgressDTO;
import com.hwz.user.dto.RecentItemDTO;
import com.hwz.user.dto.RecommendationDTO;
import com.hwz.user.dto.SummaryDTO;
import com.hwz.user.dto.TrendPointDTO;
import com.hwz.user.service.LearnAnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/learn-analytics")
public class LearnAnalyticsController {

    private final LearnAnalyticsService learnAnalyticsService;

    public LearnAnalyticsController(LearnAnalyticsService learnAnalyticsService) {
        this.learnAnalyticsService = learnAnalyticsService;
    }

    @GetMapping("/dashboard")
    public DashboardDTO dashboard() {
        Long userId = requireUserId();
        return learnAnalyticsService.getDashboard(userId);
    }

    @GetMapping("/summary")
    public SummaryDTO summary() {
        Long userId = requireUserId();
        return learnAnalyticsService.getSummary(userId);
    }

    @GetMapping("/by-category")
    public List<CategoryProgressDTO> byCategory() {
        Long userId = requireUserId();
        return learnAnalyticsService.getByCategory(userId);
    }

    @GetMapping("/by-difficulty")
    public List<DifficultyProgressDTO> byDifficulty() {
        Long userId = requireUserId();
        return learnAnalyticsService.getByDifficulty(userId);
    }

    @GetMapping("/completion-trend")
    public List<TrendPointDTO> completionTrend(@RequestParam(required = false) Integer days) {
        Long userId = requireUserId();
        return learnAnalyticsService.getCompletionTrend(userId, days);
    }

    @GetMapping("/recent")
    public List<RecentItemDTO> recent(@RequestParam(required = false) Integer limit) {
        Long userId = requireUserId();
        return learnAnalyticsService.getRecent(userId, limit);
    }

    @GetMapping("/recommendation")
    public RecommendationDTO recommendation() {
        Long userId = requireUserId();
        return learnAnalyticsService.getRecommendation(userId);
    }

    private Long requireUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        return userId;
    }
}
