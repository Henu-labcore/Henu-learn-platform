package com.hwz.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AssignmentDtos {

    private AssignmentDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentSaveRequest {
        private String title;
        private String description;
        private String category;
        private LocalDateTime deadline;
        private BigDecimal totalScore;
        @Builder.Default
        private List<QuestionInput> questions = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionInput {
        private Long questionId;
        private String title;
        private String content;
        private BigDecimal score;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentSummary {
        private Long assignmentId;
        private String title;
        private String description;
        private String category;
        private LocalDateTime deadline;
        private BigDecimal totalScore;
        private String status;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private SubmissionSummary mySubmission;
        private long submissionCount;
        private long gradedCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentDetail {
        private Long assignmentId;
        private String title;
        private String description;
        private String category;
        private LocalDateTime deadline;
        private BigDecimal totalScore;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        @Builder.Default
        private List<QuestionDetail> questions = new ArrayList<>();
        @Builder.Default
        private List<MaterialDetail> materials = new ArrayList<>();
        private SubmissionDetail mySubmission;
        private long submissionCount;
        private long gradedCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDetail {
        private Long questionId;
        private String title;
        private String content;
        private BigDecimal score;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionSummary {
        private Long submissionId;
        private Long assignmentId;
        private Long studentId;
        private String studentName;
        private String status;
        private LocalDateTime submittedAt;
        private LocalDateTime gradedAt;
        private BigDecimal score;
        private String feedback;
        private int fileCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionDetail {
        private Long submissionId;
        private Long assignmentId;
        private Long studentId;
        private String studentName;
        private String status;
        private String answerText;
        private LocalDateTime submittedAt;
        private LocalDateTime gradedAt;
        private BigDecimal score;
        private String feedback;
        @Builder.Default
        private List<SubmissionFileDetail> files = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionFileDetail {
        private Long fileId;
        private Long submissionId;
        private String fileType;
        private String originalName;
        private String mimeType;
        private Long fileSize;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchDownloadInfo {
        private long fileCount;
        private long totalBytes;
        private long maxBytes;
        private boolean allowed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialDetail {
        private Long materialId;
        private Long assignmentId;
        private String materialType;
        private String title;
        private String originalName;
        private String mimeType;
        private Long fileSize;
        private Integer sortOrder;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeRequest {
        private BigDecimal score;
        private String feedback;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerSaveRequest {
        private String answerText;
    }
}
