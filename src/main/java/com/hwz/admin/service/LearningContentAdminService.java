package com.hwz.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwz.admin.dto.AdminLearningItemDetailResponse;
import com.hwz.admin.dto.AdminLearningItemSaveRequest;
import com.hwz.admin.dto.AdminLearningItemSummaryResponse;
import com.hwz.common.PageResponse;
import com.hwz.common.entity.User;
import com.hwz.user.dto.LearningFeature;
import com.hwz.user.entity.LearningItem;
import com.hwz.user.entity.LearningStep;
import com.hwz.user.mapper.LearningItemMapper;
import com.hwz.user.mapper.LearningStepMapper;
import com.hwz.user.service.LearningTemplateStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class LearningContentAdminService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String STATUS_NO_MATCH = "__NO_MATCH__";
    private static final String STATIC_EXPERIMENT_BASE = "classpath:/static/experiments/";

    private final LearningItemMapper learningItemMapper;
    private final LearningStepMapper learningStepMapper;
    private final LearningTemplateStorageService templateStorageService;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public LearningContentAdminService(LearningItemMapper learningItemMapper,
                                       LearningStepMapper learningStepMapper,
                                       LearningTemplateStorageService templateStorageService,
                                       ResourceLoader resourceLoader,
                                       ObjectMapper objectMapper) {
        this.learningItemMapper = learningItemMapper;
        this.learningStepMapper = learningStepMapper;
        this.templateStorageService = templateStorageService;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    public List<AdminLearningItemSummaryResponse> listItems(String keyword, String status, String category) {
        return pageItems(keyword, status, category, 1, 100).getRecords();
    }

    public PageResponse<AdminLearningItemSummaryResponse> pageItems(String keyword, String status, String category, long page, long pageSize) {
        LambdaQueryWrapper<LearningItem> wrapper = itemQuery(keyword, status, category);
        long total = learningItemMapper.selectCount(wrapper);
        wrapper.orderByDesc(LearningItem::getUpdatedAt).orderByDesc(LearningItem::getItemPk);
        wrapper.last("LIMIT " + offset(page, pageSize) + ", " + pageSize);
        List<LearningItem> items = learningItemMapper.selectList(wrapper);
        if (items.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), total, page, pageSize);
        }

        List<Long> itemIds = items.stream().map(LearningItem::getItemPk).collect(Collectors.toList());
        Map<Long, Integer> stepCountMap = learningStepMapper.selectList(
                new LambdaQueryWrapper<LearningStep>().in(LearningStep::getItemPk, itemIds)
        ).stream().collect(Collectors.groupingBy(LearningStep::getItemPk, Collectors.summingInt(step -> 1)));

        List<AdminLearningItemSummaryResponse> records = items.stream()
                .map(item -> AdminLearningItemSummaryResponse.builder()
                        .id(item.getItemPk())
                        .title(item.getTitle())
                        .category(item.getCategory())
                        .difficulty(item.getDifficulty())
                        .duration(item.getDuration())
                        .status(normalizeExistingStatus(item.getStatus()))
                        .authorId(item.getAuthorId())
                        .stepCount(stepCountMap.getOrDefault(item.getItemPk(), 0))
                        .templateAvailable(hasTemplate(item))
                        .publishedAt(item.getPublishedAt())
                        .updatedAt(item.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
        return PageResponse.of(records, total, page, pageSize);
    }

    public Map<String, Long> stats(String keyword, String status, String category) {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", learningItemMapper.selectCount(itemQuery(keyword, status, category)));
        stats.put("draft", learningItemMapper.selectCount(itemQuery(keyword, restrictStatus(status, STATUS_DRAFT), category)));
        stats.put("published", learningItemMapper.selectCount(itemQuery(keyword, restrictStatus(status, STATUS_PUBLISHED), category)));
        stats.put("archived", learningItemMapper.selectCount(itemQuery(keyword, restrictStatus(status, STATUS_ARCHIVED), category)));
        return stats;
    }

    private LambdaQueryWrapper<LearningItem> itemQuery(String keyword, String status, String category) {
        LambdaQueryWrapper<LearningItem> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(LearningItem::getTitle, keyword.trim())
                    .or()
                    .like(LearningItem::getSummary, keyword.trim()));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(LearningItem::getStatus, STATUS_NO_MATCH.equals(status) ? STATUS_NO_MATCH : normalizeStatus(status));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(LearningItem::getCategory, category.trim());
        }
        return wrapper;
    }

    private String restrictStatus(String selectedStatus, String statStatus) {
        if (!StringUtils.hasText(selectedStatus)) {
            return statStatus;
        }
        return statStatus.equalsIgnoreCase(selectedStatus.trim()) ? statStatus : STATUS_NO_MATCH;
    }

    private long offset(long page, long pageSize) {
        return (page - 1) * pageSize;
    }

    public AdminLearningItemDetailResponse getDetail(Long itemId) {
        LearningItem item = getItemOrThrow(itemId);
        return toDetailResponse(item);
    }

    @Transactional
    public AdminLearningItemDetailResponse createItem(AdminLearningItemSaveRequest request, User operator) {
        validateSaveRequest(request);
        LocalDateTime now = LocalDateTime.now();
        LearningItem item = LearningItem.builder()
                .jsonId(generateJsonId())
                .title(request.getTitle().trim())
                .summary(trimToNull(request.getSummary()))
                .category(trimToNull(request.getCategory()))
                .difficulty(trimToNull(request.getDifficulty()))
                .duration(trimToNull(request.getDuration()))
                .prerequisites(trimToNull(request.getPrerequisites()))
                .objectives(writeObjectives(request.getObjectives()))
                .features(writeFeatures(request.getFeatures()))
                .status(STATUS_DRAFT)
                .authorId(operator == null ? null : operator.getId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        learningItemMapper.insert(item);
        replaceSteps(item.getItemPk(), request.getSteps());
        return toDetailResponse(getItemOrThrow(item.getItemPk()));
    }

    @Transactional
    public AdminLearningItemDetailResponse updateItem(Long itemId, AdminLearningItemSaveRequest request) {
        validateSaveRequest(request);
        LearningItem existing = getItemOrThrow(itemId);
        existing.setTitle(request.getTitle().trim());
        existing.setSummary(trimToNull(request.getSummary()));
        existing.setCategory(trimToNull(request.getCategory()));
        existing.setDifficulty(trimToNull(request.getDifficulty()));
        existing.setDuration(trimToNull(request.getDuration()));
        existing.setPrerequisites(trimToNull(request.getPrerequisites()));
        existing.setObjectives(writeObjectives(request.getObjectives()));
        existing.setFeatures(writeFeatures(request.getFeatures()));
        existing.setUpdatedAt(LocalDateTime.now());
        learningItemMapper.updateById(existing);
        replaceSteps(itemId, request.getSteps());
        return toDetailResponse(getItemOrThrow(itemId));
    }

    @Transactional
    public AdminLearningItemDetailResponse updateStatus(Long itemId, String status) {
        LearningItem existing = getItemOrThrow(itemId);
        String normalizedStatus = normalizeStatus(status);
        if (STATUS_PUBLISHED.equals(normalizedStatus) && !hasTemplate(existing)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Publish requires a notebook template");
        }
        existing.setStatus(normalizedStatus);
        existing.setUpdatedAt(LocalDateTime.now());
        if (STATUS_PUBLISHED.equals(normalizedStatus) && existing.getPublishedAt() == null) {
            existing.setPublishedAt(LocalDateTime.now());
        }
        learningItemMapper.updateById(existing);
        return toDetailResponse(existing);
    }

    @Transactional
    public AdminLearningItemDetailResponse uploadTemplate(Long itemId, MultipartFile file) {
        LearningItem existing = getItemOrThrow(itemId);
        try {
            String storedPath = templateStorageService.storeTemplate(itemId, file);
            existing.setTemplatePath(storedPath);
            existing.setUpdatedAt(LocalDateTime.now());
            learningItemMapper.updateById(existing);
            return toDetailResponse(existing);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    public Resource resolveTemplateResource(LearningItem item) {
        if (item != null && StringUtils.hasText(item.getTemplatePath()) && templateStorageService.templateExists(item.getTemplatePath())) {
            return templateStorageService.loadTemplate(item.getTemplatePath());
        }
        Resource fallback = resourceLoader.getResource(STATIC_EXPERIMENT_BASE + "exp" + item.getItemPk() + ".ipynb");
        if (fallback.exists()) {
            return fallback;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment notebook not found");
    }

    public boolean hasTemplate(LearningItem item) {
        if (item == null || item.getItemPk() == null) {
            return false;
        }
        if (StringUtils.hasText(item.getTemplatePath()) && templateStorageService.templateExists(item.getTemplatePath())) {
            return true;
        }
        Resource fallback = resourceLoader.getResource(STATIC_EXPERIMENT_BASE + "exp" + item.getItemPk() + ".ipynb");
        return fallback.exists();
    }

    public LearningItem getItemOrThrow(Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid learning item id");
        }
        LearningItem item = learningItemMapper.selectById(itemId);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning item not found");
        }
        return item;
    }

    private AdminLearningItemDetailResponse toDetailResponse(LearningItem item) {
        List<LearningStep> steps = learningStepMapper.selectList(
                new LambdaQueryWrapper<LearningStep>()
                        .eq(LearningStep::getItemPk, item.getItemPk())
                        .orderByAsc(LearningStep::getStepNo)
                        .orderByAsc(LearningStep::getStepId)
        );

        return AdminLearningItemDetailResponse.builder()
                .id(item.getItemPk())
                .jsonId(item.getJsonId())
                .title(item.getTitle())
                .summary(item.getSummary())
                .category(item.getCategory())
                .difficulty(item.getDifficulty())
                .duration(item.getDuration())
                .prerequisites(item.getPrerequisites())
                .objectives(readObjectives(item.getObjectives()))
                .features(readFeatures(item.getFeatures()))
                .steps(steps.stream()
                        .map(step -> AdminLearningItemDetailResponse.StepDetail.builder()
                                .stepNo(step.getStepNo())
                                .title(step.getTitle())
                                .description(step.getDescription())
                                .tip(step.getTip())
                                .code(step.getCode())
                                .build())
                        .collect(Collectors.toList()))
                .status(normalizeExistingStatus(item.getStatus()))
                .templatePath(item.getTemplatePath())
                .templateAvailable(hasTemplate(item))
                .authorId(item.getAuthorId())
                .publishedAt(item.getPublishedAt())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private void validateSaveRequest(AdminLearningItemSaveRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
    }

    private void replaceSteps(Long itemId, List<AdminLearningItemSaveRequest.StepInput> stepInputs) {
        learningStepMapper.delete(new LambdaQueryWrapper<LearningStep>().eq(LearningStep::getItemPk, itemId));
        if (stepInputs == null || stepInputs.isEmpty()) {
            return;
        }
        List<AdminLearningItemSaveRequest.StepInput> orderedSteps = new ArrayList<>(stepInputs);
        orderedSteps.sort(Comparator.comparing(step -> step.getStepNo() == null ? Integer.MAX_VALUE : step.getStepNo()));
        int index = 1;
        for (AdminLearningItemSaveRequest.StepInput stepInput : orderedSteps) {
            if (stepInput == null || !StringUtils.hasText(stepInput.getTitle())) {
                continue;
            }
            LearningStep step = LearningStep.builder()
                    .itemPk(itemId)
                    .stepNo(stepInput.getStepNo() == null || stepInput.getStepNo() <= 0 ? index : stepInput.getStepNo())
                    .title(stepInput.getTitle().trim())
                    .description(trimToNull(stepInput.getDescription()))
                    .tip(trimToNull(stepInput.getTip()))
                    .code(trimToNull(stepInput.getCode()))
                    .build();
            learningStepMapper.insert(step);
            index++;
        }
    }

    private String writeObjectives(List<String> objectives) {
        if (objectives == null) {
            return null;
        }
        List<String> cleaned = objectives.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toList());
        try {
            return cleaned.isEmpty() ? null : objectMapper.writeValueAsString(cleaned);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to serialize objectives");
        }
    }

    private List<String> readObjectives(String rawObjectives) {
        if (!StringUtils.hasText(rawObjectives)) {
            return Collections.emptyList();
        }
        try {
            if (rawObjectives.trim().startsWith("[")) {
                return objectMapper.readValue(rawObjectives, new TypeReference<List<String>>() {});
            }
            if (rawObjectives.trim().startsWith("\"")) {
                return Collections.singletonList(objectMapper.readValue(rawObjectives, String.class));
            }
        } catch (Exception ignored) {
            // fall through
        }
        return Collections.singletonList(rawObjectives);
    }

    private String writeFeatures(List<LearningFeature> features) {
        if (features == null) {
            return null;
        }
        List<LearningFeature> cleaned = features.stream()
                .filter(Objects::nonNull)
                .filter(feature -> StringUtils.hasText(feature.getTitle()) || StringUtils.hasText(feature.getDescription()))
                .map(feature -> LearningFeature.builder()
                        .title(trimToNull(feature.getTitle()))
                        .description(trimToNull(feature.getDescription()))
                        .build())
                .collect(Collectors.toList());
        try {
            return cleaned.isEmpty() ? null : objectMapper.writeValueAsString(cleaned);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to serialize features");
        }
    }

    private List<LearningFeature> readFeatures(String rawFeatures) {
        if (!StringUtils.hasText(rawFeatures)) {
            return Collections.emptyList();
        }
        try {
            String json = rawFeatures;
            if (rawFeatures.trim().startsWith("\"")) {
                json = objectMapper.readValue(rawFeatures, String.class);
            }
            return objectMapper.readValue(json, new TypeReference<List<LearningFeature>>() {});
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (STATUS_DRAFT.equals(normalized) || STATUS_PUBLISHED.equals(normalized) || STATUS_ARCHIVED.equals(normalized)) {
            return normalized;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid learning item status");
    }

    private String normalizeExistingStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return STATUS_PUBLISHED;
        }
        return status.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private long generateJsonId() {
        long timestampPart = System.currentTimeMillis();
        int randomPart = ThreadLocalRandom.current().nextInt(100, 1000);
        return Long.parseLong(String.valueOf(timestampPart).substring(4) + randomPart);
    }
}
