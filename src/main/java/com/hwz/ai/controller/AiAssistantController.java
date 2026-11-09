package com.hwz.ai.controller;

import com.hwz.ai.dto.AiAssistantDtos.ChatRequest;
import com.hwz.ai.dto.AiAssistantDtos.ChatResponse;
import com.hwz.ai.dto.AiAssistantDtos.FeedbackRequest;
import com.hwz.ai.dto.AiAssistantDtos.HistoryResponse;
import com.hwz.ai.dto.AiAssistantDtos.ConversationCreateRequest;
import com.hwz.ai.dto.AiAssistantDtos.ConversationRenameRequest;
import com.hwz.ai.dto.AiAssistantDtos.ConversationResponse;
import com.hwz.ai.service.AiAssistantService;
import com.hwz.common.Result;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/** AI 助学对话接口，仅负责参数接收，业务规则集中在服务层。 */
@RestController
@RequestMapping("/ai-assistant")
public class AiAssistantController {
    private final AiAssistantService assistantService;

    public AiAssistantController(AiAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        return Result.ok(assistantService.chat(request));
    }

    /**
     * 以SSE逐段返回DeepSeek内容。no-transform和X-Accel-Buffering响应头用于关闭中间层缓冲，
     * 避免后端已经分段发送，但浏览器直到整段回答结束后才一次性收到。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request, HttpServletResponse response) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        return assistantService.streamChat(request);
    }

    @GetMapping("/history")
    public Result<HistoryResponse> history(@RequestParam Long itemPk,
                                           @RequestParam String scene,
                                           @RequestParam(required = false) Long conversationId) {
        return Result.ok(assistantService.history(itemPk, scene, conversationId));
    }

    @GetMapping("/conversations")
    public Result<java.util.List<ConversationResponse>> conversations(@RequestParam Long itemPk,
                                                                       @RequestParam String scene) {
        return Result.ok(assistantService.listConversations(itemPk, scene));
    }

    @PostMapping("/conversations")
    public Result<ConversationResponse> createConversation(@RequestBody ConversationCreateRequest request) {
        return Result.ok(assistantService.createConversation(request));
    }

    @PutMapping("/conversations/{conversationId}")
    public Result<ConversationResponse> renameConversation(@PathVariable Long conversationId,
                                                            @RequestBody ConversationRenameRequest request) {
        return Result.ok(assistantService.renameConversation(conversationId, request));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable Long conversationId) {
        assistantService.deleteConversation(conversationId);
        return Result.ok();
    }

    /** 保存单条 AI 回答的有用性反馈。 */
    @PutMapping("/messages/{messageId}/feedback")
    public Result<Void> updateFeedback(@PathVariable Long messageId, @RequestBody FeedbackRequest request) {
        assistantService.updateFeedback(messageId, request);
        return Result.ok();
    }
}
