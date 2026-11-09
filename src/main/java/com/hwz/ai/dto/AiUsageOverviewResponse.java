package com.hwz.ai.dto;

import lombok.Data;

/** 管理端 AI 用量概览：只展示汇总数字，不包含学生问题或回答正文。 */
@Data
public class AiUsageOverviewResponse {
    private long totalRequests;
    private long todayRequests;
    private long successfulRequests;
    private long failedRequests;
    private long totalTokens;
    private long averageLatencyMs;
}
