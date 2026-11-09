package com.hwz.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwz.ai.dto.AiAssistantDtos.ChatRequest;
import com.hwz.ai.dto.AiAssistantDtos.ChatResponse;
import com.hwz.ai.dto.AiAssistantDtos.HistoryResponse;
import com.hwz.ai.dto.AiAssistantDtos.MessageResponse;
import com.hwz.ai.dto.AiAssistantDtos.FeedbackRequest;
import com.hwz.ai.dto.AiAssistantDtos.ConversationCreateRequest;
import com.hwz.ai.dto.AiAssistantDtos.ConversationRenameRequest;
import com.hwz.ai.dto.AiAssistantDtos.ConversationResponse;
import com.hwz.ai.entity.AiConversation;
import com.hwz.ai.entity.AiMessage;
import com.hwz.ai.entity.AiUsageRecord;
import com.hwz.ai.mapper.AiConversationMapper;
import com.hwz.ai.mapper.AiMessageMapper;
import com.hwz.ai.mapper.AiUsageRecordMapper;
import com.hwz.common.context.BaseContext;
import com.hwz.user.entity.LearningItem;
import com.hwz.user.entity.LearningStep;
import com.hwz.user.mapper.LearningItemMapper;
import com.hwz.user.mapper.LearningStepMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PreDestroy;

/**
 * AI 助学核心服务：组装当前课程上下文、维护会话历史，并调用兼容 Chat Completions 的模型接口。
 * 模型密钥未配置时返回明确标识的演示回答，保证本地页面仍可完整展示且不会伪装成真实模型结果。
 */
@Service
public class AiAssistantService {
    private static final Set<String> SCENES = new HashSet<>(Arrays.asList("LEARNING", "EXPERIMENT"));
    private static final Set<String> FEEDBACK_VALUES = new HashSet<>(Arrays.asList("HELPFUL", "NOT_HELPFUL", "NONE"));
    private static final int MAX_CONVERSATIONS_PER_ITEM = 20;

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final LearningItemMapper learningItemMapper;
    private final LearningStepMapper learningStepMapper;
    private final AiUsageRecordMapper usageRecordMapper;
    private final AiRequestGuard requestGuard;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ExecutorService streamExecutor;

    @Value("${labcore.ai.api-url:}")
    private String apiUrl;
    @Value("${labcore.ai.api-key:}")
    private String apiKey;
    @Value("${labcore.ai.model:}")
    private String model;
    @Value("${labcore.ai.max-history:12}")
    private int maxHistory;
    @Value("${labcore.ai.max-tokens:2048}")
    private int maxTokens;
    @Value("${labcore.ai.max-retries:2}")
    private int maxRetries;

    public AiAssistantService(AiConversationMapper conversationMapper,
                              AiMessageMapper messageMapper,
                              AiUsageRecordMapper usageRecordMapper,
                              AiRequestGuard requestGuard,
                              LearningItemMapper learningItemMapper,
                              LearningStepMapper learningStepMapper,
                              ObjectMapper objectMapper,
                              RestTemplateBuilder restTemplateBuilder) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.usageRecordMapper = usageRecordMapper;
        this.requestGuard = requestGuard;
        this.learningItemMapper = learningItemMapper;
        this.learningStepMapper = learningStepMapper;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(8))
                .setReadTimeout(Duration.ofSeconds(90))
                .build();
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task, "ai-stream-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        this.streamExecutor = new ThreadPoolExecutor(4, 8, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100), threadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

    @PreDestroy
    public void shutdownStreamExecutor() {
        streamExecutor.shutdownNow();
    }

    public ChatResponse chat(ChatRequest request) {
        validateChatRequest(request);
        Long userId = requireUserId();
        long startedAt = System.currentTimeMillis();
        AiRequestGuard.Permit permit = requestGuard.acquire(userId);
        ProviderResult providerResult = null;
        AiConversation conversation = null;
        try {
            String scene = normalizeScene(request.getScene());
            LearningItem item = requirePublishedItem(request.getItemPk());
            conversation = resolveConversation(userId, item, scene, request.getConversationId());
            saveMessage(conversation.getConversationId(), "USER", request.getQuestion(), null);
            String sourceLabel = buildSourceLabel(item, request.getCurrentStep());
            boolean demoMode = !isProviderConfigured();
            providerResult = demoMode
                    ? ProviderResult.demo(buildDemoAnswer(request, item))
                    : callProvider(buildSystemPrompt(item, scene, request.getCurrentStep(), request.getAdditionalContext()),
                    conversation.getConversationId());
            AiMessage assistantMessage = saveMessage(conversation.getConversationId(), "ASSISTANT", providerResult.answer, sourceLabel);
            touchConversation(conversation);
            recordUsage(userId, conversation.getConversationId(), providerResult, true, null,
                    System.currentTimeMillis() - startedAt);

            ChatResponse response = new ChatResponse();
            response.setConversationId(conversation.getConversationId());
            response.setMessageId(assistantMessage.getMessageId());
            response.setAnswer(providerResult.answer);
            response.setSourceLabel(sourceLabel);
            response.setDemoMode(demoMode);
            return response;
        } catch (RuntimeException ex) {
            recordUsage(userId, conversation == null ? null : conversation.getConversationId(), providerResult,
                    false, ex.getMessage(), System.currentTimeMillis() - startedAt);
            throw ex;
        } finally {
            permit.close();
        }
    }

    /**
     * 流式问答入口：请求线程先完成鉴权、课程校验和用户消息落库，再由后台线程读取模型 SSE。
     * 这样即使 HTTP 过滤器清理了 ThreadLocal，后台线程也不再依赖登录上下文。
     */
    public SseEmitter streamChat(ChatRequest request) {
        validateChatRequest(request);
        Long userId = requireUserId();
        long startedAt = System.currentTimeMillis();
        String scene = normalizeScene(request.getScene());
        LearningItem item = requirePublishedItem(request.getItemPk());
        AiRequestGuard.Permit permit = requestGuard.acquire(userId);
        try {
            AiConversation conversation = resolveConversation(userId, item, scene, request.getConversationId());
            saveMessage(conversation.getConversationId(), "USER", request.getQuestion(), null);
            String sourceLabel = buildSourceLabel(item, request.getCurrentStep());
            boolean demoMode = !isProviderConfigured();
            SseEmitter emitter = new SseEmitter(120_000L);
            // 使用有界线程池承接模型的阻塞式流读取，避免大量请求耗尽公共线程池。
            streamExecutor.execute(() -> {
                try {
                    runStream(request, item, scene, conversation, sourceLabel, demoMode,
                            userId, startedAt, emitter);
                } finally {
                    permit.close();
                }
            });
            return emitter;
        } catch (RejectedExecutionException ex) {
            permit.close();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 服务繁忙，请稍后重试");
        } catch (RuntimeException ex) {
            // 会话或用户消息落库失败时，后台任务尚未接管 Permit，必须在当前线程释放。
            permit.close();
            throw ex;
        }
    }

    private void runStream(ChatRequest request,
                           LearningItem item,
                           String scene,
                           AiConversation conversation,
                           String sourceLabel,
                           boolean demoMode,
                           Long userId,
                           long startedAt,
                           SseEmitter emitter) {
        StringBuilder answer = new StringBuilder();
        ProviderResult providerResult = null;
        try {
            sendEvent(emitter, "meta", streamEvent("demoMode", demoMode, "sourceLabel", sourceLabel));
            if (demoMode) {
                // 演示模式同样按短片段发送，便于无密钥环境验证前端流式渲染链路。
                String demoAnswer = buildDemoAnswer(request, item);
                streamDemoAnswer(demoAnswer, answer, emitter);
                providerResult = ProviderResult.demo(demoAnswer);
            } else {
                providerResult = streamProvider(buildSystemPrompt(item, scene, request.getCurrentStep(), request.getAdditionalContext()),
                        conversation.getConversationId(), answer, emitter);
            }
            AiMessage saved = saveMessage(conversation.getConversationId(), "ASSISTANT", answer.toString(), sourceLabel);
            touchConversation(conversation);
            recordUsage(userId, conversation.getConversationId(), providerResult, true, null,
                    System.currentTimeMillis() - startedAt);
            sendEvent(emitter, "done", streamEvent("conversationId", conversation.getConversationId(),
                    "messageId", saved.getMessageId(), "sourceLabel", sourceLabel));
            emitter.complete();
        } catch (Exception ex) {
            recordUsage(userId, conversation.getConversationId(), providerResult, false, ex.getMessage(),
                    System.currentTimeMillis() - startedAt);
            try {
                sendEvent(emitter, "failure", streamEvent("message", "AI 服务暂时不可用，请稍后重试"));
            } catch (Exception ignored) {
                // 客户端已经断开时无需再次写入。
            }
            emitter.completeWithError(ex);
        }
    }

    private ProviderResult streamProvider(String systemPrompt,
                                          Long conversationId,
                                          StringBuilder answer,
                                          SseEmitter emitter) {
        Exception lastError = null;
        for (int attempt = 0; attempt <= Math.max(0, maxRetries); attempt++) {
            try {
                return executeStreamAttempt(systemPrompt, conversationId, answer, emitter);
            } catch (Exception ex) {
                lastError = ex;
                // 已向浏览器发送过内容时重试会造成重复文本，因此只允许在首字尚未返回时重试。
                if (answer.length() > 0 || attempt >= Math.max(0, maxRetries) || !isRetryable(ex)) break;
                waitBeforeRetry(attempt);
            }
        }
        throw new IllegalStateException("DeepSeek 流式调用失败", lastError);
    }

    /** 执行一次模型 SSE 调用，并从最后的 usage 数据块中提取令牌用量。 */
    private ProviderResult executeStreamAttempt(String systemPrompt,
                                                Long conversationId,
                                                StringBuilder answer,
                                                SseEmitter emitter) {
        Map<String, Object> body = buildProviderBody(systemPrompt, conversationId);
        body.put("stream", true);
        body.put("stream_options", Collections.singletonMap("include_usage", true));
        int[] usage = new int[3];
        restTemplate.execute(apiUrl, HttpMethod.POST, request -> {
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            request.getHeaders().setBearerAuth(apiKey);
            objectMapper.writeValue(request.getBody(), body);
        }, response -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) break;
                    JsonNode root = objectMapper.readTree(data);
                    readUsage(root.path("usage"), usage);
                    String delta = root.path("choices").path(0).path("delta").path("content").asText("");
                    if (StringUtils.hasText(delta)) {
                        answer.append(delta);
                        sendEvent(emitter, "delta", streamEvent("content", delta));
                    }
                }
            }
            return null;
        });
        if (answer.length() == 0) throw new IllegalStateException("DeepSeek 未返回有效内容");
        return new ProviderResult(answer.toString(), usage[0], usage[1], usage[2]);
    }

    private void streamDemoAnswer(String content, StringBuilder answer, SseEmitter emitter) throws Exception {
        for (int start = 0; start < content.length(); start += 6) {
            String delta = content.substring(start, Math.min(content.length(), start + 6));
            answer.append(delta);
            sendEvent(emitter, "delta", streamEvent("content", delta));
            Thread.sleep(35L);
        }
    }

    /** 校验消息角色和会话归属，学生不能评价用户消息或修改他人的回答。 */
    public void updateFeedback(Long messageId, FeedbackRequest request) {
        Long userId = requireUserId();
        String feedback = request == null || request.getFeedback() == null
                ? "" : request.getFeedback().trim().toUpperCase(Locale.ROOT);
        if (!FEEDBACK_VALUES.contains(feedback)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的回答反馈");
        }
        AiMessage message = messageId == null ? null : messageMapper.selectById(messageId);
        if (message == null || !"ASSISTANT".equalsIgnoreCase(message.getRole())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "助手回答不存在");
        }
        AiConversation conversation = conversationMapper.selectById(message.getConversationId());
        if (conversation == null || !userId.equals(conversation.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "助手回答不存在");
        }
        message.setFeedback("NONE".equals(feedback) ? null : feedback);
        messageMapper.updateById(message);
    }

    public HistoryResponse history(Long itemPk, String rawScene, Long conversationId) {
        Long userId = requireUserId();
        String scene = normalizeScene(rawScene);
        AiConversation conversation = conversationId == null
                ? findLatestConversation(userId, itemPk, scene)
                : requireOwnedConversation(conversationId, userId, itemPk, scene);
        HistoryResponse response = new HistoryResponse();
        response.setDemoMode(!isProviderConfigured());
        if (conversation == null) {
            response.setMessages(Collections.emptyList());
            return response;
        }
        response.setConversationId(conversation.getConversationId());
        response.setConversationTitle(conversation.getTitle());
        response.setMessages(messageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversation.getConversationId())
                        .orderByAsc(AiMessage::getMessageId))
                .stream().map(this::toResponse).collect(Collectors.toList()));
        return response;
    }

    /** 返回当前学习项下的会话摘要，最新使用的会话排在最前。 */
    public List<ConversationResponse> listConversations(Long itemPk, String rawScene) {
        Long userId = requireUserId();
        String scene = normalizeScene(rawScene);
        return conversationMapper.selectList(new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .eq(AiConversation::getItemPk, itemPk)
                        .eq(AiConversation::getScene, scene)
                        .orderByDesc(AiConversation::getUpdatedAt)
                        .orderByDesc(AiConversation::getConversationId))
                .stream().map(this::toConversationResponse).collect(Collectors.toList());
    }

    /** 新会话与旧会话完全隔离；单个学习项最多保留 20 条，防止无限增长。 */
    public ConversationResponse createConversation(ConversationCreateRequest request) {
        if (request == null || request.getItemPk() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学习内容不能为空");
        }
        Long userId = requireUserId();
        String scene = normalizeScene(request.getScene());
        LearningItem item = requirePublishedItem(request.getItemPk());
        long count = conversationMapper.selectCount(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getItemPk, item.getItemPk())
                .eq(AiConversation::getScene, scene));
        if (count >= MAX_CONVERSATIONS_PER_ITEM) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前学习内容最多保留 20 个会话，请先删除旧会话");
        }
        AiConversation created = createConversationEntity(userId, item, scene,
                normalizeConversationTitle(request.getTitle(), "新会话 " + (count + 1)));
        return toConversationResponse(created);
    }

    public ConversationResponse renameConversation(Long conversationId, ConversationRenameRequest request) {
        Long userId = requireUserId();
        AiConversation conversation = requireOwnedConversation(conversationId, userId, null, null);
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "会话名称不能为空");
        }
        conversation.setTitle(normalizeConversationTitle(request.getTitle(), conversation.getTitle()));
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return toConversationResponse(conversation);
    }

    /** 删除会话时先删除其消息，并用事务保证不会留下孤立数据。 */
    @Transactional
    public void deleteConversation(Long conversationId) {
        Long userId = requireUserId();
        AiConversation conversation = requireOwnedConversation(conversationId, userId, null, null);
        messageMapper.delete(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversation.getConversationId()));
        conversationMapper.deleteById(conversation.getConversationId());
    }

    private String buildSystemPrompt(LearningItem item, String scene, String requestStep, String additionalContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是研究生实训平台的学习助手，只提供启发式学习支持，不参与评分。\n")
                .append("优先解释思路、概念和调试步骤，不直接代写考核答案。\n")
                .append("当前场景：").append(scene).append("\n")
                .append("学习主题：").append(item.getTitle()).append("\n")
                .append("简介：").append(safe(item.getSummary())).append("\n")
                .append("学习目标：").append(safe(item.getObjectives())).append("\n")
                .append("学习步骤：\n");
        List<LearningStep> steps = learningStepMapper.selectList(new LambdaQueryWrapper<LearningStep>()
                .eq(LearningStep::getItemPk, item.getItemPk()).orderByAsc(LearningStep::getStepNo));
        for (LearningStep step : steps) {
            prompt.append(step.getStepNo()).append(". ").append(safe(step.getTitle()))
                    .append("：").append(limit(safe(step.getDescription()), 800)).append("\n");
        }
        if (StringUtils.hasText(requestStep)) {
            prompt.append("学生当前正在浏览：").append(limit(requestStep, 300)).append("\n");
        }
        if (StringUtils.hasText(additionalContext)) {
            prompt.append("学生补充的代码或报错：\n").append(limit(additionalContext, 4000));
        }
        return limit(prompt.toString(), 10000);
    }

    private ProviderResult callProvider(String systemPrompt, Long conversationId) {
        Exception lastError = null;
        for (int attempt = 0; attempt <= Math.max(0, maxRetries); attempt++) {
            try {
                Map<String, Object> body = buildProviderBody(systemPrompt, conversationId);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);
                ResponseEntity<String> response = restTemplate.exchange(
                        apiUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").path(0).path("message").path("content").asText();
                if (!StringUtils.hasText(content)) throw new IllegalStateException("模型未返回有效内容");
                int[] usage = new int[3];
                readUsage(root.path("usage"), usage);
                return new ProviderResult(content, usage[0], usage[1], usage[2]);
            } catch (Exception ex) {
                lastError = ex;
                if (attempt >= Math.max(0, maxRetries) || !isRetryable(ex)) break;
                waitBeforeRetry(attempt);
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 服务暂时不可用，请稍后重试", lastError);
    }

    /** 普通响应和流式响应共用同一请求体，避免两种模式的上下文不一致。 */
    private Map<String, Object> buildProviderBody(String systemPrompt, Long conversationId) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt));
        List<AiMessage> history = messageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .orderByDesc(AiMessage::getMessageId)
                .last("LIMIT " + Math.max(2, maxHistory)));
        Collections.reverse(history);
        for (AiMessage entry : history) {
            messages.add(message(entry.getRole().toLowerCase(Locale.ROOT), entry.getContent()));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("max_tokens", Math.max(256, maxTokens));
        // DeepSeek V4 默认开启思考；学习问答优先即时反馈，因此显式使用非思考模式。
        if (model != null && model.startsWith("deepseek-v4")) {
            body.put("thinking", Collections.singletonMap("type", "disabled"));
        }
        return body;
    }

    private String buildDemoAnswer(ChatRequest request, LearningItem item) {
        String question = request.getQuestion();
        String context = StringUtils.hasText(request.getCurrentStep())
                ? "当前识别到你正在学习“" + request.getCurrentStep() + "”。"
                : "当前问题已关联到《" + item.getTitle() + "》。";
        if (question.contains("报错") || question.contains("错误")) {
            return "【演示模式】" + context + "请先定位报错的最后一行，并依次检查变量是否已定义、依赖是否已导入、代码单元是否按顺序运行。你可以把完整报错和相关代码粘贴进来，我会按“现象—原因—验证—修改”的顺序帮助排查。";
        }
        if (question.contains("提示") || question.contains("思路")) {
            return "【演示模式】" + context + "先不要直接写最终实现。请明确输入、期望输出和约束，再把任务拆成两个最小步骤逐一验证。";
        }
        return "【演示模式】" + context + "建议先梳理该知识点的核心概念，再用一个最小示例验证理解。配置模型 API 后，这里会返回结合完整课程上下文生成的个性化回答。";
    }

    private void validateChatRequest(ChatRequest request) {
        if (request == null || request.getItemPk() == null || !StringUtils.hasText(request.getQuestion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学习内容和问题不能为空");
        }
    }

    private String buildSourceLabel(LearningItem item, String currentStep) {
        String sourceLabel = "当前学习卡片：《" + item.getTitle() + "》";
        if (StringUtils.hasText(currentStep)) sourceLabel += " · " + limit(currentStep.trim(), 120);
        return sourceLabel;
    }

    private Map<String, Object> streamEvent(Object... entries) {
        Map<String, Object> value = new LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) value.put(String.valueOf(entries[i]), entries[i + 1]);
        return value;
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(name).data(data));
    }

    private AiConversation resolveConversation(Long userId, LearningItem item, String scene, Long conversationId) {
        if (conversationId != null) {
            return requireOwnedConversation(conversationId, userId, item.getItemPk(), scene);
        }
        AiConversation existing = findLatestConversation(userId, item.getItemPk(), scene);
        if (existing != null) return existing;
        return createConversationEntity(userId, item, scene, item.getTitle());
    }

    private AiConversation createConversationEntity(Long userId, LearningItem item, String scene, String title) {
        AiConversation created = new AiConversation();
        created.setUserId(userId);
        created.setItemPk(item.getItemPk());
        created.setScene(scene);
        created.setTitle(title);
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        conversationMapper.insert(created);
        return created;
    }

    private AiConversation findLatestConversation(Long userId, Long itemPk, String scene) {
        return conversationMapper.selectOne(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getItemPk, itemPk)
                .eq(AiConversation::getScene, scene)
                .orderByDesc(AiConversation::getUpdatedAt)
                .orderByDesc(AiConversation::getConversationId)
                .last("LIMIT 1"));
    }

    /** 所有按会话主键访问的操作都通过本方法进行用户、学习项和场景校验。 */
    private AiConversation requireOwnedConversation(Long conversationId, Long userId, Long itemPk, String scene) {
        AiConversation conversation = conversationId == null ? null : conversationMapper.selectById(conversationId);
        boolean mismatch = conversation == null || !userId.equals(conversation.getUserId())
                || (itemPk != null && !itemPk.equals(conversation.getItemPk()))
                || (scene != null && !scene.equals(conversation.getScene()));
        if (mismatch) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在");
        return conversation;
    }

    private ConversationResponse toConversationResponse(AiConversation conversation) {
        ConversationResponse response = new ConversationResponse();
        response.setConversationId(conversation.getConversationId());
        response.setItemPk(conversation.getItemPk());
        response.setScene(conversation.getScene());
        response.setTitle(conversation.getTitle());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());
        response.setMessageCount(messageMapper.selectCount(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversation.getConversationId())));
        return response;
    }

    private String normalizeConversationTitle(String value, String fallback) {
        String title = StringUtils.hasText(value) ? value.trim() : fallback;
        return limit(title, 60);
    }

    private AiMessage saveMessage(Long conversationId, String role, String content, String sourceLabel) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(limit(content, 16000));
        message.setSourceLabel(sourceLabel);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    private void touchConversation(AiConversation conversation) {
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
    }

    private LearningItem requirePublishedItem(Long itemPk) {
        LearningItem item = itemPk == null ? null : learningItemMapper.selectById(itemPk);
        if (item == null || !"PUBLISHED".equalsIgnoreCase(item.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学习内容不存在");
        }
        return item;
    }

    private Long requireUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        return userId;
    }

    private String normalizeScene(String rawScene) {
        String scene = rawScene == null ? "" : rawScene.trim().toUpperCase(Locale.ROOT);
        if (!SCENES.contains(scene)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的助学场景");
        return scene;
    }

    private boolean isProviderConfigured() {
        return StringUtils.hasText(apiUrl) && StringUtils.hasText(apiKey) && StringUtils.hasText(model);
    }

    private MessageResponse toResponse(AiMessage message) {
        MessageResponse response = new MessageResponse();
        response.setMessageId(message.getMessageId());
        response.setRole(message.getRole());
        response.setContent(message.getContent());
        response.setSourceLabel(message.getSourceLabel());
        response.setFeedback(message.getFeedback());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

    /** 从兼容 Chat Completions 的 usage 节点读取输入、输出和总令牌数。 */
    private void readUsage(JsonNode usageNode, int[] usage) {
        if (usageNode == null || usageNode.isMissingNode() || usageNode.isNull()) return;
        usage[0] = usageNode.path("prompt_tokens").asInt(usage[0]);
        usage[1] = usageNode.path("completion_tokens").asInt(usage[1]);
        usage[2] = usageNode.path("total_tokens").asInt(usage[0] + usage[1]);
    }

    /** 仅对网络错误、限流和服务端故障重试，普通参数错误不会重复请求模型。 */
    private boolean isRetryable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ResourceAccessException) return true;
            if (current instanceof HttpStatusCodeException) {
                int status = ((HttpStatusCodeException) current).getRawStatusCode();
                return status == 429 || status >= 500;
            }
            current = current.getCause();
        }
        return false;
    }

    /** 短退避可缓解模型接口的瞬时拥堵，同时避免一次请求长时间占用线程。 */
    private void waitBeforeRetry(int attempt) {
        try {
            Thread.sleep(Math.min(1000L, 250L * (1L << Math.min(attempt, 2))));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI 请求已取消", ex);
        }
    }

    /** 用量记录失败不能反向影响已经生成的回答，因此数据库异常只记录到应用日志。 */
    private void recordUsage(Long userId,
                             Long conversationId,
                             ProviderResult result,
                             boolean success,
                             String errorMessage,
                             long latencyMs) {
        try {
            AiUsageRecord record = new AiUsageRecord();
            record.setUserId(userId);
            record.setConversationId(conversationId);
            record.setModel(isProviderConfigured() ? limit(model, 100) : "DEMO");
            record.setPromptTokens(result == null ? 0 : result.promptTokens);
            record.setCompletionTokens(result == null ? 0 : result.completionTokens);
            record.setTotalTokens(result == null ? 0 : result.totalTokens);
            record.setSuccess(success);
            record.setErrorMessage(StringUtils.hasText(errorMessage) ? limit(errorMessage, 500) : null);
            record.setLatencyMs(Math.max(0L, latencyMs));
            record.setCreatedAt(LocalDateTime.now());
            usageRecordMapper.insert(record);
        } catch (RuntimeException ignored) {
            // 统计属于旁路能力，不能因为统计表暂时不可用而覆盖真实问答结果。
        }
    }

    /** 模型结果对象同时携带正文和令牌统计，供普通响应与流式响应共用。 */
    private static final class ProviderResult {
        private final String answer;
        private final int promptTokens;
        private final int completionTokens;
        private final int totalTokens;

        private ProviderResult(String answer, int promptTokens, int completionTokens, int totalTokens) {
            this.answer = answer;
            this.promptTokens = Math.max(0, promptTokens);
            this.completionTokens = Math.max(0, completionTokens);
            this.totalTokens = Math.max(0, totalTokens);
        }

        private static ProviderResult demo(String answer) {
            return new ProviderResult(answer, 0, 0, 0);
        }
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> value = new LinkedHashMap<>();
        value.put("role", role);
        value.put("content", content);
        return value;
    }

    private String safe(String value) { return value == null ? "" : value; }
    private String limit(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
}
