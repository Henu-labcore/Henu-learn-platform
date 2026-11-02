package com.hwz.user.mapper;

import com.hwz.user.dto.CategoryProgressDTO;
import com.hwz.user.dto.DifficultyProgressDTO;
import com.hwz.user.dto.RecentItemDTO;
import com.hwz.user.dto.RecommendationDTO;
import com.hwz.user.dto.SummaryDTO;
import com.hwz.user.dto.TrendPointDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LearnAnalyticsMapper {
    @Select({
            "SELECT",
            "  COUNT(*) AS totalCount,",
            "  COALESCE(SUM(lr.complete_time IS NOT NULL), 0) AS completedCount,",
            "  COALESCE(SUM(lr.complete_time IS NULL AND (lr.learn_duration_sec > 0 OR lr.first_learn_time IS NOT NULL)), 0) AS inProgressCount,",
            "  COALESCE(SUM(COALESCE(lr.learn_duration_sec, 0)), 0) AS learnDurationSec,",
            "  MAX(lr.updated_at) AS lastActiveTime,",
            "  (SELECT COUNT(*) FROM learning_item WHERE status = 'PUBLISHED') AS totalCourseCount,",
            "  IFNULL(",
            "    SUM(lr.complete_time IS NOT NULL) / NULLIF((SELECT COUNT(*) FROM learning_item WHERE status = 'PUBLISHED'), 0),",
            "    0",
            "  ) AS completionRate",
            "FROM learning_record lr",
            "WHERE lr.user_id = #{userId}"
    })
    SummaryDTO selectSummary(@Param("userId") Long userId);

    @Select({
            "SELECT",
            "  COALESCE(NULLIF(li.category, ''), 'Uncategorized') AS category,",
            "  COUNT(li.item_pk) AS totalCount,",
            "  COALESCE(SUM(lr.complete_time IS NOT NULL), 0) AS completedCount,",
            "  COALESCE(SUM(COALESCE(lr.learn_duration_sec, 0)), 0) AS learnDurationSec",
            "FROM learning_item li",
            "LEFT JOIN learning_record lr",
            "  ON lr.item_pk = li.item_pk AND lr.user_id = #{userId}",
            "WHERE li.status = 'PUBLISHED'",
            "GROUP BY COALESCE(NULLIF(li.category, ''), 'Uncategorized')",
            "ORDER BY totalCount DESC, learnDurationSec DESC"
    })
    List<CategoryProgressDTO> selectByCategory(@Param("userId") Long userId);

    @Select({
            "SELECT",
            "  COALESCE(NULLIF(li.difficulty, ''), 'Unknown') AS difficulty,",
            "  COUNT(li.item_pk) AS totalCount,",
            "  COALESCE(SUM(lr.complete_time IS NOT NULL), 0) AS completedCount,",
            "  COALESCE(SUM(COALESCE(lr.learn_duration_sec, 0)), 0) AS learnDurationSec",
            "FROM learning_item li",
            "LEFT JOIN learning_record lr",
            "  ON lr.item_pk = li.item_pk AND lr.user_id = #{userId}",
            "WHERE li.status = 'PUBLISHED'",
            "GROUP BY COALESCE(NULLIF(li.difficulty, ''), 'Unknown')"
    })
    List<DifficultyProgressDTO> selectByDifficulty(@Param("userId") Long userId);

    @Select({
            "SELECT",
            "  DATE(lr.complete_time) AS date,",
            "  COUNT(*) AS completedCount",
            "FROM learning_record lr",
            "WHERE lr.user_id = #{userId}",
            "  AND lr.complete_time IS NOT NULL",
            "  AND lr.complete_time >= #{startDate}",
            "GROUP BY DATE(lr.complete_time)",
            "ORDER BY date"
    })
    List<TrendPointDTO> selectCompletionTrend(@Param("userId") Long userId,
                                             @Param("startDate") java.time.LocalDate startDate);

    @Select({
            "SELECT",
            "  li.item_pk AS itemPk,",
            "  li.title AS title,",
            "  COALESCE(NULLIF(li.category, ''), 'Uncategorized') AS category,",
            "  COALESCE(NULLIF(li.difficulty, ''), 'Unknown') AS difficulty,",
            "  lr.complete_time AS completeTime,",
            "  lr.complete_remark AS completeRemark,",
            "  lr.learn_duration_sec AS learnDurationSec,",
            "  lr.updated_at AS updatedAt",
            "FROM learning_record lr",
            "JOIN learning_item li ON li.item_pk = lr.item_pk",
            "WHERE lr.user_id = #{userId}",
            "ORDER BY lr.updated_at DESC",
            "LIMIT #{limit}"
    })
    List<RecentItemDTO> selectRecent(@Param("userId") Long userId,
                                     @Param("limit") Integer limit);

    @Select({
            "SELECT",
            "  li.item_pk AS itemPk,",
            "  li.title AS title,",
            "  COALESCE(NULLIF(li.category, ''), 'Uncategorized') AS category,",
            "  COALESCE(NULLIF(li.difficulty, ''), 'Unknown') AS difficulty,",
            "  li.duration AS duration,",
            "  lr.learn_duration_sec AS learnDurationSec,",
            "  lr.updated_at AS lastLearnTime,",
            "  NULL AS expectedDurationSec",
            "FROM learning_record lr",
            "JOIN learning_item li ON li.item_pk = lr.item_pk",
            "WHERE lr.user_id = #{userId}",
            "  AND lr.first_learn_time IS NOT NULL",
            "  AND lr.complete_time IS NULL",
            "ORDER BY lr.updated_at ASC",
            "LIMIT 1"
    })
    RecommendationDTO selectRecommendationInProgress(@Param("userId") Long userId);

    @Select({
            "SELECT",
            "  li.item_pk AS itemPk,",
            "  li.title AS title,",
            "  COALESCE(NULLIF(li.category, ''), 'Uncategorized') AS category,",
            "  COALESCE(NULLIF(li.difficulty, ''), 'Unknown') AS difficulty,",
            "  li.duration AS duration,",
            "  lr.learn_duration_sec AS learnDurationSec,",
            "  lr.updated_at AS lastLearnTime,",
            "  NULL AS expectedDurationSec",
            "FROM learning_record lr",
            "JOIN learning_item li ON li.item_pk = lr.item_pk",
            "WHERE lr.user_id = #{userId}",
            "ORDER BY lr.updated_at DESC",
            "LIMIT 1"
    })
    RecommendationDTO selectRecommendationRecent(@Param("userId") Long userId);
}
