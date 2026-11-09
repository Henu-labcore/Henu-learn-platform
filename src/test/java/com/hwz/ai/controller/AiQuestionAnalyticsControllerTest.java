package com.hwz.ai.controller;

import com.hwz.ai.dto.AiUsageOverviewResponse;
import com.hwz.ai.service.AiQuestionAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证管理端 AI 用量统计接口的字段契约。 */
class AiQuestionAnalyticsControllerTest {

    @Test
    void shouldReturnUsageOverview() throws Exception {
        AiQuestionAnalyticsService service = mock(AiQuestionAnalyticsService.class);
        AiUsageOverviewResponse response = new AiUsageOverviewResponse();
        response.setTotalRequests(8L);
        response.setTotalTokens(1200L);
        response.setFailedRequests(1L);
        when(service.usageOverview()).thenReturn(response);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AiQuestionAnalyticsController(service)).build();

        mockMvc.perform(get("/admin/ai-assistant/usage-overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRequests").value(8))
                .andExpect(jsonPath("$.data.totalTokens").value(1200))
                .andExpect(jsonPath("$.data.failedRequests").value(1));
    }
}
