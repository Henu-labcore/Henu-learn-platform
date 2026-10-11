package com.hwz.learningplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hwz.common.PageResponse;
import com.hwz.common.entity.User;
import com.hwz.common.mapper.UserMapper;
import com.hwz.learningplan.dto.LearningPlanDtos;
import com.hwz.learningplan.dto.LearningPlanDtos.AdminRiskResponse;
import com.hwz.learningplan.dto.LearningPlanDtos.DetailResponse;
import com.hwz.learningplan.dto.LearningPlanDtos.ItemRequest;
import com.hwz.learningplan.dto.LearningPlanDtos.ItemResponse;
import com.hwz.learningplan.dto.LearningPlanDtos.ItemViewRow;
import com.hwz.learningplan.dto.LearningPlanDtos.SaveRequest;
import com.hwz.learningplan.entity.LearningPlan;
import com.hwz.learningplan.entity.LearningPlanItem;
import com.hwz.learningplan.mapper.LearningPlanItemMapper;
import com.hwz.learningplan.mapper.LearningPlanMapper;
import com.hwz.user.entity.LearningItem;
import com.hwz.user.mapper.LearningItemMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 沿用原学习计划命名与风险规则。学习记录只读，计划状态写入新增模块表。
 * 调整/取消使用行锁，完成同步使用版本条件更新，避免跨设备覆盖和影响原学习流程。
 */
@Service
public class LearningPlanService {
    public static final String ACTIVE = "ACTIVE";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private final LearningPlanMapper planMapper;
    private final LearningPlanItemMapper itemMapper;
    private final LearningItemMapper learningItemMapper;
    private final UserMapper userMapper;
    private final LearningPlanRiskEvaluator riskEvaluator;
    private final Clock clock;

    public LearningPlanService(LearningPlanMapper planMapper,
                               LearningPlanItemMapper itemMapper,
                               LearningItemMapper learningItemMapper,
                               UserMapper userMapper,
                               LearningPlanRiskEvaluator riskEvaluator,
                               @Qualifier("learningPlanClock") Clock clock) {
        this.planMapper = planMapper;
        this.itemMapper = itemMapper;
        this.learningItemMapper = learningItemMapper;
        this.userMapper = userMapper;
        this.riskEvaluator = riskEvaluator;
        this.clock = clock;
    }

    @Transactional
    public DetailResponse create(Long userId, SaveRequest request) {
        validateRequest(request);
        // 沿用原学习完成状态，不重置学习记录；前端提示之外仍需后端防止绕过。
        List<Long> selectedIds = request.getItems().stream().map(ItemRequest::getItemPk).collect(Collectors.toList());
        if (itemMapper.countUncompletedItems(userId, selectedIds) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "所选学习条目均已完成，请至少添加一项未完成内容，避免新计划直接进入历史");
        }
        synchronizeActivePlan(userId);
        if (findActive(userId) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "每名学生只能有一个进行中的学习计划");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LearningPlan plan = LearningPlan.builder()
                .userId(userId)
                .planName(request.getPlanName().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(ACTIVE)
                .activeMarker(1)
                .version(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            planMapper.insert(plan);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "每名学生只能有一个进行中的学习计划");
        }
        replaceItems(plan.getPlanId(), request.getItems(), now);
        return buildDetail(plan);
    }

    @Transactional
    public DetailResponse update(Long userId, Long planId, SaveRequest request) {
        validateRequest(request);
        LearningPlan plan = planMapper.selectForUpdate(planId);
        checkOwnership(userId, plan);
        ensureActive(plan);
        // 后端检查版本，防止另一台设备覆盖已更新的计划。
        if (request.getVersion() == null || !request.getVersion().equals(plan.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "计划已在其他设备更新，请刷新后重试");
        }
        plan.setVersion(plan.getVersion() + 1);
        plan.setPlanName(request.getPlanName().trim());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setUpdatedAt(LocalDateTime.now(clock));
        planMapper.updateById(plan);
        itemMapper.delete(new LambdaQueryWrapper<LearningPlanItem>()
                .eq(LearningPlanItem::getPlanId, planId));
        replaceItems(planId, request.getItems(), LocalDateTime.now(clock));
        return buildDetail(plan);
    }

    @Transactional
    public DetailResponse cancel(Long userId, Long planId) {
        LearningPlan plan = planMapper.selectForUpdate(planId);
        checkOwnership(userId, plan);
        ensureActive(plan);
        plan.setVersion(plan.getVersion() + 1);
        plan.setStatus(CANCELLED);
        plan.setActiveMarker(null);
        plan.setUpdatedAt(LocalDateTime.now(clock));
        planMapper.updateById(plan);
        return buildDetail(plan);
    }

    @Transactional
    public DetailResponse getCurrent(Long userId) {
        LearningPlan active = findActive(userId);
        if (active == null) return null;
        DetailResponse detail = buildDetail(active);
        if (detail.getCompletedCount() == detail.getTotalCount() && detail.getTotalCount() > 0) {
            if (complete(active)) {
                detail.setStatus(COMPLETED);
                detail.setVersion(active.getVersion());
            } else {
                return buildDetail(requireOwnedPlan(userId, active.getPlanId()));
            }
        }
        return detail;
    }

    public DetailResponse getById(Long userId, Long planId) {
        return buildDetail(requireOwnedPlan(userId, planId));
    }

    public List<DetailResponse> getHistory(Long userId) {
        List<LearningPlan> plans = planMapper.selectList(new LambdaQueryWrapper<LearningPlan>()
                .eq(LearningPlan::getUserId, userId)
                .ne(LearningPlan::getStatus, ACTIVE)
                .orderByDesc(LearningPlan::getUpdatedAt));
        if (plans == null) return Collections.emptyList();
        return plans.stream().map(this::buildDetail).collect(Collectors.toList());
    }

    @Transactional
    public void synchronizeActivePlan(Long userId) {
        LearningPlan active = findActive(userId);
        if (active == null) return;
        DetailResponse detail = buildDetail(active);
        if (detail.getTotalCount() > 0 && detail.getCompletedCount() == detail.getTotalCount()) {
            complete(active);
        }
    }

    public PageResponse<AdminRiskResponse> getAdminRisks(String riskLevel,
                                                          String keyword,
                                                          int page,
                                                          int pageSize) {
        int resolvedPage = Math.max(page, 1);
        int resolvedSize = Math.min(Math.max(pageSize, 1), 100);
        String riskFilter = normalizeRiskFilter(riskLevel);
        String normalizedKeyword = StringUtils.hasText(keyword)
                ? keyword.trim().toLowerCase(Locale.ROOT) : null;

        List<LearningPlan> activePlans = planMapper.selectList(new LambdaQueryWrapper<LearningPlan>()
                .eq(LearningPlan::getStatus, ACTIVE)
                .orderByAsc(LearningPlan::getEndDate));
        List<AdminRiskResponse> rows = new ArrayList<>();
        if (activePlans != null) {
            for (LearningPlan plan : activePlans) {
                DetailResponse detail = buildDetail(plan);
                if (detail.getTotalCount() > 0 && detail.getCompletedCount() == detail.getTotalCount()) {
                    if (complete(plan)) continue;
                }
                User user = userMapper.selectById(plan.getUserId());
                if (user == null || user.getStatus() != User.Status.ACTIVE) continue;
                if (riskFilter != null && !riskFilter.equals(detail.getRiskLevel())) continue;
                String searchable = (safe(user.getUsername()) + " " + safe(user.getDisplayName()) + " "
                        + safe(user.getRealName()) + " " + safe(plan.getPlanName())).toLowerCase(Locale.ROOT);
                if (normalizedKeyword != null && !searchable.contains(normalizedKeyword)) continue;
                rows.add(toAdminRow(user, plan, detail));
            }
        }
        rows.sort(Comparator
                .comparingInt((AdminRiskResponse row) -> riskRank(row.getRiskLevel())).reversed()
                .thenComparing(AdminRiskResponse::getEndDate));
        int from = Math.min((resolvedPage - 1) * resolvedSize, rows.size());
        int to = Math.min(from + resolvedSize, rows.size());
        return PageResponse.of(new ArrayList<>(rows.subList(from, to)), rows.size(), resolvedPage, resolvedSize);
    }

    private void validateRequest(SaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getPlanName())
                || request.getPlanName().trim().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "计划名称长度应为 1 至 100 个字符");
        }
        if (request.getStartDate() == null || request.getEndDate() == null
                || request.getStartDate().isAfter(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "计划开始日期不能晚于结束日期");
        }
        if (request.getItems() == null || request.getItems().isEmpty() || request.getItems().size() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学习计划应包含 1 至 50 个学习条目");
        }
        Set<Long> itemIds = new HashSet<>();
        for (ItemRequest item : request.getItems()) {
            if (item == null || item.getItemPk() == null || item.getItemPk() <= 0 || item.getTargetDate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学习条目信息不完整");
            }
            if (!itemIds.add(item.getItemPk())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "同一学习计划不能重复添加学习条目");
            }
            if (item.getTargetDate().isBefore(request.getStartDate())
                    || item.getTargetDate().isAfter(request.getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "条目目标日期必须位于计划周期内");
            }
        }
        List<LearningItem> learningItems = learningItemMapper.selectList(
                new LambdaQueryWrapper<LearningItem>().in(LearningItem::getItemPk, itemIds));
        if (learningItems == null || learningItems.size() != itemIds.size()
                || learningItems.stream().anyMatch(item -> !"PUBLISHED".equalsIgnoreCase(item.getStatus()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "计划只能包含已发布的学习条目");
        }
    }

    private void replaceItems(Long planId, List<ItemRequest> requests, LocalDateTime now) {
        int fallbackOrder = 1;
        for (ItemRequest request : requests) {
            int sortOrder = request.getSortOrder() == null ? fallbackOrder : request.getSortOrder();
            LearningPlanItem item = LearningPlanItem.builder()
                    .planId(planId)
                    .itemPk(request.getItemPk())
                    .targetDate(request.getTargetDate())
                    .sortOrder(sortOrder)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            itemMapper.insert(item);
            fallbackOrder++;
        }
    }

    private DetailResponse buildDetail(LearningPlan plan) {
        List<ItemViewRow> rows = itemMapper.selectViewRows(plan.getPlanId(), plan.getUserId());
        if (rows == null) rows = Collections.emptyList();
        LocalDate today = LocalDate.now(clock);
        LocalDate fallbackActivityDate = plan.getStartDate().isAfter(plan.getCreatedAt().toLocalDate())
                ? plan.getStartDate() : plan.getCreatedAt().toLocalDate();
        List<ItemResponse> items = new ArrayList<>();
        LearningPlanRiskEvaluator.RiskLevel planRisk = LearningPlanRiskEvaluator.RiskLevel.NORMAL;
        int completed = 0, overdue = 0, dueSoon = 0, stalled = 0;
        LocalDateTime lastActive = null;
        for (ItemViewRow row : rows) {
            boolean isCompleted = row.getCompleteTime() != null;
            if (isCompleted) completed++;
            LearningPlanRiskEvaluator.RiskLevel risk = riskEvaluator.evaluate(
                    today, row.getTargetDate(), isCompleted, row.getLastActivityTime(), fallbackActivityDate);
            planRisk = riskEvaluator.max(planRisk, risk);
            if (risk == LearningPlanRiskEvaluator.RiskLevel.OVERDUE) overdue++;
            if (risk == LearningPlanRiskEvaluator.RiskLevel.DUE_SOON) dueSoon++;
            if (risk == LearningPlanRiskEvaluator.RiskLevel.STALLED) stalled++;
            if (row.getLastActivityTime() != null && (lastActive == null || row.getLastActivityTime().isAfter(lastActive))) {
                lastActive = row.getLastActivityTime();
            }
            items.add(toItemResponse(row, risk, isCompleted));
        }
        DetailResponse response = new DetailResponse();
        response.setPlanId(plan.getPlanId());
        response.setVersion(plan.getVersion());
        response.setUserId(plan.getUserId());
        response.setPlanName(plan.getPlanName());
        response.setStartDate(plan.getStartDate());
        response.setEndDate(plan.getEndDate());
        response.setStatus(plan.getStatus());
        response.setTotalCount(items.size());
        response.setCompletedCount(completed);
        response.setProgressRate(items.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / items.size()));
        response.setRiskLevel(planRisk.name());
        response.setOverdueCount(overdue);
        response.setDueSoonCount(dueSoon);
        response.setStalledCount(stalled);
        response.setLastActiveTime(lastActive);
        response.setItems(items);
        response.setCreatedAt(plan.getCreatedAt());
        response.setUpdatedAt(plan.getUpdatedAt());
        return response;
    }

    private ItemResponse toItemResponse(ItemViewRow row,
                                        LearningPlanRiskEvaluator.RiskLevel risk,
                                        boolean completed) {
        ItemResponse response = new ItemResponse();
        response.setPlanItemId(row.getPlanItemId());
        response.setAvailable(row.getAvailable());
        response.setItemPk(row.getItemPk());
        response.setTitle(row.getTitle());
        response.setCategory(row.getCategory());
        response.setDifficulty(row.getDifficulty());
        response.setDuration(row.getDuration());
        response.setTargetDate(row.getTargetDate());
        response.setSortOrder(row.getSortOrder());
        response.setCompleted(completed);
        response.setCompleteTime(row.getCompleteTime());
        response.setLearnDurationSec(row.getLearnDurationSec());
        response.setLastActivityTime(row.getLastActivityTime());
        response.setRiskLevel(risk.name());
        return response;
    }

    private AdminRiskResponse toAdminRow(User user, LearningPlan plan, DetailResponse detail) {
        AdminRiskResponse row = new AdminRiskResponse();
        row.setUserId(user.getId());
        row.setUsername(user.getUsername());
        row.setDisplayName(StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getUsername());
        row.setPlanId(plan.getPlanId());
        row.setPlanName(plan.getPlanName());
        row.setEndDate(plan.getEndDate());
        row.setProgressRate(detail.getProgressRate());
        row.setRiskLevel(detail.getRiskLevel());
        row.setOverdueCount(detail.getOverdueCount());
        row.setDueSoonCount(detail.getDueSoonCount());
        row.setStalledCount(detail.getStalledCount());
        row.setLastActiveTime(detail.getLastActiveTime());
        return row;
    }

    private LearningPlan requireOwnedPlan(Long userId, Long planId) {
        LearningPlan plan = planMapper.selectById(planId);
        checkOwnership(userId, plan);
        return plan;
    }

    private void checkOwnership(Long userId, LearningPlan plan) {
        if (plan == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学习计划不存在");
        if (!plan.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该学习计划");
        }
    }

    private LearningPlan findActive(Long userId) {
        return planMapper.selectOne(new LambdaQueryWrapper<LearningPlan>()
                .eq(LearningPlan::getUserId, userId)
                .eq(LearningPlan::getStatus, ACTIVE)
                .last("LIMIT 1"));
    }

    private void ensureActive(LearningPlan plan) {
        if (!ACTIVE.equals(plan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只有进行中的计划可以修改");
        }
    }

    private boolean complete(LearningPlan plan) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (planMapper.markCompleted(plan.getPlanId(), plan.getVersion(), now) != 1) return false;
        plan.setStatus(COMPLETED);
        plan.setActiveMarker(null);
        plan.setVersion(plan.getVersion() + 1);
        plan.setUpdatedAt(now);
        return true;
    }

    private String normalizeRiskFilter(String riskLevel) {
        if (!StringUtils.hasText(riskLevel)) return null;
        String value = riskLevel.trim().toUpperCase(Locale.ROOT);
        try {
            LearningPlanRiskEvaluator.RiskLevel.valueOf(value);
            return value;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "风险等级参数不正确");
        }
    }

    private int riskRank(String riskLevel) {
        try {
            return LearningPlanRiskEvaluator.RiskLevel.valueOf(riskLevel).ordinal();
        } catch (Exception ex) {
            return 0;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
