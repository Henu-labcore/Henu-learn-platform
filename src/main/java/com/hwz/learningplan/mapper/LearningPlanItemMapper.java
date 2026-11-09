package com.hwz.learningplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hwz.learningplan.dto.LearningPlanDtos.ItemViewRow;
import com.hwz.learningplan.entity.LearningPlanItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LearningPlanItemMapper extends BaseMapper<LearningPlanItem> {
    /** 只读原学习状态：新计划至少含一个待完成条目，避免保存后立即归档。 */
    @Select({
            "<script>",
            "SELECT COUNT(*) FROM learning_item li",
            "LEFT JOIN learning_record lr ON lr.item_pk=li.item_pk AND lr.user_id=#{userId}",
            "WHERE lr.complete_time IS NULL AND li.item_pk IN",
            "<foreach collection='itemIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int countUncompletedItems(@Param("userId") Long userId, @Param("itemIds") List<Long> itemIds);

    @Select({
            "SELECT lpi.plan_item_id AS planItemId, lpi.item_pk AS itemPk,",
            "COALESCE(li.title, '内容已删除') AS title, li.category, li.difficulty, li.duration,",
            "COALESCE(li.status = 'PUBLISHED', 0) AS available,",
            "lpi.target_date AS targetDate, lpi.sort_order AS sortOrder,",
            "lr.complete_time AS completeTime, COALESCE(lr.learn_duration_sec, 0) AS learnDurationSec,",
            "lr.updated_at AS lastActivityTime",
            "FROM learning_plan_item lpi",
            // 不限制原学习内容删除；内容失效后保留计划历史条目。
            "LEFT JOIN learning_item li ON li.item_pk = lpi.item_pk",
            "LEFT JOIN learning_record lr ON lr.item_pk = lpi.item_pk AND lr.user_id = #{userId}",
            "WHERE lpi.plan_id = #{planId}",
            "ORDER BY lpi.sort_order ASC, lpi.plan_item_id ASC"
    })
    List<ItemViewRow> selectViewRows(@Param("planId") Long planId, @Param("userId") Long userId);
}
//计划表数据访问
