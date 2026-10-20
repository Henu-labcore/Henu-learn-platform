package com.hwz.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hwz.common.entity.AssignmentKnowledgePoint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考核与知识点关联 Mapper。
 */
@Mapper
public interface AssignmentKnowledgePointMapper
        extends BaseMapper<AssignmentKnowledgePoint> {
}