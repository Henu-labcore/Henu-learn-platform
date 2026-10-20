package com.hwz.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hwz.common.entity.LearningItemKnowledgePoint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习资源与知识点关联 Mapper。
 */
@Mapper
public interface LearningItemKnowledgePointMapper
        extends BaseMapper<LearningItemKnowledgePoint> {
}