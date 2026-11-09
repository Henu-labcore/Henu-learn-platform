package com.hwz.contributor.service;

import com.hwz.admin.service.AdminAccessService;
import com.hwz.common.mapper.UserMapper;
import com.hwz.contributor.dto.ContributorDtos;
import com.hwz.contributor.entity.Contributor;
import com.hwz.contributor.mapper.ContributorMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 贡献者档案维护。
 *
 * <p>权限口径：贡献者档案的增删改仅限 ADMIN，复用 {@link AdminAccessService#requireAdmin()}，
 * 不引入新的角色，因此不影响现有 34 处后台权限校验。
 */
@Service
public class ContributorService {

    private static final int MAX_DISPLAY_NAME = 100;
    private static final int MAX_ROLE_TITLE = 100;
    private static final int MAX_MODULE_SCOPE = 255;
    private static final int MAX_GITHUB_URL = 255;

    private final ContributorMapper contributorMapper;
    private final UserMapper userMapper;
    private final AdminAccessService accessService;

    public ContributorService(ContributorMapper contributorMapper,
                              UserMapper userMapper,
                              AdminAccessService accessService) {
        this.contributorMapper = contributorMapper;
        this.userMapper = userMapper;
        this.accessService = accessService;
    }

    // ------------------------------------------------------------------ 查询

    /** 前台展示列表：仅返回 visible = 1 的记录。 */
    public List<ContributorDtos.ContributorView> listPublic() {
        List<Map<String, Object>> rows = contributorMapper.listWithUser(true);
        List<ContributorDtos.ContributorView> views = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            ContributorDtos.ContributorView view = new ContributorDtos.ContributorView();
            view.setContributorId(asLong(row.get("contributorId")));
            view.setDisplayName(asString(row.get("displayName")));
            view.setRoleTitle(asString(row.get("roleTitle")));
            view.setModuleScope(asString(row.get("moduleScope")));
            view.setDescription(asString(row.get("description")));
            view.setGithubUrl(asString(row.get("githubUrl")));
            view.setJoinDate(asLocalDate(row.get("joinDate")));
            view.setSortOrder(asInteger(row.get("sortOrder")));
            view.setGrade(asString(row.get("grade")));
            views.add(view);
        }
        return views;
    }

    /** 管理员列表：含不可见记录及关联账号信息。 */
    public List<ContributorDtos.ContributorAdminView> listForAdmin() {
        List<Map<String, Object>> rows = contributorMapper.listWithUser(false);
        List<ContributorDtos.ContributorAdminView> views = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            views.add(toAdminView(row));
        }
        return views;
    }

    // ------------------------------------------------------------------ 增删改（仅 ADMIN）

    public ContributorDtos.ContributorAdminView create(ContributorDtos.ContributorSaveRequest request) {
        accessService.requireAdmin();
        validate(request);

        Contributor entity = new Contributor();
        applyRequest(entity, request);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        contributorMapper.insert(entity);

        return toAdminView(contributorMapper.findWithUserById(entity.getContributorId()));
    }

    public ContributorDtos.ContributorAdminView update(Long contributorId,
                                                       ContributorDtos.ContributorSaveRequest request) {
        accessService.requireAdmin();
        Contributor existing = requireContributor(contributorId);
        validate(request);

        applyRequest(existing, request);
        existing.setUpdatedAt(LocalDateTime.now());
        contributorMapper.updateById(existing);

        return toAdminView(contributorMapper.findWithUserById(contributorId));
    }

    public void delete(Long contributorId) {
        accessService.requireAdmin();
        requireContributor(contributorId);
        contributorMapper.deleteById(contributorId);
    }

    // ------------------------------------------------------------------ 内部方法

    private Contributor requireContributor(Long contributorId) {
        if (contributorId == null || contributorId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "贡献者编号不正确");
        }
        Contributor existing = contributorMapper.selectById(contributorId);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "贡献者记录不存在");
        }
        return existing;
    }

    private void validate(ContributorDtos.ContributorSaveRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求内容不能为空");
        }

        boolean linked = request.getUserId() != null && request.getUserId() > 0;
        if (linked) {
            if (userMapper.selectById(request.getUserId()) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "关联的平台账号不存在");
            }
        } else if (!StringUtils.hasText(request.getDisplayName())) {
            // 无账号时必须自带展示名，否则前台无法显示
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未关联平台账号时必须填写展示名");
        }

        checkLength(request.getDisplayName(), MAX_DISPLAY_NAME, "展示名");
        checkLength(request.getRoleTitle(), MAX_ROLE_TITLE, "职责标题");
        checkLength(request.getModuleScope(), MAX_MODULE_SCOPE, "负责模块");
        checkLength(request.getGithubUrl(), MAX_GITHUB_URL, "GitHub 地址");
    }

    private void checkLength(String value, int max, String label) {
        if (value != null && value.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + "长度不能超过 " + max + " 个字符");
        }
    }

    private void applyRequest(Contributor entity, ContributorDtos.ContributorSaveRequest request) {
        boolean linked = request.getUserId() != null && request.getUserId() > 0;
        entity.setUserId(linked ? request.getUserId() : null);
        entity.setDisplayName(trimToNull(request.getDisplayName()));
        entity.setRoleTitle(trimToNull(request.getRoleTitle()));
        entity.setModuleScope(trimToNull(request.getModuleScope()));
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setGithubUrl(trimToNull(request.getGithubUrl()));
        entity.setJoinDate(request.getJoinDate());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setVisible(request.getVisible() == null || request.getVisible() != 0 ? 1 : 0);
    }

    private ContributorDtos.ContributorAdminView toAdminView(Map<String, Object> row) {
        ContributorDtos.ContributorAdminView view = new ContributorDtos.ContributorAdminView();
        if (row == null) {
            return view;
        }
        view.setContributorId(asLong(row.get("contributorId")));
        view.setUserId(asLong(row.get("userId")));
        view.setDisplayName(asString(row.get("displayName")));
        view.setUsername(asString(row.get("username")));
        view.setGrade(asString(row.get("grade")));
        view.setRoleTitle(asString(row.get("roleTitle")));
        view.setModuleScope(asString(row.get("moduleScope")));
        view.setDescription(asString(row.get("description")));
        view.setGithubUrl(asString(row.get("githubUrl")));
        view.setJoinDate(asLocalDate(row.get("joinDate")));
        view.setSortOrder(asInteger(row.get("sortOrder")));
        view.setVisible(asInteger(row.get("visible")));
        view.setCreatedAt(asLocalDateTime(row.get("createdAt")));
        view.setUpdatedAt(asLocalDateTime(row.get("updatedAt")));
        return view;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number ? ((Number) value).longValue() : Long.valueOf(String.valueOf(value));
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number ? ((Number) value).intValue() : Integer.valueOf(String.valueOf(value));
    }

    private static java.time.LocalDate asLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.time.LocalDate) {
            return (java.time.LocalDate) value;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        return java.time.LocalDate.parse(String.valueOf(value).substring(0, 10));
    }

    private static LocalDateTime asLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        }
        String text = String.valueOf(value).replace('T', ' ');
        if (text.length() > 19) {
            text = text.substring(0, 19);
        }
        return LocalDateTime.parse(text.replace(' ', 'T'));
    }
}
