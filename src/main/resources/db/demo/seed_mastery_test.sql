-- =========================================================
-- 掌握度计算测试数据（8 条用例）
-- 测试学生：S1 = demo2024_001 李明(user_id=4)，S2 = demo2024_002 王芳(user_id=5)
--
-- 用例覆盖：
--   T1 双关联满分    S1×KP-AI-001  57完成+56完成 + 考核1 GRADED100 → 综合100 LOW
--   T2 混合加权      S2×KP-AI-001  57完成+56开始 + 考核1 GRADED50  → 综合58  HIGH
--   T3 双关联全零    S1×KP-AI-002  53未开始 + 考核2未提交         → 综合0   HIGH
--   T4 只学满分      S1×KP-AI-003  54完成（无考核关联）           → 综合100 LOW
--   T5 只学开始      S2×KP-AI-003  54开始（无考核关联）           → 综合40  HIGH
--   T6 只考=阈值     S1×KP-AI-004  无卡片 + 考核3 GRADED60        → 综合60  MEDIUM
--   T7 只考未批改    S2×KP-AI-004  无卡片 + 考核3 SUBMITTED无分   → 综合0   HIGH
--   T8 权重2:1       S1×KP-CR-001  42完成(权2)+49开始(权1)        → 综合80  LOW
--
-- 幂等：先按范围 DELETE 再 INSERT，可重复执行；只动测试学生，不影响他人数据。
-- =========================================================

USE labcore;

START TRANSACTION;

-- ---------- 1. 清理测试范围旧数据 ----------
DELETE FROM learning_record
WHERE user_id IN (4, 5)
  AND item_pk IN (53, 54, 56, 57, 42, 49);

DELETE FROM assignment_submission
WHERE student_id IN (4, 5)
  AND assignment_id IN (1, 2, 3);

-- ---------- 2. S1（user_id=4 李明）学习记录 ----------
-- T1/T8：57 完成、56 完成、54 完成、42 完成、49 开始（53 不建记录=未开始 → T3）
INSERT INTO learning_record (user_id, item_pk, first_learn_time, complete_time, complete_remark, learn_duration_sec) VALUES
(4, 57, '2026-10-01 09:00:00', '2026-10-01 10:30:00', '完成多智能体协作系统学习', 5400),
(4, 56, '2026-10-02 09:00:00', '2026-10-02 10:00:00', '完成代码沙箱学习', 3600),
(4, 54, '2026-10-03 09:00:00', '2026-10-03 10:30:00', '完成工具调用学习', 5400),
(4, 42, '2026-10-04 09:00:00', '2026-10-04 11:00:00', '完成AES学习', 7200),
(4, 49, '2026-10-05 14:00:00', NULL, NULL, 1800);

-- ---------- 3. S2（user_id=5 王芳）学习记录 ----------
-- T2/T5：57 完成、56 开始、54 开始（53/42/49 不建记录 → 未开始）
INSERT INTO learning_record (user_id, item_pk, first_learn_time, complete_time, complete_remark, learn_duration_sec) VALUES
(5, 57, '2026-10-01 15:00:00', '2026-10-01 16:00:00', '完成多智能体学习', 3600),
(5, 56, '2026-10-02 15:00:00', NULL, NULL, 900),
(5, 54, '2026-10-03 15:00:00', NULL, NULL, 1200);

-- ---------- 4. S1 考核提交 ----------
-- T1：考核1 GRADED 100；T6：考核3 GRADED 60；考核2 不建=未提交 → T3
INSERT INTO assignment_submission (assignment_id, student_id, status, answer_text, submitted_at, graded_at, score, feedback, graded_by) VALUES
(1, 4, 'GRADED', '智能体方案设计提交-李明', '2026-10-06 09:00:00', '2026-10-07 10:00:00', 100.00, '方案完整，思路清晰', 1),
(3, 4, 'GRADED', '代码沙箱调试提交-李明', '2026-10-06 10:00:00', '2026-10-07 11:00:00', 60.00, '基本完成', 1);

-- ---------- 5. S2 考核提交 ----------
-- T2：考核1 GRADED 50；T7：考核3 SUBMITTED 无分
INSERT INTO assignment_submission (assignment_id, student_id, status, answer_text, submitted_at, graded_at, score, feedback, graded_by) VALUES
(1, 5, 'GRADED', '智能体方案设计提交-王芳', '2026-10-06 11:00:00', '2026-10-07 12:00:00', 50.00, '方案基本可行，缺细节', 1),
(3, 5, 'SUBMITTED', '代码沙箱调试提交-王芳', '2026-10-06 12:00:00', NULL, NULL, NULL, NULL);

COMMIT;

-- ---------- 6. 结果核对提示 ----------
SELECT '测试数据就绪，可重算后对照用例期望值' AS info;
