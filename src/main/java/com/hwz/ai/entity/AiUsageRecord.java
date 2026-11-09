package com.hwz.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 每次 AI 请求的用量与结果，用于限额评估、故障定位和管理端统计。 */
@Data
@TableName("ai_usage_record")
public class AiUsageRecord {
    @TableId(value = "usage_id", type = IdType.AUTO)
    private Long usageId;
    private Long userId;
    private Long conversationId;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Boolean success;
    private String errorMessage;
    private Long latencyMs;
    private LocalDateTime createdAt;
}
