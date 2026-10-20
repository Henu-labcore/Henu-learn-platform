package com.hwz.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hwz.common.entity.StudentKnowledgeMastery;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生知识点掌握度 Mapper。
 */
@Mapper
public interface StudentKnowledgeMasteryMapper
        extends BaseMapper<StudentKnowledgeMastery> {
}