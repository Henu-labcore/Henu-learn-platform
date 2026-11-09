package com.hwz.contributor.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 贡献者相关 DTO。沿用项目「一个模块一个 DTO 容器类」的约定。
 */
public final class ContributorDtos {

    private ContributorDtos() {
    }

    /** 前台展示用，不含内部字段。 */
    @Data
    public static class ContributorView {
        private Long contributorId;
        /** 最终展示名：优先账号 display_name，回退档案 display_name */
        private String displayName;
        private String roleTitle;
        private String moduleScope;
        private String description;
        private String githubUrl;
        private LocalDate joinDate;
        private Integer sortOrder;
        private String grade;
    }

    /** 管理员列表用，含关联账号与可见性等管理字段。 */
    @Data
    public static class ContributorAdminView {
        private Long contributorId;
        private Long userId;
        private String displayName;
        private String username;
        private String grade;
        private String roleTitle;
        private String moduleScope;
        private String description;
        private String githubUrl;
        private LocalDate joinDate;
        private Integer sortOrder;
        private Integer visible;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /** 新增/更新请求。 */
    @Data
    public static class ContributorSaveRequest {
        private Long userId;
        private String displayName;
        private String roleTitle;
        private String moduleScope;
        private String description;
        private String githubUrl;
        private LocalDate joinDate;
        private Integer sortOrder;
        private Integer visible;
    }
}