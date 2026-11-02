-- =========================================================
-- 2026-10-26 新增：学生画像快照（按天）
--
-- 用途：
--   学生画像页（五维雷达 / 类型 / 标签 / 建议）按天留档，
--   支持最近 30 天趋势；不改变现有掌握度快照表。
--
-- 写入策略：
--   同一学生同一天只保留最后一次（唯一键 + ON DUPLICATE KEY UPDATE）。
--
-- 幂等：可重复执行。
-- =========================================================
USE labcore;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS student_profile_snapshot (
    profile_id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '画像快照主键',
    student_id          BIGINT       NOT NULL COMMENT '学生用户ID',
    snapshot_date       DATE         NOT NULL COMMENT '快照日期',
    knowledge_score     DECIMAL(5,2) NULL COMMENT '知识掌握维度分（无接触数据时为空）',
    assessment_score    DECIMAL(5,2) NULL COMMENT '考核表现维度分（无已批改时为空）',
    engagement_score    DECIMAL(5,2) NULL COMMENT '学习投入维度分',
    stability_score     DECIMAL(5,2) NULL COMMENT '学习稳定维度分',
    execution_score     DECIMAL(5,2) NULL COMMENT '任务执行维度分（无计划时为空）',
    persona_code        VARCHAR(40)  NOT NULL DEFAULT 'INSUFFICIENT' COMMENT '画像类型编码',
    persona_label       VARCHAR(60)  NOT NULL DEFAULT '数据积累中' COMMENT '画像类型名称',
    tags_json           TEXT         NULL COMMENT '标签 JSON 数组',
    confidence_level    VARCHAR(20)  NOT NULL DEFAULT 'LOW' COMMENT '置信度：HIGH/MEDIUM/LOW',
    calculated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (profile_id),
    UNIQUE KEY uk_profile_daily (student_id, snapshot_date),
    KEY idx_profile_student_date (student_id, snapshot_date),
    CONSTRAINT fk_profile_student
        FOREIGN KEY (student_id) REFERENCES sys_user (user_id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '学生画像按天快照';

SELECT '迁移完成：student_profile_snapshot' AS info;
