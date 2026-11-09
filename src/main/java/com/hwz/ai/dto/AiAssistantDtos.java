package com.hwz.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** AI 助学接口使用的请求与响应对象集合。 */
public final class AiAssistantDtos {
    private AiAssistantDtos() {
    }

    @Data
    public static class ChatRequest {
        /** 为空时沿用最近一次会话；多会话界面会显式传入当前会话。 */
        private Long conversationId;
        private Long itemPk;
        private String scene;
        private String question;
        /** 学习页根据滚动位置识别出的当前知识步骤，例如“第 2 步：定义本地工具函数”。 */
        private String currentStep;
        /** 学生主动粘贴的代码、报错或当前步骤，不从 JupyterLite 中强行抓取。 */
        private String additionalContext;
    }

    @Data
    public static class ChatResponse {
        private Long conversationId;
        /** 新生成的助手消息主键，前端用它提交回答质量反馈。 */
        private Long messageId;
        private String answer;
        private String sourceLabel;
        private boolean demoMode;
    }

    @Data
    public static class MessageResponse {
        private Long messageId;
        private String role;
        private String content;
        private String sourceLabel;
        private String feedback;
        private LocalDateTime createdAt;
    }

    @Data
    public static class FeedbackRequest {
        /** HELPFUL、NOT_HELPFUL 或 NONE；NONE 表示撤销反馈。 */
        private String feedback;
    }

    @Data
    public static class HistoryResponse {
        private Long conversationId;
        private String conversationTitle;
        private boolean demoMode;
        private List<MessageResponse> messages;
    }

    @Data
    public static class ConversationCreateRequest {
        private Long itemPk;
        private String scene;
        private String title;
    }

    @Data
    public static class ConversationRenameRequest {
        private String title;
    }

    /** 会话列表只返回摘要，不重复携带完整消息，避免记录面板加载过重。 */
    @Data
    public static class ConversationResponse {
        private Long conversationId;
        private Long itemPk;
        private String scene;
        private String title;
        private long messageCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
