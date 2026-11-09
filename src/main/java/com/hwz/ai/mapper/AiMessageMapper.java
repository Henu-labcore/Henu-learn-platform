package com.hwz.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hwz.ai.entity.AiMessage;
import org.apache.ibatis.annotations.Mapper;

/** AI 消息数据访问模块。 */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
}

