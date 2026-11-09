package com.hwz.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 助学会话。每名学生可在同一学习内容和场景下建立多条相互独立的会话。
 */
@Data
@TableName("ai_conversation")
public class AiConversation {
    @TableId(value = "conversation_id", type = IdType.AUTO)
    private Long conversationId;
    private Long userId;
    private Long itemPk;
    private String scene;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
