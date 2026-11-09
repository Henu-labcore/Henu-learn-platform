package com.hwz.ai.controller;

import com.hwz.ai.dto.AiPopularItemsResponse;
import com.hwz.ai.dto.AiUsageOverviewResponse;
import com.hwz.ai.service.AiQuestionAnalyticsService;
import com.hwz.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端高频疑问统计接口。 */
@RestController
@RequestMapping("/admin/ai-assistant")
public class AiQuestionAnalyticsController {
    private final AiQuestionAnalyticsService analyticsService;

    public AiQuestionAnalyticsController(AiQuestionAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/popular-items")
    public Result<AiPopularItemsResponse> popularItems() {
        return Result.ok(analyticsService.popularItems());
    }

    @GetMapping("/usage-overview")
    public Result<AiUsageOverviewResponse> usageOverview() {
        return Result.ok(analyticsService.usageOverview());
    }
}
