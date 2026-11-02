-- =========================================================
-- 2026-10-21 新增：掌握度历史快照（用于趋势与热力图）
--
-- 背景：
--   student_knowledge_mastery 是"当前快照"（每人每知识点一行，重算覆盖），
--   无法表达"历史变化"。本表按天保留掌握度，供趋势折线图与热力图使用。
--
-- 写入策略：
--   每次重算写入一条；同一学生/知识点/同一天只保留当天最后一次
--   （唯一键 + ON DUPLICATE KEY UPDATE），避免学生反复打开页面产生噪音。
--
-- 幂等：可重复执行。
-- =========================================================
USE labcore;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS student_mastery_history (
    history_id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '历史记录主键',
    student_id          BIGINT       NOT NULL COMMENT '学生用户ID',
    knowledge_point_id  BIGINT       NOT NULL COMMENT '知识点主键',
    learning_score      DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '当日学习行为得分',
    assessment_score    DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '当日考核表现得分',
    mastery_score       DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '当日综合掌握度',
    risk_level          VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN' COMMENT '当日风险等级：UNKNOWN/LOW/MEDIUM/HIGH',
    snapshot_date       DATE         NOT NULL COMMENT '快照日期（按天聚合）',
    calculated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (history_id),
    UNIQUE KEY uk_mastery_history_daily (student_id, knowledge_point_id, snapshot_date),
    KEY idx_mastery_history_student (student_id, snapshot_date),
    KEY idx_mastery_history_kp (knowledge_point_id, snapshot_date),
    CONSTRAINT fk_mastery_history_student
        FOREIGN KEY (student_id) REFERENCES sys_user (user_id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_mastery_history_kp
        FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point (knowledge_point_id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '学生知识点掌握度历史快照（按天）';

-- 基线：把当前已有快照补成"今天"的历史，保证趋势图上线即有起点
INSERT INTO student_mastery_history (
    student_id, knowledge_point_id,
    learning_score, assessment_score, mastery_score,
    risk_level, snapshot_date, calculated_at
)
SELECT
    skm.student_id, skm.knowledge_point_id,
    skm.learning_score, skm.assessment_score, skm.mastery_score,
    skm.risk_level, CURDATE(), skm.calculated_at
FROM student_knowledge_mastery skm
ON DUPLICATE KEY UPDATE
    learning_score = VALUES(learning_score),
    assessment_score = VALUES(assessment_score),
    mastery_score = VALUES(mastery_score),
    risk_level = VALUES(risk_level),
    calculated_at = VALUES(calculated_at);

SELECT '迁移完成：student_mastery_history' AS info, COUNT(*) AS 历史记录数 FROM student_mastery_history;
