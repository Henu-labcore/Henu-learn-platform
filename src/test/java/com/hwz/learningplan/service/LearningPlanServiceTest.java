package com.hwz.learningplan.service;

import com.hwz.common.mapper.UserMapper;
import com.hwz.learningplan.dto.LearningPlanDtos.DetailResponse;
import com.hwz.learningplan.dto.LearningPlanDtos.ItemRequest;
import com.hwz.learningplan.dto.LearningPlanDtos.ItemViewRow;
import com.hwz.learningplan.dto.LearningPlanDtos.SaveRequest;
import com.hwz.learningplan.entity.LearningPlan;
import com.hwz.learningplan.entity.LearningPlanItem;
import com.hwz.learningplan.mapper.LearningPlanItemMapper;
import com.hwz.learningplan.mapper.LearningPlanMapper;
import com.hwz.user.entity.LearningItem;
import com.hwz.user.mapper.LearningItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningPlanServiceTest {
    @Mock private LearningPlanMapper planMapper;
    @Mock private LearningPlanItemMapper itemMapper;
    @Mock private LearningItemMapper learningItemMapper;
    @Mock private UserMapper userMapper;

    private LearningPlanService service;
    private final LocalDate today = LocalDate.of(2026, 7, 17);

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-17T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
        service = new LearningPlanService(planMapper, itemMapper, learningItemMapper, userMapper,
                new LearningPlanRiskEvaluator(3, 7), clock);
    }

    @Test
    void createsPlanWithPublishedUniqueItems() {
        SaveRequest request = request(11L, today.plusDays(5));
        LearningItem item = LearningItem.builder().itemPk(11L).status("PUBLISHED").build();
        when(learningItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));
        when(planMapper.selectOne(any())).thenReturn(null);
        when(itemMapper.countUncompletedItems(eq(7L), any())).thenReturn(1);
        doAnswer(invocation -> {
            LearningPlan plan = invocation.getArgument(0);
            plan.setPlanId(100L);
            return 1;
        }).when(planMapper).insert(any(LearningPlan.class));
        ItemViewRow row = new ItemViewRow();
        row.setPlanItemId(1L);
        row.setItemPk(11L);
        row.setTitle("测试课程");
        row.setTargetDate(today.plusDays(5));
        row.setSortOrder(1);
        row.setLearnDurationSec(0L);
        when(itemMapper.selectViewRows(100L, 7L)).thenReturn(Collections.singletonList(row));

        DetailResponse result = service.create(7L, request);

        assertEquals(100L, result.getPlanId());
        assertEquals(1, result.getTotalCount());
        assertEquals("NORMAL", result.getRiskLevel());
        verify(itemMapper).insert(any(com.hwz.learningplan.entity.LearningPlanItem.class));
    }

    @Test
    void rejectsNewPlanContainingOnlyPreviouslyCompletedItemsWithoutWriting() {
        SaveRequest request = request(11L, today.plusDays(2));
        when(learningItemMapper.selectList(any())).thenReturn(Collections.singletonList(
                LearningItem.builder().itemPk(11L).status("PUBLISHED").build()));
        when(itemMapper.countUncompletedItems(eq(7L), any())).thenReturn(0);

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () -> service.create(7L, request));

        assertEquals(409, failure.getStatus().value());
        verify(planMapper, never()).insert(any(LearningPlan.class));
        verify(itemMapper, never()).insert(any(LearningPlanItem.class));
    }

    @Test
    void rejectsDuplicateItemsAndInvalidDateRange() {
        SaveRequest duplicate = request(11L, today.plusDays(2));
        duplicate.setItems(Arrays.asList(duplicate.getItems().get(0), duplicate.getItems().get(0)));
        assertThrows(ResponseStatusException.class, () -> service.create(7L, duplicate));

        SaveRequest invalidRange = request(11L, today.plusDays(2));
        invalidRange.setStartDate(today.plusDays(3));
        invalidRange.setEndDate(today);
        assertThrows(ResponseStatusException.class, () -> service.create(7L, invalidRange));
    }

    @Test
    void rejectsUnpublishedLearningItem() {
        SaveRequest request = request(11L, today.plusDays(2));
        when(learningItemMapper.selectList(any())).thenReturn(Collections.singletonList(
                LearningItem.builder().itemPk(11L).status("DRAFT").build()));
        assertThrows(ResponseStatusException.class, () -> service.create(7L, request));
    }

    @Test
    void preventsReadingAnotherUsersPlan() {
        LearningPlan plan = activePlan();
        plan.setUserId(9L);
        when(planMapper.selectById(100L)).thenReturn(plan);
        assertThrows(ResponseStatusException.class, () -> service.getById(7L, 100L));
    }

    @Test
    void completesActivePlanWhenEveryItemIsComplete() {
        LearningPlan plan = activePlan();
        when(planMapper.selectOne(any())).thenReturn(plan);
        ItemViewRow row = new ItemViewRow();
        row.setPlanItemId(1L);
        row.setItemPk(11L);
        row.setTitle("测试课程");
        row.setTargetDate(today.plusDays(1));
        row.setSortOrder(1);
        row.setCompleteTime(LocalDateTime.of(2026, 7, 17, 9, 0));
        row.setLearnDurationSec(120L);
        when(itemMapper.selectViewRows(100L, 7L)).thenReturn(Collections.singletonList(row));

        service.synchronizeActivePlan(7L);

        verify(planMapper).markCompleted(eq(100L),eq(1),any(LocalDateTime.class));
    }

    @Test
    void cancelClearsActiveMarkerSoAnotherPlanCanBeCreated() {
        LearningPlan plan = activePlan();
        when(planMapper.selectForUpdate(100L)).thenReturn(plan);
        when(itemMapper.selectViewRows(100L, 7L)).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            LearningPlan updated = invocation.getArgument(0);
            assertEquals(LearningPlanService.CANCELLED, updated.getStatus());
            assertEquals(null, updated.getActiveMarker());
            return 1;
        }).when(planMapper).updateById(any(LearningPlan.class));

        DetailResponse result = service.cancel(7L, 100L);

        assertEquals(LearningPlanService.CANCELLED, result.getStatus());
        verify(planMapper).updateById(plan);
    }

    private SaveRequest request(Long itemPk, LocalDate targetDate) {
        ItemRequest item = new ItemRequest();
        item.setItemPk(itemPk);
        item.setTargetDate(targetDate);
        item.setSortOrder(1);
        SaveRequest request = new SaveRequest();
        request.setPlanName("密码学强化计划");
        request.setStartDate(today);
        request.setEndDate(today.plusDays(14));
        request.setItems(Collections.singletonList(item));
        return request;
    }

    private LearningPlan activePlan() {
        return LearningPlan.builder()
                .planId(100L)
                .userId(7L)
                .planName("密码学强化计划")
                .startDate(today)
                .endDate(today.plusDays(14))
                .status(LearningPlanService.ACTIVE)
                .activeMarker(1)
                .version(1)
                .createdAt(LocalDateTime.of(2026, 7, 17, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 7, 17, 8, 0))
                .build();
    }
}
