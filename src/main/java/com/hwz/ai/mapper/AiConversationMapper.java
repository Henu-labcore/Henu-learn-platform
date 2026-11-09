package com.hwz.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hwz.ai.entity.AiConversation;
import org.apache.ibatis.annotations.Mapper;

/** AI 会话数据访问模块。 */
@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {
}

