package com.hwz.contributor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hwz.contributor.entity.Contributor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 贡献者档案访问层。
 *
 * <p>查询用 {@code LEFT JOIN sys_user} 合并账号信息：展示名与年级优先取账号，
 * 账号不存在（外部贡献者）时回退到档案自身的 {@code display_name}。
 * 返回通用 {@code Map}，由 Service 转换为 DTO，避免为只读联表查询单独建 VO。
 */
@Mapper
public interface ContributorMapper extends BaseMapper<Contributor> {

    String SELECT_COLUMNS =
            "SELECT c.contributor_id AS contributorId, "
                    + "c.user_id AS userId, "
                    + "COALESCE(u.display_name, c.display_name) AS displayName, "
                    + "u.username AS username, "
                    + "u.grade AS grade, "
                    + "c.role_title AS roleTitle, "
                    + "c.module_scope AS moduleScope, "
                    + "c.description AS description, "
                    + "c.github_url AS githubUrl, "
                    + "c.join_date AS joinDate, "
                    + "c.sort_order AS sortOrder, "
                    + "c.visible AS visible, "
                    + "c.created_at AS createdAt, "
                    + "c.updated_at AS updatedAt ";

    /**
     * 列出贡献者。{@code visibleOnly} 为 true 时只返回前台可见的记录。
     */
    @Select("<script>"
            + SELECT_COLUMNS
            + "FROM contributor c LEFT JOIN sys_user u ON c.user_id = u.user_id "
            + "<where>"
            + "  <if test='visibleOnly'> c.visible = 1 </if>"
            + "</where>"
            + "ORDER BY c.sort_order ASC, c.contributor_id ASC"
            + "</script>")
    List<Map<String, Object>> listWithUser(@Param("visibleOnly") boolean visibleOnly);

    /**
     * 按主键取单条（含账号信息），不存在时返回 null。
     */
    @Select(SELECT_COLUMNS
            + "FROM contributor c LEFT JOIN sys_user u ON c.user_id = u.user_id "
            + "WHERE c.contributor_id = #{contributorId}")
    Map<String, Object> findWithUserById(@Param("contributorId") Long contributorId);
}
