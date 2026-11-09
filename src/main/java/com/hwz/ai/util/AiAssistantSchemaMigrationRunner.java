package com.hwz.ai.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 启动时以幂等方式补齐 AI 助学数据表，兼容已有学习平台数据库。 */
@Component
public class AiAssistantSchemaMigrationRunner implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public AiAssistantSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS ai_conversation (" +
                "conversation_id BIGINT NOT NULL AUTO_INCREMENT," +
                "user_id BIGINT NOT NULL,item_pk BIGINT NOT NULL,scene VARCHAR(20) NOT NULL," +
                "title VARCHAR(255) NULL,created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "PRIMARY KEY (conversation_id),KEY idx_ai_conversation_user_item_scene (user_id,item_pk,scene,updated_at)," +
                "KEY idx_ai_conversation_item (item_pk)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS ai_message (" +
                "message_id BIGINT NOT NULL AUTO_INCREMENT,conversation_id BIGINT NOT NULL," +
                "role VARCHAR(20) NOT NULL,content TEXT NOT NULL,source_label VARCHAR(255) NULL," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY (message_id)," +
                "KEY idx_ai_message_conversation (conversation_id),KEY idx_ai_message_created (created_at)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        // 已部署数据库需要幂等补列，先查元数据可避免重复启动时 ALTER TABLE 报错。
        Integer feedbackColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_message' AND COLUMN_NAME = 'feedback'",
                Integer.class);
        if (feedbackColumnCount == null || feedbackColumnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE ai_message ADD COLUMN feedback VARCHAR(20) NULL AFTER source_label");
        }

        // 初版唯一索引限制每个学习项只有一条会话；多会话上线时安全移除并改为普通查询索引。
        Integer oldUniqueIndexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_conversation' " +
                        "AND INDEX_NAME = 'uk_ai_conversation_user_item_scene'",
                Integer.class);
        if (oldUniqueIndexCount != null && oldUniqueIndexCount > 0) {
            jdbcTemplate.execute("ALTER TABLE ai_conversation DROP INDEX uk_ai_conversation_user_item_scene");
        }
        Integer conversationIndexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_conversation' " +
                        "AND INDEX_NAME = 'idx_ai_conversation_user_item_scene'",
                Integer.class);
        if (conversationIndexCount == null || conversationIndexCount == 0) {
            jdbcTemplate.execute("ALTER TABLE ai_conversation ADD INDEX idx_ai_conversation_user_item_scene " +
                    "(user_id, item_pk, scene, updated_at)");
        }

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS ai_usage_record (" +
                "usage_id BIGINT NOT NULL AUTO_INCREMENT,user_id BIGINT NOT NULL," +
                "conversation_id BIGINT NULL,model VARCHAR(100) NULL," +
                "prompt_tokens INT NOT NULL DEFAULT 0,completion_tokens INT NOT NULL DEFAULT 0," +
                "total_tokens INT NOT NULL DEFAULT 0,success TINYINT(1) NOT NULL DEFAULT 0," +
                "error_message VARCHAR(500) NULL,latency_ms BIGINT NOT NULL DEFAULT 0," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY (usage_id)," +
                "KEY idx_ai_usage_user_created (user_id,created_at)," +
                "KEY idx_ai_usage_conversation (conversation_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    }
}

