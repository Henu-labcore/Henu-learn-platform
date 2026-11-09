package com.hwz.ai.controller;

import com.hwz.ai.dto.AiAssistantDtos.ConversationResponse;
import com.hwz.ai.dto.AiAssistantDtos.ChatRequest;
import com.hwz.ai.dto.AiAssistantDtos.HistoryResponse;
import com.hwz.ai.service.AiAssistantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 多会话 HTTP 契约测试：覆盖列表、新建、重命名、删除和指定会话历史。 */
class AiAssistantControllerTest {
    private AiAssistantService service;
    private AiAssistantController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AiAssistantService.class);
        controller = new AiAssistantController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldExposeConversationManagementEndpoints() throws Exception {
        ConversationResponse conversation = new ConversationResponse();
        conversation.setConversationId(9L);
        conversation.setTitle("调试记录");
        when(service.listConversations(1L, "EXPERIMENT")).thenReturn(Collections.singletonList(conversation));
        when(service.createConversation(any())).thenReturn(conversation);
        when(service.renameConversation(eq(9L), any())).thenReturn(conversation);

        mockMvc.perform(get("/ai-assistant/conversations").param("itemPk", "1").param("scene", "EXPERIMENT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].conversationId").value(9));
        mockMvc.perform(post("/ai-assistant/conversations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemPk\":1,\"scene\":\"EXPERIMENT\",\"title\":\"调试记录\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("调试记录"));
        mockMvc.perform(put("/ai-assistant/conversations/9").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"调试记录\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/ai-assistant/conversations/9")).andExpect(status().isOk());
        verify(service).deleteConversation(9L);
    }

    @Test
    void shouldPassConversationIdWhenLoadingHistory() throws Exception {
        HistoryResponse history = new HistoryResponse();
        history.setConversationId(9L);
        history.setMessages(Collections.emptyList());
        when(service.history(1L, "LEARNING", 9L)).thenReturn(history);

        mockMvc.perform(get("/ai-assistant/history").param("itemPk", "1")
                        .param("scene", "LEARNING").param("conversationId", "9"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.conversationId").value(9));
    }

    /** SSE响应必须明确禁止代理缓冲，否则模型增量可能在代理层被合并后一次性返回。 */
    @Test
    void shouldDisableBufferingForStreamingChat() {
        ChatRequest request = new ChatRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        SseEmitter emitter = new SseEmitter();
        when(service.streamChat(request)).thenReturn(emitter);

        assertSame(emitter, controller.streamChat(request, response));
        assertEquals("no-cache, no-transform", response.getHeader("Cache-Control"));
        assertEquals("no", response.getHeader("X-Accel-Buffering"));
        assertEquals("UTF-8", response.getCharacterEncoding());
    }
}
