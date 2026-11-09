package com.hwz.ai.service;

import com.hwz.ai.dto.AiAssistantDtos.ChatRequest;
import com.hwz.ai.entity.AiConversation;
import com.hwz.ai.entity.AiMessage;
import com.hwz.ai.entity.AiUsageRecord;
import com.hwz.ai.mapper.AiConversationMapper;
import com.hwz.ai.mapper.AiMessageMapper;
import com.hwz.ai.mapper.AiUsageRecordMapper;
import com.hwz.common.context.BaseContext;
import com.hwz.user.entity.LearningItem;
import com.hwz.user.mapper.LearningItemMapper;
import com.hwz.user.mapper.LearningStepMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证真实模型普通调用的异常重试、回答落库和 Token 用量记录。 */
class AiAssistantServiceTest {

    @AfterEach
    void clearUserContext() {
        BaseContext.clear();
    }

    @Test
    void shouldRetryTransientFailureAndRecordUsage() {
        AiConversationMapper conversationMapper = mock(AiConversationMapper.class);
        AiMessageMapper messageMapper = mock(AiMessageMapper.class);
        AiUsageRecordMapper usageMapper = mock(AiUsageRecordMapper.class);
        LearningItemMapper itemMapper = mock(LearningItemMapper.class);
        LearningStepMapper stepMapper = mock(LearningStepMapper.class);
        AiRequestGuard guard = new AiRequestGuard();
        ReflectionTestUtils.setField(guard, "requestsPerMinute", 10);
        ReflectionTestUtils.setField(guard, "maxConcurrentPerUser", 2);

        RestTemplate restTemplate = mock(RestTemplate.class);
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.setConnectTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.setReadTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);

        LearningItem item = LearningItem.builder().itemPk(1L).title("Tool Use")
                .summary("summary").objectives("objectives").status("PUBLISHED").build();
        AiConversation conversation = new AiConversation();
        conversation.setConversationId(20L);
        conversation.setUserId(7L);
        conversation.setItemPk(1L);
        conversation.setScene("LEARNING");
        conversation.setTitle("默认会话");
        conversation.setUpdatedAt(LocalDateTime.now());
        when(itemMapper.selectById(1L)).thenReturn(item);
        when(conversationMapper.selectOne(any())).thenReturn(conversation);
        when(stepMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(messageMapper.selectList(any())).thenReturn(Collections.emptyList());
        AtomicLong messageIds = new AtomicLong(100L);
        doAnswer(invocation -> {
            AiMessage message = invocation.getArgument(0);
            message.setMessageId(messageIds.incrementAndGet());
            return 1;
        }).when(messageMapper).insert(any(AiMessage.class));

        String providerJson = "{\"choices\":[{\"message\":{\"content\":\"重试成功\"}}]," +
                "\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":3,\"total_tokens\":15}}";
        when(restTemplate.exchange(eq("https://model.test/chat"), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("temporary timeout"))
                .thenReturn(ResponseEntity.ok(providerJson));

        AiAssistantService service = new AiAssistantService(conversationMapper, messageMapper, usageMapper,
                guard, itemMapper, stepMapper, new com.fasterxml.jackson.databind.ObjectMapper(), builder);
        ReflectionTestUtils.setField(service, "apiUrl", "https://model.test/chat");
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "deepseek-test");
        ReflectionTestUtils.setField(service, "maxRetries", 1);
        ReflectionTestUtils.setField(service, "maxHistory", 12);
        ReflectionTestUtils.setField(service, "maxTokens", 512);
        BaseContext.setCurrentId(7L);

        ChatRequest request = new ChatRequest();
        request.setItemPk(1L);
        request.setScene("LEARNING");
        request.setQuestion("请解释 Tool Use");
        assertEquals("重试成功", service.chat(request).getAnswer());
        verify(restTemplate, times(2)).exchange(eq("https://model.test/chat"), eq(HttpMethod.POST), any(), eq(String.class));

        ArgumentCaptor<AiUsageRecord> usageCaptor = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(usageMapper).insert(usageCaptor.capture());
        assertTrue(usageCaptor.getValue().getSuccess());
        assertEquals(15, usageCaptor.getValue().getTotalTokens());
        service.shutdownStreamExecutor();
    }
}
