package com.hwz.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hwz.user.dto.AdminLearningSummaryResponse;
import com.hwz.user.dto.LearningProgressResponse;
import com.hwz.user.dto.LearningProgressTimeResponse;
import com.hwz.user.dto.UserProgressDetailResponse;
import com.hwz.user.entity.LearningItem;
import com.hwz.user.entity.LearningProgress;
import com.hwz.user.mapper.LearningItemMapper;
import com.hwz.user.mapper.LearningProgressMapper;
import com.hwz.user.service.LearningProgressService;
import com.hwz.user.util.DurationTextParser;
import com.hwz.common.mapper.UserMapper;
import com.hwz.common.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Service
public class LearningProgressServiceImpl implements LearningProgressService {

    private static final long MAX_DELTA_SEC = 1800L;
    private static final long MIN_COMPLETE_SEC = 60L;

    private final LearningProgressMapper learningProgressMapper;
    private final LearningItemMapper learningItemMapper;
    private final UserMapper userMapper;

    public LearningProgressServiceImpl(LearningProgressMapper learningProgressMapper,
                                       LearningItemMapper learningItemMapper,
                                       UserMapper userMapper) {
        this.learningProgressMapper = learningProgressMapper;
        this.learningItemMapper = learningItemMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<LearningProgressResponse> listByUserAndItem(Long userId, Long itemPk) {
        return learningProgressMapper.selectList(
                        new LambdaQueryWrapper<LearningProgress>()
                                .eq(LearningProgress::getUserId, userId)
                                .eq(LearningProgress::getItemPk, itemPk)
                                .orderByDesc(LearningProgress::getUpdatedAt)
                ).stream()
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LearningProgressResponse> listByUser(Long userId) {
        return learningProgressMapper.selectList(
                        new LambdaQueryWrapper<LearningProgress>()
                                .eq(LearningProgress::getUserId, userId)
                                .orderByDesc(LearningProgress::getUpdatedAt)
                ).stream()
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LearningProgressResponse createFirstEntry(Long userId, Long itemPk) {
        LearningItem item = learningItemMapper.selectById(itemPk);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning item not found");
        }

        LearningProgress existing = learningProgressMapper.selectOne(
                new LambdaQueryWrapper<LearningProgress>()
                        .eq(LearningProgress::getUserId, userId)
                        .eq(LearningProgress::getItemPk, itemPk)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            existing.setUpdatedAt(LocalDateTime.now());
            learningProgressMapper.updateById(existing);
            return toResponse(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        LearningProgress progress = LearningProgress.builder()
                .userId(userId)
                .itemPk(itemPk)
                .firstLearnTime(now)
                .createdAt(now)
                .updatedAt(now)
                .learnDurationSec(0L)
                .build();
        learningProgressMapper.insert(progress);
        return toResponse(progress);
    }

    @Override
    public LearningProgressResponse completeLearning(Long userId, Long itemPk, String completeRemark) {
        LearningItem item = learningItemMapper.selectById(itemPk);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning item not found");
        }

        LearningProgress existing = learningProgressMapper.selectOne(
                new LambdaQueryWrapper<LearningProgress>()
                        .eq(LearningProgress::getUserId, userId)
                        .eq(LearningProgress::getItemPk, itemPk)
                        .last("LIMIT 1")
        );

        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Learning has not started");
        }

        long learnedSec = existing.getLearnDurationSec() == null ? 0L : existing.getLearnDurationSec();
        long requiredSec = resolveRequiredCompleteSec(item);
        if (learnedSec < requiredSec) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient learning duration");
        }

        LocalDateTime now = LocalDateTime.now();
        existing.setCompleteTime(now);
        if (completeRemark != null) {
            existing.setCompleteRemark(completeRemark);
        }
        existing.setUpdatedAt(now);
        learningProgressMapper.updateById(existing);
        return toResponse(existing);
    }

    @Override
    public LearningProgressTimeResponse updateLearnDuration(Long userId, Long itemPk, Long deltaSec) {
        if (deltaSec == null || deltaSec < 0 || deltaSec > MAX_DELTA_SEC) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid deltaSec");
        }

        LearningItem item = learningItemMapper.selectById(itemPk);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning item not found");
        }

        int updated = learningProgressMapper.addLearnDuration(userId, itemPk, deltaSec);
        if (updated <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Learning has not started");
        }
        Long total = learningProgressMapper.selectLearnDurationSec(userId, itemPk);
        if (total == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Learning record not found");
        }

        return LearningProgressTimeResponse.builder()
                .itemPk(itemPk)
                .learnDurationSec(total)
                .build();
    }

    @Override
    public AdminLearningSummaryResponse getAdminSummary() {
        // 获取所有用户
        List<User> allUsers = userMapper.selectList(null);
        int totalUsers = allUsers.size();

        // 获取所有学习进度记录
        List<LearningProgress> allProgress = learningProgressMapper.selectList(null);

        // 统计数据
        Map<Long, AdminLearningSummaryResponse.UserProgressSummary> userStats = new HashMap<>();
        long totalLearningItems = 0;
        long completedItems = 0;
        long totalLearningSeconds = 0;
        int activeUsers = 0;

        // 获取所有学习项数量
        List<LearningItem> allItems = learningItemMapper.selectList(null);
        totalLearningItems = allItems.size();

        // 按用户分组统计
        for (LearningProgress progress : allProgress) {
            Long userId = progress.getUserId();
            AdminLearningSummaryResponse.UserProgressSummary summary = userStats.computeIfAbsent(userId, k ->
                new AdminLearningSummaryResponse.UserProgressSummary());

            summary.setUserId(userId);
            summary.setTotalItems(totalLearningItems);

            if (progress.getCompleteTime() != null) {
                summary.setCompletedItems(summary.getCompletedItems() + 1);
                completedItems++;
            }

            long durationSec = progress.getLearnDurationSec() == null ? 0L : progress.getLearnDurationSec();
            summary.setLearningSeconds(summary.getLearningSeconds() + durationSec);
            totalLearningSeconds += durationSec;

            // 设置最后活跃时间
            if (progress.getUpdatedAt() != null) {
                String lastActive = progress.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                if (summary.getLastActiveTime() == null ||
                    lastActive.compareTo(summary.getLastActiveTime()) > 0) {
                    summary.setLastActiveTime(lastActive);
                }
            }
        }

        // 为每个用户设置基本信息
        for (User user : allUsers) {
            AdminLearningSummaryResponse.UserProgressSummary summary = userStats.computeIfAbsent(user.getId(), k ->
                new AdminLearningSummaryResponse.UserProgressSummary());

            summary.setUserId(user.getId());
            summary.setUsername(user.getUsername());
            summary.setDisplayName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
            summary.setTotalItems(totalLearningItems);

            if (summary.getLearningSeconds() > 0) {
                activeUsers++;
            }
        }

        List<AdminLearningSummaryResponse.UserProgressSummary> userSummaries =
            userStats.values().stream().collect(Collectors.toList());

        return new AdminLearningSummaryResponse(
            totalUsers, activeUsers, totalLearningItems, completedItems,
            totalLearningSeconds, userSummaries);
    }

    private LearningProgressResponse toResponse(LearningProgress progress) {
        return LearningProgressResponse.builder()
                .id(progress.getId())
                .userId(progress.getUserId())
                .itemPk(progress.getItemPk())
                .firstLearnTime(progress.getFirstLearnTime())
                .completeTime(progress.getCompleteTime())
                .completeRemark(progress.getCompleteRemark())
                .learnDurationSec(progress.getLearnDurationSec())
                .createdAt(progress.getCreatedAt())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }

    @Override
    public UserProgressDetailResponse getUserProgressDetail(Long userId) {
        // 获取用户的学习进度记录
        List<LearningProgress> userProgressList = learningProgressMapper.selectList(
            new QueryWrapper<LearningProgress>().eq("user_id", userId)
        );

        // 获取所有学习项信息
        List<LearningItem> allItems = learningItemMapper.selectList(null);
        Map<Long, LearningItem> itemMap = allItems.stream()
            .collect(Collectors.toMap(LearningItem::getItemPk, item -> item));

        // 统计数据
        int totalItems = allItems.size();
        int completedItems = 0;
        int inProgressItems = 0;
        long totalLearningTime = 0;

        List<UserProgressDetailResponse.LearningItemProgress> learningItems = new ArrayList<>();
        List<UserProgressDetailResponse.RecentActivity> recentActivities = new ArrayList<>();

        for (LearningProgress progress : userProgressList) {
            LearningItem item = itemMap.get(progress.getItemPk());
            if (item == null) continue;

            // 计算进度
            int progressPercent = 0;
            String status = "NOT_STARTED";

            if (progress.getCompleteTime() != null) {
                progressPercent = 100;
                status = "COMPLETED";
                completedItems++;
            } else if (progress.getLearnDurationSec() != null && progress.getLearnDurationSec() > 0) {
                // 假设完成需要至少10分钟的学习时间作为基础
                long requiredTime = resolveExpectedDurationSec(item);
                progressPercent = (int) Math.min(99, (progress.getLearnDurationSec() * 100.0) / requiredTime);
                status = "IN_PROGRESS";
                inProgressItems++;
            }

            totalLearningTime += progress.getLearnDurationSec() != null ? progress.getLearnDurationSec() : 0;

            // 构建学习项目进度
            UserProgressDetailResponse.LearningItemProgress itemProgress =
                new UserProgressDetailResponse.LearningItemProgress();
            itemProgress.setItemId(item.getItemPk());
            itemProgress.setItemName(item.getTitle());
            itemProgress.setCategory(item.getCategory() != null ? item.getCategory() : "未分类");
            itemProgress.setProgress(progressPercent);
            itemProgress.setStatus(status);
            itemProgress.setLearningTime(progress.getLearnDurationSec() != null ? progress.getLearnDurationSec() : 0);
            itemProgress.setLastAccessTime(progress.getUpdatedAt() != null ? progress.getUpdatedAt().toString() : null);
            learningItems.add(itemProgress);

            // 构建最近活动记录（最近7天的活动）
            if (progress.getUpdatedAt() != null) {
                long updateTimeMillis = progress.getUpdatedAt()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
                long daysDiff = (System.currentTimeMillis() - updateTimeMillis) / (1000 * 60 * 60 * 24);
                if (daysDiff <= 7) {
                    String action = "学习中";
                    if (progress.getCompleteTime() != null) {
                        action = "完成学习";
                    } else if (progress.getFirstLearnTime() != null) {
                        action = "开始学习";
                    }

                    recentActivities.add(UserProgressDetailResponse.RecentActivity.builder()
                        .id(progress.getId())
                        .itemName(item.getTitle())
                        .action(action)
                        .duration(progress.getLearnDurationSec() != null ? progress.getLearnDurationSec() : 0)
                        .timestamp(progress.getUpdatedAt().toString())
                        .build());
                }
            }
        }

        // 按时间倒序排列最近活动
        recentActivities.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        return UserProgressDetailResponse.builder()
            .totalItems(totalItems)
            .completedItems(completedItems)
            .inProgressItems(inProgressItems)
            .totalLearningTime(totalLearningTime)
            .learningItems(learningItems)
            .recentActivities(recentActivities)
            .build();
    }

    private long resolveExpectedDurationSec(LearningItem item) {
        java.util.OptionalLong parsed = DurationTextParser.parseSeconds(item == null ? null : item.getDuration());
        if (parsed.isPresent() && parsed.getAsLong() > 0) {
            return parsed.getAsLong();
        }
        return 600L;
    }

    private long resolveRequiredCompleteSec(LearningItem item) {
        long expected = resolveExpectedDurationSec(item);
        return Math.max(MIN_COMPLETE_SEC, Math.min(expected, 600L));
    }
}
