package com.hwz.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** AI 助学消息，保存用户问题和助手回答，便于恢复学习上下文。 */
@Data
@TableName("ai_message")
public class AiMessage {
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;
    private Long conversationId;
    private String role;
    private String content;
    private String sourceLabel;
    /** 学生对助手回答的评价：HELPFUL / NOT_HELPFUL；空值表示未评价。 */
    private String feedback;
    private LocalDateTime createdAt;
}

