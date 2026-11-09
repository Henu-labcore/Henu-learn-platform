package com.hwz.ai.service;

import com.hwz.admin.service.AdminAccessService;
import com.hwz.ai.dto.AiPopularItemsResponse;
import com.hwz.ai.dto.AiUsageOverviewResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 高频疑问统计：按学习内容聚合学生提问数量，帮助教师发现普遍较难的课程。
 * 这里只返回课程级计数，不读取或暴露学生的具体问题内容。
 */
@Service
public class AiQuestionAnalyticsService {
    private final JdbcTemplate jdbcTemplate;
    private final AdminAccessService adminAccessService;

    public AiQuestionAnalyticsService(JdbcTemplate jdbcTemplate, AdminAccessService adminAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.adminAccessService = adminAccessService;
    }

    public AiPopularItemsResponse popularItems() {
        adminAccessService.requireAdmin();
        List<AiPopularItemsResponse.PopularItem> items = jdbcTemplate.query(
                "SELECT c.item_pk, COALESCE(li.title, c.title, '未命名学习内容') AS title, COUNT(*) AS question_count " +
                        "FROM ai_message m JOIN ai_conversation c ON c.conversation_id = m.conversation_id " +
                        "LEFT JOIN learning_item li ON li.item_pk = c.item_pk " +
                        "WHERE m.role = 'USER' GROUP BY c.item_pk, li.title, c.title " +
                        "ORDER BY question_count DESC, c.item_pk ASC LIMIT 5",
                (resultSet, rowNum) -> {
                    AiPopularItemsResponse.PopularItem item = new AiPopularItemsResponse.PopularItem();
                    item.setItemPk(resultSet.getLong("item_pk"));
                    item.setTitle(resultSet.getString("title"));
                    item.setQuestionCount(resultSet.getLong("question_count"));
                    return item;
                });
        AiPopularItemsResponse response = new AiPopularItemsResponse();
        response.setItems(items);
        return response;
    }

    /** 汇总模型调用次数、成功率、令牌数和平均耗时，便于管理员观察本地测试用量。 */
    public AiUsageOverviewResponse usageOverview() {
        adminAccessService.requireAdmin();
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) AS total_requests," +
                        "SUM(CASE WHEN created_at >= CURDATE() THEN 1 ELSE 0 END) AS today_requests," +
                        "SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS successful_requests," +
                        "SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) AS failed_requests," +
                        "COALESCE(SUM(total_tokens), 0) AS total_tokens," +
                        "COALESCE(AVG(latency_ms), 0) AS average_latency_ms FROM ai_usage_record",
                (resultSet, rowNum) -> {
                    AiUsageOverviewResponse response = new AiUsageOverviewResponse();
                    response.setTotalRequests(resultSet.getLong("total_requests"));
                    response.setTodayRequests(resultSet.getLong("today_requests"));
                    response.setSuccessfulRequests(resultSet.getLong("successful_requests"));
                    response.setFailedRequests(resultSet.getLong("failed_requests"));
                    response.setTotalTokens(resultSet.getLong("total_tokens"));
                    response.setAverageLatencyMs(resultSet.getLong("average_latency_ms"));
                    return response;
                });
    }
}
