package com.hwz.learningplan.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class LearningPlanDtos {
    private LearningPlanDtos() {}

    @Data
    public static class SaveRequest {
        private Integer version;
        private String planName;
        private LocalDate startDate;
        private LocalDate endDate;
        private List<ItemRequest> items;
    }

    @Data
    public static class ItemRequest {
        private Long itemPk;
        private LocalDate targetDate;
        private Integer sortOrder;
    }

    @Data
    public static class ItemViewRow {
        private Boolean available;
        private Long planItemId;
        private Long itemPk;
        private String title;
        private String category;
        private String difficulty;
        private String duration;
        private LocalDate targetDate;
        private Integer sortOrder;
        private LocalDateTime completeTime;
        private Long learnDurationSec;
        private LocalDateTime lastActivityTime;
    }

    @Data
    public static class ItemResponse {
        private Boolean available;
        private Long planItemId;
        private Long itemPk;
        private String title;
        private String category;
        private String difficulty;
        private String duration;
        private LocalDate targetDate;
        private Integer sortOrder;
        private boolean completed;
        private LocalDateTime completeTime;
        private Long learnDurationSec;
        private LocalDateTime lastActivityTime;
        private String riskLevel;
    }

    @Data
    public static class DetailResponse {
        private Integer version;
        private Long planId;
        private Long userId;
        private String planName;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private int totalCount;
        private int completedCount;
        private int progressRate;
        private String riskLevel;
        private int overdueCount;
        private int dueSoonCount;
        private int stalledCount;
        private LocalDateTime lastActiveTime;
        private List<ItemResponse> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class AdminRiskResponse {
        private Long userId;
        private String username;
        private String displayName;
        private Long planId;
        private String planName;
        private LocalDate endDate;
        private int progressRate;
        private String riskLevel;
        private int overdueCount;
        private int dueSoonCount;
        private int stalledCount;
        private LocalDateTime lastActiveTime;
    }
}
//请求/响应对象
