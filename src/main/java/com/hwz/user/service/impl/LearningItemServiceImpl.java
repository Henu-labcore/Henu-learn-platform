package com.hwz.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwz.user.dto.LearningFeature;
import com.hwz.user.dto.LearningItemDetailResponse;
import com.hwz.user.dto.LearningItemListResponse;
import com.hwz.user.dto.LearningItemPreviewResponse;
import com.hwz.user.dto.LearningStepResponse;
import com.hwz.user.entity.LearningItem;
import com.hwz.user.entity.LearningStep;
import com.hwz.user.mapper.LearningItemMapper;
import com.hwz.user.mapper.LearningStepMapper;
import com.hwz.user.service.LearningItemService;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LearningItemServiceImpl implements LearningItemService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final LearningItemMapper learningItemMapper;
    private final LearningStepMapper learningStepMapper;
    private final ObjectMapper objectMapper;

    public LearningItemServiceImpl(LearningItemMapper learningItemMapper,
                                   LearningStepMapper learningStepMapper,
                                   ObjectMapper objectMapper) {
        this.learningItemMapper = learningItemMapper;
        this.learningStepMapper = learningStepMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<LearningItemListResponse> listItems(String category) {
        LambdaQueryWrapper<LearningItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningItem::getStatus, STATUS_PUBLISHED);
        if (StringUtils.hasText(category)) {
            wrapper.eq(LearningItem::getCategory, category);
        }
        wrapper.orderByDesc(LearningItem::getItemPk);

        return learningItemMapper.selectList(wrapper).stream()
                .filter(Objects::nonNull)
                .map(item -> LearningItemListResponse.builder()
                        .id(item.getItemPk())
                        .title(item.getTitle())
                        .summary(item.getSummary())
                        .category(item.getCategory())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public LearningItemPreviewResponse getPreview(Long id) {
        LearningItem item = getItemOrThrow(id);
        return LearningItemPreviewResponse.builder()
                .id(item.getItemPk())
                .title(item.getTitle())
                .summary(item.getSummary())
                .difficulty(item.getDifficulty())
                .duration(item.getDuration())
                .prerequisites(item.getPrerequisites())
                .objectives(formatObjectives(item.getObjectives()))
                .build();
    }

    @Override
    public LearningItemDetailResponse getDetail(Long id) {
        LearningItem item = getItemOrThrow(id);
        List<LearningStep> steps = learningStepMapper.selectList(
                new LambdaQueryWrapper<LearningStep>()
                        .eq(LearningStep::getItemPk, id)
                        .orderByAsc(LearningStep::getStepNo)
        );

        return LearningItemDetailResponse.builder()
                .id(item.getItemPk())
                .title(item.getTitle())
                .summary(item.getSummary())
                .category(item.getCategory())
                .difficulty(item.getDifficulty())
                .duration(item.getDuration())
                .prerequisites(item.getPrerequisites())
                .objectives(formatObjectives(item.getObjectives()))
                .features(parseFeatures(item.getFeatures()))
                .steps(steps.stream()
                        .map(step -> LearningStepResponse.builder()
                                .stepNo(step.getStepNo())
                                .title(step.getTitle())
                                .description(step.getDescription())
                                .tip(step.getTip())
                                .code(step.getCode())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private LearningItem getItemOrThrow(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid id");
        }
        LearningItem item = learningItemMapper.selectById(id);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        }
        if (!STATUS_PUBLISHED.equalsIgnoreCase(item.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        }
        return item;
    }

    private String formatObjectives(String rawObjectives) {
        if (!StringUtils.hasText(rawObjectives)) {
            return "";
        }
        String trimmed = rawObjectives.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> list = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
                return String.join("; ", list);
            } catch (Exception ignored) {
                return rawObjectives;
            }
        }
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            try {
                return objectMapper.readValue(trimmed, String.class);
            } catch (Exception ignored) {
                return rawObjectives;
            }
        }
        return rawObjectives;
    }

    private List<LearningFeature> parseFeatures(String rawFeatures) {
        if (!StringUtils.hasText(rawFeatures)) {
            return Collections.emptyList();
        }
        String trimmed = rawFeatures.trim();
        String json = trimmed;
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            try {
                json = objectMapper.readValue(trimmed, String.class);
            } catch (Exception ignored) {
                json = trimmed;
            }
        }
        if (!json.startsWith("[")) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<LearningFeature>>() {});
        } catch (Exception ex) {
            return parseFeaturesFallback(json);
        }
    }

    private List<LearningFeature> parseFeaturesFallback(String json) {
        try {
            JsonParser parser = JsonParserFactory.getJsonParser();
            List<Object> list = parser.parseList(json);
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> {
                        Map<?, ?> map = (Map<?, ?>) entry;
                        Object title = map.get("title");
                        Object description = map.get("description");
                        return LearningFeature.builder()
                                .title(title == null ? "" : String.valueOf(title))
                                .description(description == null ? "" : String.valueOf(description))
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
