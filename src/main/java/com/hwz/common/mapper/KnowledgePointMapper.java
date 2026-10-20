package com.hwz.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hwz.common.entity.KnowledgePoint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识点 Mapper。
 */
@Mapper
public interface KnowledgePointMapper
        extends BaseMapper<KnowledgePoint> {
}