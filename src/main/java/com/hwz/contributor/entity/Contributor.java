package com.hwz.contributor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目贡献者档案。
 *
 * <p>与 {@code sys_user} 是「档案 / 账号」的关系：{@code user_id} 为空表示无平台账号的外部贡献者，
 * 此时展示名取本表的 {@code display_name}；否则优先取 {@code sys_user.display_name}。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("contributor")
public class Contributor implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "contributor_id", type = IdType.AUTO)
    private Long contributorId;

    /** 关联平台账号，NULL 表示外部贡献者 */
    @TableField("user_id")
    private Long userId;

    /** 展示名，仅外部贡献者使用 */
    @TableField("display_name")
    private String displayName;

    /** 职责标题，如：内容维护、前端开发 */
    @TableField("role_title")
    private String roleTitle;

    /** 负责模块，如：学习卡片、模板管理 */
    @TableField("module_scope")
    private String moduleScope;

    /** 贡献描述 */
    @TableField("description")
    private String description;

    @TableField("github_url")
    private String githubUrl;

    /** 加入项目时间 */
    @TableField("join_date")
    private LocalDate joinDate;

    /** 展示排序，越小越靠前 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 是否在前台展示 */
    @TableField("visible")
    private Integer visible;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
