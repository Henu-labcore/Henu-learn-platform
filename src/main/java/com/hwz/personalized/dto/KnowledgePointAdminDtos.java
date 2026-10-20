package com.hwz.personalized.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识点管理模块使用的请求和响应对象。
 */
public final class KnowledgePointAdminDtos {

    private KnowledgePointAdminDtos() {
    }

    /**
     * 新增或编辑知识点请求。
     *
     * 同时包含：
     * 1. 知识点基础信息；
     * 2. 前置知识点关系；
     * 3. 学习资源绑定；
     * 4. 能力考核绑定。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgePointSaveRequest {

        /**
         * 知识点编码。
         */
        private String code;

        /**
         * 知识点名称。
         */
        private String name;

        /**
         * 知识点说明。
         */
        private String description;

        /**
         * 知识点分类。
         */
        private String category;

        /**
         * 难度：
         * BASIC、INTERMEDIATE、ADVANCED。
         */
        private String difficulty;

        /**
         * 状态：
         * ACTIVE、INACTIVE。
         */
        private String status;

        /**
         * 掌握度预警阈值，范围 0 到 100。
         */
        private BigDecimal warningThreshold;

        /**
         * 显示顺序。
         */
        private Integer sortOrder;

        /**
         * 前置知识点主键列表。
         */
        @Builder.Default
        private List<Long> prerequisiteIds =
                new ArrayList<>();

        /**
         * 学习资源绑定列表。
         */
        @Builder.Default
        private List<LearningItemBindingInput>
                learningItemBindings =
                new ArrayList<>();

        /**
         * 能力考核绑定列表。
         */
        @Builder.Default
        private List<AssignmentBindingInput>
                assignmentBindings =
                new ArrayList<>();
    }

    /**
     * 单独修改知识点状态请求。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgePointStatusRequest {

        /**
         * ACTIVE 或 INACTIVE。
         */
        private String status;
    }

    /**
     * 学习资源绑定输入。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningItemBindingInput {

        /**
         * learning_item.item_pk。
         */
        private Long itemPk;

        /**
         * 该资源在当前知识点计算中的权重。
         */
        private BigDecimal weight;
    }

    /**
     * 能力考核绑定输入。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentBindingInput {

        /**
         * assignment.assignment_id。
         */
        private Long assignmentId;

        /**
         * 该考核在当前知识点计算中的权重。
         */
        private BigDecimal weight;
    }

    /**
     * 知识点列表项。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgePointSummary {

        private Long knowledgePointId;

        private String code;

        private String name;

        private String description;

        private String category;

        private String difficulty;

        private String status;

        private BigDecimal warningThreshold;

        private Integer sortOrder;

        /**
         * 前置知识点数量。
         */
        private Integer prerequisiteCount;

        /**
         * 已绑定学习资源数量。
         */
        private Integer learningItemCount;

        /**
         * 已绑定能力考核数量。
         */
        private Integer assignmentCount;

        private Long createdBy;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;
    }

    /**
     * 知识点完整详情。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgePointDetail {

        private Long knowledgePointId;

        private String code;

        private String name;

        private String description;

        private String category;

        private String difficulty;

        private String status;

        private BigDecimal warningThreshold;

        private Integer sortOrder;

        private Long createdBy;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;

        /**
         * 当前知识点的前置知识点。
         */
        @Builder.Default
        private List<PrerequisiteDetail>
                prerequisites =
                new ArrayList<>();

        /**
         * 当前知识点绑定的学习资源。
         */
        @Builder.Default
        private List<LearningItemBindingDetail>
                learningItemBindings =
                new ArrayList<>();

        /**
         * 当前知识点绑定的能力考核。
         */
        @Builder.Default
        private List<AssignmentBindingDetail>
                assignmentBindings =
                new ArrayList<>();
    }

    /**
     * 前置知识点详情。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrerequisiteDetail {

        private Long relationId;

        private Long knowledgePointId;

        private String code;

        private String name;

        private String category;

        private String difficulty;

        private String status;
    }

    /**
     * 学习资源绑定详情。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningItemBindingDetail {

        private Long relationId;

        private Long itemPk;

        private String title;

        private String category;

        private String difficulty;

        private String status;

        private BigDecimal weight;
    }

    /**
     * 能力考核绑定详情。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentBindingDetail {

        private Long relationId;

        private Long assignmentId;

        private String title;

        private String category;

        private String status;

        private BigDecimal totalScore;

        private LocalDateTime deadline;

        private BigDecimal weight;
    }

    /**
     * 前端选择前置知识点时使用的选项。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgePointOption {

        private Long knowledgePointId;

        private String code;

        private String name;

        private String category;

        private String difficulty;

        private String status;
    }

    /**
     * 前端选择学习资源时使用的选项。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningItemOption {

        private Long itemPk;

        private String title;

        private String category;

        private String difficulty;

        private String status;
    }

    /**
     * 前端选择能力考核时使用的选项。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentOption {

        private Long assignmentId;

        private String title;

        private String category;

        private String status;

        private BigDecimal totalScore;

        private LocalDateTime deadline;
    }

    /**
     * 知识点管理页面顶部统计数据。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgePointStats {

        /**
         * 知识点总数。
         */
        private Long totalCount;

        /**
         * 启用知识点数。
         */
        private Long activeCount;

        /**
         * 停用知识点数。
         */
        private Long inactiveCount;

        /**
         * 已绑定学习资源的知识点数。
         */
        private Long boundLearningItemCount;

        /**
         * 已绑定能力考核的知识点数。
         */
        private Long boundAssignmentCount;

        /**
         * 存在前置关系的知识点数。
         */
        private Long prerequisiteConfiguredCount;
    }
}