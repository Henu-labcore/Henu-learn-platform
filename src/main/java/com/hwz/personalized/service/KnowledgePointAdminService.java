package com.hwz.personalized.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hwz.assignment.entity.Assignment;
import com.hwz.assignment.mapper.AssignmentMapper;
import com.hwz.common.PageResponse;
import com.hwz.common.entity.AssignmentKnowledgePoint;
import com.hwz.common.entity.KnowledgePoint;
import com.hwz.common.entity.KnowledgePointPrerequisite;
import com.hwz.common.entity.LearningItemKnowledgePoint;
import com.hwz.common.entity.User;
import com.hwz.common.mapper.AssignmentKnowledgePointMapper;
import com.hwz.common.mapper.KnowledgePointMapper;
import com.hwz.common.mapper.KnowledgePointPrerequisiteMapper;
import com.hwz.common.mapper.LearningItemKnowledgePointMapper;
import com.hwz.personalized.dto.KnowledgePointAdminDtos;
import com.hwz.user.entity.LearningItem;
import com.hwz.user.mapper.LearningItemMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端知识点、前置关系和资源绑定服务。
 */
@Service
public class KnowledgePointAdminService {

    private static final String STATUS_ACTIVE =
            "ACTIVE";

    private static final String STATUS_INACTIVE =
            "INACTIVE";

    private static final String DIFFICULTY_BASIC =
            "BASIC";

    private static final String DIFFICULTY_INTERMEDIATE =
            "INTERMEDIATE";

    private static final String DIFFICULTY_ADVANCED =
            "ADVANCED";

    private static final BigDecimal DEFAULT_WARNING_THRESHOLD =
            new BigDecimal("60.00");

    private static final BigDecimal MIN_WARNING_THRESHOLD =
            new BigDecimal("0.00");

    private static final BigDecimal MAX_WARNING_THRESHOLD =
            new BigDecimal("100.00");

    private static final BigDecimal DEFAULT_WEIGHT =
            new BigDecimal("1.00");

    private static final BigDecimal MIN_WEIGHT =
            new BigDecimal("0.01");

    private static final BigDecimal MAX_WEIGHT =
            new BigDecimal("1.00");

    private final KnowledgePointMapper
            knowledgePointMapper;

    private final KnowledgePointPrerequisiteMapper
            prerequisiteMapper;

    private final LearningItemKnowledgePointMapper
            learningBindingMapper;

    private final AssignmentKnowledgePointMapper
            assignmentBindingMapper;

    private final LearningItemMapper
            learningItemMapper;

    private final AssignmentMapper
            assignmentMapper;

    public KnowledgePointAdminService(
            KnowledgePointMapper knowledgePointMapper,
            KnowledgePointPrerequisiteMapper prerequisiteMapper,
            LearningItemKnowledgePointMapper learningBindingMapper,
            AssignmentKnowledgePointMapper assignmentBindingMapper,
            LearningItemMapper learningItemMapper,
            AssignmentMapper assignmentMapper) {

        this.knowledgePointMapper =
                knowledgePointMapper;

        this.prerequisiteMapper =
                prerequisiteMapper;

        this.learningBindingMapper =
                learningBindingMapper;

        this.assignmentBindingMapper =
                assignmentBindingMapper;

        this.learningItemMapper =
                learningItemMapper;

        this.assignmentMapper =
                assignmentMapper;
    }

    /**
     * 分页查询知识点。
     */
    public PageResponse
            <KnowledgePointAdminDtos.KnowledgePointSummary>
    pageKnowledgePoints(
            String keyword,
            String status,
            String category,
            long page,
            long pageSize) {

        LambdaQueryWrapper<KnowledgePoint> wrapper =
                buildKnowledgePointQuery(
                        keyword,
                        status,
                        category
                );

        long total =
                knowledgePointMapper.selectCount(
                        wrapper
                );

        wrapper.orderByAsc(
                        KnowledgePoint::getSortOrder
                )
                .orderByAsc(
                        KnowledgePoint::getKnowledgePointId
                )
                .last(
                        "LIMIT "
                                + offset(page, pageSize)
                                + ", "
                                + pageSize
                );

        List<KnowledgePoint> knowledgePoints =
                knowledgePointMapper.selectList(
                        wrapper
                );

        if (knowledgePoints.isEmpty()) {
            return PageResponse.of(
                    Collections.emptyList(),
                    total,
                    page,
                    pageSize
            );
        }

        List<Long> knowledgePointIds =
                knowledgePoints.stream()
                        .map(
                                KnowledgePoint
                                        ::getKnowledgePointId
                        )
                        .collect(
                                Collectors.toList()
                        );

        Map<Long, Integer> prerequisiteCountMap =
                prerequisiteMapper.selectList(
                                new LambdaQueryWrapper
                                        <KnowledgePointPrerequisite>()
                                        .in(
                                                KnowledgePointPrerequisite
                                                        ::getKnowledgePointId,
                                                knowledgePointIds
                                        )
                        )
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        KnowledgePointPrerequisite
                                                ::getKnowledgePointId,
                                        Collectors.summingInt(
                                                relation -> 1
                                        )
                                )
                        );

        Map<Long, Integer> learningItemCountMap =
                learningBindingMapper.selectList(
                                new LambdaQueryWrapper
                                        <LearningItemKnowledgePoint>()
                                        .in(
                                                LearningItemKnowledgePoint
                                                        ::getKnowledgePointId,
                                                knowledgePointIds
                                        )
                        )
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        LearningItemKnowledgePoint
                                                ::getKnowledgePointId,
                                        Collectors.summingInt(
                                                relation -> 1
                                        )
                                )
                        );

        Map<Long, Integer> assignmentCountMap =
                assignmentBindingMapper.selectList(
                                new LambdaQueryWrapper
                                        <AssignmentKnowledgePoint>()
                                        .in(
                                                AssignmentKnowledgePoint
                                                        ::getKnowledgePointId,
                                                knowledgePointIds
                                        )
                        )
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        AssignmentKnowledgePoint
                                                ::getKnowledgePointId,
                                        Collectors.summingInt(
                                                relation -> 1
                                        )
                                )
                        );

        List<KnowledgePointAdminDtos.KnowledgePointSummary>
                records =
                knowledgePoints.stream()
                        .map(
                                knowledgePoint ->
                                        toSummary(
                                                knowledgePoint,
                                                prerequisiteCountMap
                                                        .getOrDefault(
                                                                knowledgePoint
                                                                        .getKnowledgePointId(),
                                                                0
                                                        ),
                                                learningItemCountMap
                                                        .getOrDefault(
                                                                knowledgePoint
                                                                        .getKnowledgePointId(),
                                                                0
                                                        ),
                                                assignmentCountMap
                                                        .getOrDefault(
                                                                knowledgePoint
                                                                        .getKnowledgePointId(),
                                                                0
                                                        )
                                        )
                        )
                        .collect(
                                Collectors.toList()
                        );

        return PageResponse.of(
                records,
                total,
                page,
                pageSize
        );
    }

    /**
     * 查询知识点管理统计信息。
     */
    public KnowledgePointAdminDtos.KnowledgePointStats
    getStats() {

        long totalCount =
                knowledgePointMapper.selectCount(
                        null
                );

        long activeCount =
                knowledgePointMapper.selectCount(
                        new LambdaQueryWrapper<KnowledgePoint>()
                                .eq(
                                        KnowledgePoint::getStatus,
                                        STATUS_ACTIVE
                                )
                );

        long inactiveCount =
                knowledgePointMapper.selectCount(
                        new LambdaQueryWrapper<KnowledgePoint>()
                                .eq(
                                        KnowledgePoint::getStatus,
                                        STATUS_INACTIVE
                                )
                );

        long boundLearningItemCount =
                learningBindingMapper.selectList(
                                null
                        )
                        .stream()
                        .map(
                                LearningItemKnowledgePoint
                                        ::getKnowledgePointId
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .distinct()
                        .count();

        long boundAssignmentCount =
                assignmentBindingMapper.selectList(
                                null
                        )
                        .stream()
                        .map(
                                AssignmentKnowledgePoint
                                        ::getKnowledgePointId
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .distinct()
                        .count();

        long prerequisiteConfiguredCount =
                prerequisiteMapper.selectList(
                                null
                        )
                        .stream()
                        .map(
                                KnowledgePointPrerequisite
                                        ::getKnowledgePointId
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .distinct()
                        .count();

        return KnowledgePointAdminDtos
                .KnowledgePointStats
                .builder()
                .totalCount(
                        totalCount
                )
                .activeCount(
                        activeCount
                )
                .inactiveCount(
                        inactiveCount
                )
                .boundLearningItemCount(
                        boundLearningItemCount
                )
                .boundAssignmentCount(
                        boundAssignmentCount
                )
                .prerequisiteConfiguredCount(
                        prerequisiteConfiguredCount
                )
                .build();
    }

    /**
     * 查询知识点完整详情。
     */
    public KnowledgePointAdminDtos.KnowledgePointDetail
    getDetail(
            Long knowledgePointId) {

        KnowledgePoint knowledgePoint =
                getKnowledgePointOrThrow(
                        knowledgePointId
                );

        List<KnowledgePointPrerequisite>
                prerequisiteRelations =
                prerequisiteMapper.selectList(
                        new LambdaQueryWrapper
                                <KnowledgePointPrerequisite>()
                                .eq(
                                        KnowledgePointPrerequisite
                                                ::getKnowledgePointId,
                                        knowledgePointId
                                )
                                .orderByAsc(
                                        KnowledgePointPrerequisite
                                                ::getId
                                )
                );

        Map<Long, KnowledgePoint> prerequisiteMap =
                selectKnowledgePointMap(
                        prerequisiteRelations.stream()
                                .map(
                                        KnowledgePointPrerequisite
                                                ::getPrerequisiteId
                                )
                                .collect(
                                        Collectors.toList()
                                )
                );

        List<KnowledgePointAdminDtos.PrerequisiteDetail>
                prerequisites =
                prerequisiteRelations.stream()
                        .map(
                                relation -> {
                                    KnowledgePoint prerequisite =
                                            prerequisiteMap.get(
                                                    relation
                                                            .getPrerequisiteId()
                                            );

                                    if (prerequisite == null) {
                                        return null;
                                    }

                                    return KnowledgePointAdminDtos
                                            .PrerequisiteDetail
                                            .builder()
                                            .relationId(
                                                    relation.getId()
                                            )
                                            .knowledgePointId(
                                                    prerequisite
                                                            .getKnowledgePointId()
                                            )
                                            .code(
                                                    prerequisite.getCode()
                                            )
                                            .name(
                                                    prerequisite.getName()
                                            )
                                            .category(
                                                    prerequisite.getCategory()
                                            )
                                            .difficulty(
                                                    prerequisite.getDifficulty()
                                            )
                                            .status(
                                                    prerequisite.getStatus()
                                            )
                                            .build();
                                }
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .collect(
                                Collectors.toList()
                        );

        List<LearningItemKnowledgePoint>
                learningRelations =
                learningBindingMapper.selectList(
                        new LambdaQueryWrapper
                                <LearningItemKnowledgePoint>()
                                .eq(
                                        LearningItemKnowledgePoint
                                                ::getKnowledgePointId,
                                        knowledgePointId
                                )
                                .orderByAsc(
                                        LearningItemKnowledgePoint
                                                ::getId
                                )
                );

        Map<Long, LearningItem> learningItemMap =
                selectLearningItemMap(
                        learningRelations.stream()
                                .map(
                                        LearningItemKnowledgePoint
                                                ::getItemPk
                                )
                                .collect(
                                        Collectors.toList()
                                )
                );

        List<KnowledgePointAdminDtos.LearningItemBindingDetail>
                learningItemBindings =
                learningRelations.stream()
                        .map(
                                relation -> {
                                    LearningItem item =
                                            learningItemMap.get(
                                                    relation.getItemPk()
                                            );

                                    if (item == null) {
                                        return null;
                                    }

                                    return KnowledgePointAdminDtos
                                            .LearningItemBindingDetail
                                            .builder()
                                            .relationId(
                                                    relation.getId()
                                            )
                                            .itemPk(
                                                    item.getItemPk()
                                            )
                                            .title(
                                                    item.getTitle()
                                            )
                                            .category(
                                                    item.getCategory()
                                            )
                                            .difficulty(
                                                    item.getDifficulty()
                                            )
                                            .status(
                                                    item.getStatus()
                                            )
                                            .weight(
                                                    relation.getWeight()
                                            )
                                            .build();
                                }
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .collect(
                                Collectors.toList()
                        );

        List<AssignmentKnowledgePoint>
                assignmentRelations =
                assignmentBindingMapper.selectList(
                        new LambdaQueryWrapper
                                <AssignmentKnowledgePoint>()
                                .eq(
                                        AssignmentKnowledgePoint
                                                ::getKnowledgePointId,
                                        knowledgePointId
                                )
                                .orderByAsc(
                                        AssignmentKnowledgePoint
                                                ::getId
                                )
                );

        Map<Long, Assignment> assignmentMap =
                selectAssignmentMap(
                        assignmentRelations.stream()
                                .map(
                                        AssignmentKnowledgePoint
                                                ::getAssignmentId
                                )
                                .collect(
                                        Collectors.toList()
                                )
                );

        List<KnowledgePointAdminDtos.AssignmentBindingDetail>
                assignmentBindings =
                assignmentRelations.stream()
                        .map(
                                relation -> {
                                    Assignment assignment =
                                            assignmentMap.get(
                                                    relation
                                                            .getAssignmentId()
                                            );

                                    if (assignment == null) {
                                        return null;
                                    }

                                    return KnowledgePointAdminDtos
                                            .AssignmentBindingDetail
                                            .builder()
                                            .relationId(
                                                    relation.getId()
                                            )
                                            .assignmentId(
                                                    assignment
                                                            .getAssignmentId()
                                            )
                                            .title(
                                                    assignment.getTitle()
                                            )
                                            .category(
                                                    assignment.getCategory()
                                            )
                                            .status(
                                                    assignment.getStatus()
                                            )
                                            .totalScore(
                                                    assignment
                                                            .getTotalScore()
                                            )
                                            .deadline(
                                                    assignment.getDeadline()
                                            )
                                            .weight(
                                                    relation.getWeight()
                                            )
                                            .build();
                                }
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .collect(
                                Collectors.toList()
                        );

        return KnowledgePointAdminDtos
                .KnowledgePointDetail
                .builder()
                .knowledgePointId(
                        knowledgePoint
                                .getKnowledgePointId()
                )
                .code(
                        knowledgePoint.getCode()
                )
                .name(
                        knowledgePoint.getName()
                )
                .description(
                        knowledgePoint.getDescription()
                )
                .category(
                        knowledgePoint.getCategory()
                )
                .difficulty(
                        knowledgePoint.getDifficulty()
                )
                .status(
                        knowledgePoint.getStatus()
                )
                .warningThreshold(
                        knowledgePoint
                                .getWarningThreshold()
                )
                .sortOrder(
                        knowledgePoint.getSortOrder()
                )
                .createdBy(
                        knowledgePoint.getCreatedBy()
                )
                .createdAt(
                        knowledgePoint.getCreatedAt()
                )
                .updatedAt(
                        knowledgePoint.getUpdatedAt()
                )
                .prerequisites(
                        prerequisites
                )
                .learningItemBindings(
                        learningItemBindings
                )
                .assignmentBindings(
                        assignmentBindings
                )
                .build();
    }

    /**
     * 创建知识点并保存全部关系。
     */
    @Transactional
    public KnowledgePointAdminDtos.KnowledgePointDetail
    create(
            KnowledgePointAdminDtos
                    .KnowledgePointSaveRequest request,
            User operator) {

        validateRequest(
                request,
                null
        );

        LocalDateTime now =
                LocalDateTime.now();

        KnowledgePoint knowledgePoint =
                new KnowledgePoint();

        applyRequest(
                knowledgePoint,
                request
        );

        knowledgePoint.setCreatedBy(
                operator == null
                        ? null
                        : operator.getId()
        );

        knowledgePoint.setCreatedAt(
                now
        );

        knowledgePoint.setUpdatedAt(
                now
        );

        try {
            knowledgePointMapper.insert(
                    knowledgePoint
            );

            replaceRelations(
                    knowledgePoint
                            .getKnowledgePointId(),
                    request
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "知识点编码已存在，或关联数据违反唯一约束"
            );
        }

        return getDetail(
                knowledgePoint
                        .getKnowledgePointId()
        );
    }

    /**
     * 更新知识点并整体替换全部关系。
     */
    @Transactional
    public KnowledgePointAdminDtos.KnowledgePointDetail
    update(
            Long knowledgePointId,
            KnowledgePointAdminDtos
                    .KnowledgePointSaveRequest request) {

        KnowledgePoint existing =
                getKnowledgePointOrThrow(
                        knowledgePointId
                );

        validateRequest(
                request,
                knowledgePointId
        );

        applyRequest(
                existing,
                request
        );

        existing.setUpdatedAt(
                LocalDateTime.now()
        );

        try {
            knowledgePointMapper.updateById(
                    existing
            );

            replaceRelations(
                    knowledgePointId,
                    request
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "知识点编码已存在，或关联数据违反唯一约束"
            );
        }

        return getDetail(
                knowledgePointId
        );
    }

    /**
     * 单独启用或停用知识点。
     */
    @Transactional
    public KnowledgePointAdminDtos.KnowledgePointDetail
    updateStatus(
            Long knowledgePointId,
            String status) {

        KnowledgePoint knowledgePoint =
                getKnowledgePointOrThrow(
                        knowledgePointId
                );

        knowledgePoint.setStatus(
                normalizeStatus(
                        status
                )
        );

        knowledgePoint.setUpdatedAt(
                LocalDateTime.now()
        );

        knowledgePointMapper.updateById(
                knowledgePoint
        );

        return getDetail(
                knowledgePointId
        );
    }

    /**
     * 查询前置知识点选择项。
     */
    public List<KnowledgePointAdminDtos.KnowledgePointOption>
    listKnowledgePointOptions(
            Long excludeKnowledgePointId) {

        LambdaQueryWrapper<KnowledgePoint> wrapper =
                new LambdaQueryWrapper<KnowledgePoint>()
                        .ne(
                                excludeKnowledgePointId != null,
                                KnowledgePoint
                                        ::getKnowledgePointId,
                                excludeKnowledgePointId
                        )
                        .orderByAsc(
                                KnowledgePoint::getSortOrder
                        )
                        .orderByAsc(
                                KnowledgePoint
                                        ::getKnowledgePointId
                        );

        return knowledgePointMapper.selectList(
                        wrapper
                )
                .stream()
                .map(
                        knowledgePoint ->
                                KnowledgePointAdminDtos
                                        .KnowledgePointOption
                                        .builder()
                                        .knowledgePointId(
                                                knowledgePoint
                                                        .getKnowledgePointId()
                                        )
                                        .code(
                                                knowledgePoint.getCode()
                                        )
                                        .name(
                                                knowledgePoint.getName()
                                        )
                                        .category(
                                                knowledgePoint.getCategory()
                                        )
                                        .difficulty(
                                                knowledgePoint.getDifficulty()
                                        )
                                        .status(
                                                knowledgePoint.getStatus()
                                        )
                                        .build()
                )
                .collect(
                        Collectors.toList()
                );
    }

    /**
     * 查询学习资源选择项。
     */
    public List<KnowledgePointAdminDtos.LearningItemOption>
    listLearningItemOptions() {

        return learningItemMapper.selectList(
                        new LambdaQueryWrapper<LearningItem>()
                                .orderByDesc(
                                        LearningItem::getUpdatedAt
                                )
                                .orderByDesc(
                                        LearningItem::getItemPk
                                )
                )
                .stream()
                .map(
                        item ->
                                KnowledgePointAdminDtos
                                        .LearningItemOption
                                        .builder()
                                        .itemPk(
                                                item.getItemPk()
                                        )
                                        .title(
                                                item.getTitle()
                                        )
                                        .category(
                                                item.getCategory()
                                        )
                                        .difficulty(
                                                item.getDifficulty()
                                        )
                                        .status(
                                                item.getStatus()
                                        )
                                        .build()
                )
                .collect(
                        Collectors.toList()
                );
    }

    /**
     * 查询能力考核选择项。
     */
    public List<KnowledgePointAdminDtos.AssignmentOption>
    listAssignmentOptions() {

        return assignmentMapper.selectList(
                        new LambdaQueryWrapper<Assignment>()
                                .orderByDesc(
                                        Assignment::getUpdatedAt
                                )
                                .orderByDesc(
                                        Assignment::getAssignmentId
                                )
                )
                .stream()
                .map(
                        assignment ->
                                KnowledgePointAdminDtos
                                        .AssignmentOption
                                        .builder()
                                        .assignmentId(
                                                assignment
                                                        .getAssignmentId()
                                        )
                                        .title(
                                                assignment.getTitle()
                                        )
                                        .category(
                                                assignment.getCategory()
                                        )
                                        .status(
                                                assignment.getStatus()
                                        )
                                        .totalScore(
                                                assignment.getTotalScore()
                                        )
                                        .deadline(
                                                assignment.getDeadline()
                                        )
                                        .build()
                )
                .collect(
                        Collectors.toList()
                );
    }

    private LambdaQueryWrapper<KnowledgePoint>
    buildKnowledgePointQuery(
            String keyword,
            String status,
            String category) {

        LambdaQueryWrapper<KnowledgePoint> wrapper =
                new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword =
                    keyword.trim();

            wrapper.and(
                    nested ->
                            nested.like(
                                            KnowledgePoint::getCode,
                                            trimmedKeyword
                                    )
                                    .or()
                                    .like(
                                            KnowledgePoint::getName,
                                            trimmedKeyword
                                    )
                                    .or()
                                    .like(
                                            KnowledgePoint
                                                    ::getDescription,
                                            trimmedKeyword
                                    )
            );
        }

        if (StringUtils.hasText(status)) {
            wrapper.eq(
                    KnowledgePoint::getStatus,
                    normalizeStatus(status)
            );
        }

        if (StringUtils.hasText(category)) {
            wrapper.eq(
                    KnowledgePoint::getCategory,
                    category.trim()
            );
        }

        return wrapper;
    }

    private KnowledgePointAdminDtos.KnowledgePointSummary
    toSummary(
            KnowledgePoint knowledgePoint,
            int prerequisiteCount,
            int learningItemCount,
            int assignmentCount) {

        return KnowledgePointAdminDtos
                .KnowledgePointSummary
                .builder()
                .knowledgePointId(
                        knowledgePoint
                                .getKnowledgePointId()
                )
                .code(
                        knowledgePoint.getCode()
                )
                .name(
                        knowledgePoint.getName()
                )
                .description(
                        knowledgePoint.getDescription()
                )
                .category(
                        knowledgePoint.getCategory()
                )
                .difficulty(
                        knowledgePoint.getDifficulty()
                )
                .status(
                        knowledgePoint.getStatus()
                )
                .warningThreshold(
                        knowledgePoint
                                .getWarningThreshold()
                )
                .sortOrder(
                        knowledgePoint.getSortOrder()
                )
                .prerequisiteCount(
                        prerequisiteCount
                )
                .learningItemCount(
                        learningItemCount
                )
                .assignmentCount(
                        assignmentCount
                )
                .createdBy(
                        knowledgePoint.getCreatedBy()
                )
                .createdAt(
                        knowledgePoint.getCreatedAt()
                )
                .updatedAt(
                        knowledgePoint.getUpdatedAt()
                )
                .build();
    }

    private void validateRequest(
            KnowledgePointAdminDtos
                    .KnowledgePointSaveRequest request,
            Long currentKnowledgePointId) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "请求内容不能为空"
            );
        }

        if (!StringUtils.hasText(
                request.getCode()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "知识点编码不能为空"
            );
        }

        if (!StringUtils.hasText(
                request.getName()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "知识点名称不能为空"
            );
        }

        String normalizedCode =
                normalizeCode(
                        request.getCode()
                );

        long duplicatedCodeCount =
                knowledgePointMapper.selectCount(
                        new LambdaQueryWrapper<KnowledgePoint>()
                                .eq(
                                        KnowledgePoint::getCode,
                                        normalizedCode
                                )
                                .ne(
                                        currentKnowledgePointId != null,
                                        KnowledgePoint
                                                ::getKnowledgePointId,
                                        currentKnowledgePointId
                                )
                );

        if (duplicatedCodeCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "知识点编码已存在"
            );
        }

        normalizeDifficulty(
                request.getDifficulty()
        );

        normalizeStatus(
                request.getStatus()
        );

        normalizeWarningThreshold(
                request.getWarningThreshold()
        );

        normalizeSortOrder(
                request.getSortOrder()
        );

        Set<Long> prerequisiteIds =
                normalizePrerequisiteIds(
                        request.getPrerequisiteIds(),
                        currentKnowledgePointId
                );

        validatePrerequisiteCycles(
                currentKnowledgePointId,
                prerequisiteIds
        );

        validateLearningBindings(
                request.getLearningItemBindings()
        );

        validateAssignmentBindings(
                request.getAssignmentBindings()
        );
    }

    /**
     * 防止出现 A 依赖 B、B 又依赖 A 的循环关系。
     */
    private void validatePrerequisiteCycles(
            Long currentKnowledgePointId,
            Set<Long> prerequisiteIds) {

        if (currentKnowledgePointId == null
                || prerequisiteIds.isEmpty()) {
            return;
        }

        for (Long prerequisiteId : prerequisiteIds) {
            if (canReachKnowledgePoint(
                    prerequisiteId,
                    currentKnowledgePointId,
                    new HashSet<>()
            )) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "前置知识点关系会形成循环依赖"
                );
            }
        }
    }

    private boolean canReachKnowledgePoint(
            Long currentId,
            Long targetId,
            Set<Long> visited) {

        if (Objects.equals(
                currentId,
                targetId
        )) {
            return true;
        }

        if (currentId == null
                || !visited.add(currentId)) {
            return false;
        }

        List<KnowledgePointPrerequisite> relations =
                prerequisiteMapper.selectList(
                        new LambdaQueryWrapper
                                <KnowledgePointPrerequisite>()
                                .eq(
                                        KnowledgePointPrerequisite
                                                ::getKnowledgePointId,
                                        currentId
                                )
                );

        for (KnowledgePointPrerequisite relation
                : relations) {

            if (canReachKnowledgePoint(
                    relation.getPrerequisiteId(),
                    targetId,
                    visited
            )) {
                return true;
            }
        }

        return false;
    }

    private Set<Long> normalizePrerequisiteIds(
            List<Long> prerequisiteIds,
            Long currentKnowledgePointId) {

        if (prerequisiteIds == null
                || prerequisiteIds.isEmpty()) {
            return Collections.emptySet();
        }

        LinkedHashSet<Long> normalizedIds =
                new LinkedHashSet<>();

        for (Long prerequisiteId : prerequisiteIds) {
            if (prerequisiteId == null
                    || prerequisiteId <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "前置知识点编号不正确"
                );
            }

            if (Objects.equals(
                    prerequisiteId,
                    currentKnowledgePointId
            )) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "知识点不能将自身设为前置知识点"
                );
            }

            if (!normalizedIds.add(
                    prerequisiteId
            )) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "前置知识点不能重复"
                );
            }

            if (knowledgePointMapper.selectById(
                    prerequisiteId
            ) == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "前置知识点不存在："
                                + prerequisiteId
                );
            }
        }

        return normalizedIds;
    }

    private void validateLearningBindings(
            List<KnowledgePointAdminDtos
                    .LearningItemBindingInput> bindings) {

        if (bindings == null
                || bindings.isEmpty()) {
            return;
        }

        Set<Long> itemIds =
                new HashSet<>();

        for (KnowledgePointAdminDtos
                .LearningItemBindingInput binding
                : bindings) {

            if (binding == null
                    || binding.getItemPk() == null
                    || binding.getItemPk() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "学习资源编号不正确"
                );
            }

            if (!itemIds.add(
                    binding.getItemPk()
            )) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "同一学习资源不能重复绑定"
                );
            }

            if (learningItemMapper.selectById(
                    binding.getItemPk()
            ) == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "学习资源不存在："
                                + binding.getItemPk()
                );
            }

            normalizeWeight(
                    binding.getWeight()
            );
        }
    }

    private void validateAssignmentBindings(
            List<KnowledgePointAdminDtos
                    .AssignmentBindingInput> bindings) {

        if (bindings == null
                || bindings.isEmpty()) {
            return;
        }

        Set<Long> assignmentIds =
                new HashSet<>();

        for (KnowledgePointAdminDtos
                .AssignmentBindingInput binding
                : bindings) {

            if (binding == null
                    || binding.getAssignmentId() == null
                    || binding.getAssignmentId() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "能力考核编号不正确"
                );
            }

            if (!assignmentIds.add(
                    binding.getAssignmentId()
            )) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "同一能力考核不能重复绑定"
                );
            }

            if (assignmentMapper.selectById(
                    binding.getAssignmentId()
            ) == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "能力考核不存在："
                                + binding.getAssignmentId()
                );
            }

            normalizeWeight(
                    binding.getWeight()
            );
        }
    }

    private void applyRequest(
            KnowledgePoint knowledgePoint,
            KnowledgePointAdminDtos
                    .KnowledgePointSaveRequest request) {

        knowledgePoint.setCode(
                normalizeCode(
                        request.getCode()
                )
        );

        knowledgePoint.setName(
                request.getName().trim()
        );

        knowledgePoint.setDescription(
                trimToNull(
                        request.getDescription()
                )
        );

        knowledgePoint.setCategory(
                trimToNull(
                        request.getCategory()
                )
        );

        knowledgePoint.setDifficulty(
                normalizeDifficulty(
                        request.getDifficulty()
                )
        );

        knowledgePoint.setStatus(
                normalizeStatus(
                        request.getStatus()
                )
        );

        knowledgePoint.setWarningThreshold(
                normalizeWarningThreshold(
                        request.getWarningThreshold()
                )
        );

        knowledgePoint.setSortOrder(
                normalizeSortOrder(
                        request.getSortOrder()
                )
        );
    }

    private void replaceRelations(
            Long knowledgePointId,
            KnowledgePointAdminDtos
                    .KnowledgePointSaveRequest request) {

        prerequisiteMapper.delete(
                new LambdaQueryWrapper
                        <KnowledgePointPrerequisite>()
                        .eq(
                                KnowledgePointPrerequisite
                                        ::getKnowledgePointId,
                                knowledgePointId
                        )
        );

        learningBindingMapper.delete(
                new LambdaQueryWrapper
                        <LearningItemKnowledgePoint>()
                        .eq(
                                LearningItemKnowledgePoint
                                        ::getKnowledgePointId,
                                knowledgePointId
                        )
        );

        assignmentBindingMapper.delete(
                new LambdaQueryWrapper
                        <AssignmentKnowledgePoint>()
                        .eq(
                                AssignmentKnowledgePoint
                                        ::getKnowledgePointId,
                                knowledgePointId
                        )
        );

        LocalDateTime now =
                LocalDateTime.now();

        Set<Long> prerequisiteIds =
                normalizePrerequisiteIds(
                        request.getPrerequisiteIds(),
                        knowledgePointId
                );

        for (Long prerequisiteId : prerequisiteIds) {
            KnowledgePointPrerequisite relation =
                    new KnowledgePointPrerequisite();

            relation.setKnowledgePointId(
                    knowledgePointId
            );

            relation.setPrerequisiteId(
                    prerequisiteId
            );

            relation.setCreatedAt(
                    now
            );

            prerequisiteMapper.insert(
                    relation
            );
        }

        List<KnowledgePointAdminDtos
                .LearningItemBindingInput>
                learningBindings =
                request.getLearningItemBindings();

        if (learningBindings != null) {
            for (KnowledgePointAdminDtos
                    .LearningItemBindingInput binding
                    : learningBindings) {

                LearningItemKnowledgePoint relation =
                        new LearningItemKnowledgePoint();

                relation.setItemPk(
                        binding.getItemPk()
                );

                relation.setKnowledgePointId(
                        knowledgePointId
                );

                relation.setWeight(
                        normalizeWeight(
                                binding.getWeight()
                        )
                );

                relation.setCreatedAt(
                        now
                );

                learningBindingMapper.insert(
                        relation
                );
            }
        }

        List<KnowledgePointAdminDtos
                .AssignmentBindingInput>
                assignmentBindings =
                request.getAssignmentBindings();

        if (assignmentBindings != null) {
            for (KnowledgePointAdminDtos
                    .AssignmentBindingInput binding
                    : assignmentBindings) {

                AssignmentKnowledgePoint relation =
                        new AssignmentKnowledgePoint();

                relation.setAssignmentId(
                        binding.getAssignmentId()
                );

                relation.setKnowledgePointId(
                        knowledgePointId
                );

                relation.setWeight(
                        normalizeWeight(
                                binding.getWeight()
                        )
                );

                relation.setCreatedAt(
                        now
                );

                assignmentBindingMapper.insert(
                        relation
                );
            }
        }
    }

    private String normalizeCode(
            String code) {

        return code.trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private String normalizeDifficulty(
            String difficulty) {

        if (!StringUtils.hasText(
                difficulty
        )) {
            return DIFFICULTY_INTERMEDIATE;
        }

        String normalized =
                difficulty.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (DIFFICULTY_BASIC.equals(
                normalized
        )
                || DIFFICULTY_INTERMEDIATE.equals(
                normalized
        )
                || DIFFICULTY_ADVANCED.equals(
                normalized
        )) {
            return normalized;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "知识点难度必须是 BASIC、INTERMEDIATE 或 ADVANCED"
        );
    }

    private String normalizeStatus(
            String status) {

        if (!StringUtils.hasText(
                status
        )) {
            return STATUS_ACTIVE;
        }

        String normalized =
                status.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (STATUS_ACTIVE.equals(
                normalized
        )
                || STATUS_INACTIVE.equals(
                normalized
        )) {
            return normalized;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "知识点状态必须是 ACTIVE 或 INACTIVE"
        );
    }

    private BigDecimal normalizeWarningThreshold(
            BigDecimal warningThreshold) {

        BigDecimal normalized =
                warningThreshold == null
                        ? DEFAULT_WARNING_THRESHOLD
                        : warningThreshold;

        if (normalized.compareTo(
                MIN_WARNING_THRESHOLD
        ) < 0
                || normalized.compareTo(
                MAX_WARNING_THRESHOLD
        ) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "预警阈值必须在 0 到 100 之间"
            );
        }

        return normalized.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private int normalizeSortOrder(
            Integer sortOrder) {

        if (sortOrder == null) {
            return 0;
        }

        if (sortOrder < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "显示顺序不能小于 0"
            );
        }

        return sortOrder;
    }

    private BigDecimal normalizeWeight(
            BigDecimal weight) {

        BigDecimal normalized =
                weight == null
                        ? DEFAULT_WEIGHT
                        : weight;

        if (normalized.compareTo(
                MIN_WEIGHT
        ) < 0
                || normalized.compareTo(
                MAX_WEIGHT
        ) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "绑定权重必须在 0.01 到 1.00 之间"
            );
        }

        return normalized.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private KnowledgePoint getKnowledgePointOrThrow(
            Long knowledgePointId) {

        if (knowledgePointId == null
                || knowledgePointId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "知识点编号不正确"
            );
        }

        KnowledgePoint knowledgePoint =
                knowledgePointMapper.selectById(
                        knowledgePointId
                );

        if (knowledgePoint == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "知识点不存在"
            );
        }

        return knowledgePoint;
    }

    private Map<Long, KnowledgePoint>
    selectKnowledgePointMap(
            List<Long> knowledgePointIds) {

        if (knowledgePointIds == null
                || knowledgePointIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return knowledgePointMapper.selectBatchIds(
                        new LinkedHashSet<>(
                                knowledgePointIds
                        )
                )
                .stream()
                .collect(
                        Collectors.toMap(
                                KnowledgePoint
                                        ::getKnowledgePointId,
                                item -> item
                        )
                );
    }

    private Map<Long, LearningItem>
    selectLearningItemMap(
            List<Long> itemIds) {

        if (itemIds == null
                || itemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return learningItemMapper.selectBatchIds(
                        new LinkedHashSet<>(
                                itemIds
                        )
                )
                .stream()
                .collect(
                        Collectors.toMap(
                                LearningItem::getItemPk,
                                item -> item
                        )
                );
    }

    private Map<Long, Assignment>
    selectAssignmentMap(
            List<Long> assignmentIds) {

        if (assignmentIds == null
                || assignmentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return assignmentMapper.selectBatchIds(
                        new LinkedHashSet<>(
                                assignmentIds
                        )
                )
                .stream()
                .collect(
                        Collectors.toMap(
                                Assignment
                                        ::getAssignmentId,
                                item -> item
                        )
                );
    }

    private String trimToNull(
            String value) {

        return StringUtils.hasText(
                value
        )
                ? value.trim()
                : null;
    }

    private long offset(
            long page,
            long pageSize) {

        return (page - 1)
                * pageSize;
    }
}