-- Demo assessment data for local UI review.
-- Safe to re-run: existing demo assessments with title prefix "样例-" are removed first.

-- 本地开发初始化脚本创建的是 labcore_test，演示数据必须写入同一数据库。
-- 这里显式指定目标库，避免误写入其他同名业务库。
USE `labcore_test`;
SET NAMES utf8mb4;

SET @admin_id = (SELECT user_id FROM sys_user WHERE username = 'admin' LIMIT 1);
SET @student_a = COALESCE((SELECT user_id FROM sys_user WHERE role <> 'ADMIN' ORDER BY user_id LIMIT 1), @admin_id);
SET @student_b = COALESCE((SELECT user_id FROM sys_user WHERE role <> 'ADMIN' AND user_id <> @student_a ORDER BY user_id LIMIT 1), @student_a);

DELETE FROM assignment WHERE title LIKE '样例-%能力考核%';

INSERT INTO assignment (title, description, category, deadline, total_score, status, created_by, created_at, updated_at)
VALUES
('样例-智能体方案设计能力考核', '围绕一个真实教学或科研场景，设计一个具备角色分工、工具调用和记忆管理能力的智能体方案，并提交方案文档与演示视频。', '智能体', DATE_ADD(NOW(), INTERVAL 7 DAY), 100.00, 'PUBLISHED', @admin_id, NOW(), NOW()),
('样例-Prompt Engineering 实战能力考核', '完成提示词结构化设计、对比实验与效果复盘，重点考察系统提示、用户输入、约束条件和输出格式控制能力。', '智能体', DATE_ADD(NOW(), INTERVAL 3 DAY), 100.00, 'PUBLISHED', @admin_id, NOW(), NOW()),
('样例-Code Sandbox 调试能力考核', '在沙箱环境中复现一段代码生成任务，记录调试过程、错误定位方法和最终可运行结果。', '智能体', DATE_SUB(NOW(), INTERVAL 1 DAY), 100.00, 'PUBLISHED', @admin_id, NOW(), NOW()),
('样例-课程综合展示能力考核', '用于后台预览的草稿考核：要求学生综合展示一个完整学习成果，包括问题定义、技术路线、实验结果和反思。', '综合考核', DATE_ADD(NOW(), INTERVAL 14 DAY), 100.00, 'DRAFT', @admin_id, NOW(), NOW());

SET @a1 = (SELECT assignment_id FROM assignment WHERE title = '样例-智能体方案设计能力考核' LIMIT 1);
SET @a2 = (SELECT assignment_id FROM assignment WHERE title = '样例-Prompt Engineering 实战能力考核' LIMIT 1);
SET @a3 = (SELECT assignment_id FROM assignment WHERE title = '样例-Code Sandbox 调试能力考核' LIMIT 1);
SET @a4 = (SELECT assignment_id FROM assignment WHERE title = '样例-课程综合展示能力考核' LIMIT 1);

INSERT INTO assignment_question (assignment_id, title, content, score, sort_order, created_at, updated_at)
VALUES
(@a1, '场景与目标定义', '说明智能体服务的对象、业务流程、核心目标和成功标准。要求描述清楚输入、输出和约束条件。', 30.00, 1, NOW(), NOW()),
(@a1, '智能体架构设计', '画出或描述角色分工、任务拆解、工具调用、记忆机制和异常处理流程。', 40.00, 2, NOW(), NOW()),
(@a1, '演示与反思', '提交一段演示视频，并说明当前方案的不足、风险和后续优化方向。', 30.00, 3, NOW(), NOW()),
(@a2, '提示词拆解', '选取一个大模型任务，将提示词拆解为角色、目标、上下文、约束、输出格式五个部分。', 35.00, 1, NOW(), NOW()),
(@a2, '对比实验', '至少设计两组提示词版本，比较输出质量、稳定性和可复用性。', 40.00, 2, NOW(), NOW()),
(@a2, '复盘报告', '总结提示词调优过程中的有效策略和失败案例。', 25.00, 3, NOW(), NOW()),
(@a3, '任务复现', '在沙箱环境中复现代码生成任务，并截图或记录运行结果。', 30.00, 1, NOW(), NOW()),
(@a3, '错误定位', '列出至少两个出现过的错误，说明定位思路、修改方法和验证结果。', 40.00, 2, NOW(), NOW()),
(@a3, '最终交付', '提交可运行代码、运行说明和结果说明。', 30.00, 3, NOW(), NOW()),
(@a4, '综合展示材料', '整理课程学习成果，形成一份结构完整的汇报文档。', 50.00, 1, NOW(), NOW()),
(@a4, '展示视频', '录制 3-5 分钟展示视频，说明技术路线和关键成果。', 50.00, 2, NOW(), NOW());

INSERT INTO assignment_submission (assignment_id, student_id, status, submitted_at, graded_at, score, feedback, graded_by, created_at, updated_at)
VALUES
(@a1, @admin_id, 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, NULL, NULL, NULL, NOW(), NOW()),
(@a2, @admin_id, 'GRADED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 6 HOUR), 92.00, '结构清晰，实验对比充分；后续可以补充更多失败案例分析。', @admin_id, NOW(), NOW()),
(@a3, @admin_id, 'LATE', DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, NULL, NULL, NULL, NOW(), NOW()),
(@a1, @student_a, 'GRADED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 5 HOUR), 88.00, '方案完整，演示有效；工具调用链路还可以再细化。', @admin_id, NOW(), NOW()),
(@a2, @student_a, 'RETURNED', DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR), NULL, '请补充对比实验截图，并重新上传最终文档。', @admin_id, NOW(), NOW());

INSERT INTO assignment_submission (assignment_id, student_id, status, submitted_at, graded_at, score, feedback, graded_by, created_at, updated_at)
SELECT @a3, @student_b, 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, NULL, NULL, NULL, NOW(), NOW()
WHERE @student_b <> @student_a;

SET @s_admin_a1 = (SELECT submission_id FROM assignment_submission WHERE assignment_id = @a1 AND student_id = @admin_id LIMIT 1);
SET @s_admin_a2 = (SELECT submission_id FROM assignment_submission WHERE assignment_id = @a2 AND student_id = @admin_id LIMIT 1);
SET @s_admin_a3 = (SELECT submission_id FROM assignment_submission WHERE assignment_id = @a3 AND student_id = @admin_id LIMIT 1);
SET @s_student_a1 = (SELECT submission_id FROM assignment_submission WHERE assignment_id = @a1 AND student_id = @student_a LIMIT 1);
SET @s_student_a2 = (SELECT submission_id FROM assignment_submission WHERE assignment_id = @a2 AND student_id = @student_a LIMIT 1);
SET @s_student_b3 = (SELECT submission_id FROM assignment_submission WHERE assignment_id = @a3 AND student_id = @student_b LIMIT 1);

UPDATE assignment_submission
SET answer_text = '我设计的智能体采用“规划者-执行者-评审者”三角色结构，先由规划者拆解任务，再由执行者调用检索、代码和记忆工具，最后由评审者检查结果一致性。附件中补充了流程图和演示视频。'
WHERE submission_id = @s_admin_a1;

UPDATE assignment_submission
SET answer_text = '本次 Prompt 实验重点比较了宽泛提示与结构化提示。结构化版本包含角色、目标、约束、输出格式和评价标准，输出稳定性明显更好。'
WHERE submission_id = @s_admin_a2;

UPDATE assignment_submission
SET answer_text = '沙箱调试过程主要问题是依赖缺失和路径不一致。我记录了错误定位步骤，并在附件中提交最终运行说明。'
WHERE submission_id = @s_admin_a3;

UPDATE assignment_submission
SET answer_text = '方案围绕科研资料整理场景展开，智能体包含资料收集、摘要生成、交叉验证和报告输出四个阶段。'
WHERE submission_id = @s_student_a1;

UPDATE assignment_submission
SET answer_text = '已提交第一版提示词设计，但对比实验截图还不完整。'
WHERE submission_id = @s_student_a2;

UPDATE assignment_submission
SET answer_text = '我在沙箱中完成了一个代码生成任务，并记录了依赖安装、错误复现和最终验证步骤。'
WHERE submission_id = @s_student_b3;

INSERT INTO assignment_submission_file (submission_id, file_type, original_name, stored_path, mime_type, file_size, created_at)
VALUES
(@s_admin_a1, 'DOCUMENT', '智能体方案设计-Admin.docx', 'demo/agent-plan-admin.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 24576, NOW()),
(@s_admin_a1, 'VIDEO', '智能体方案演示-Admin.mp4', 'demo/agent-plan-admin.mp4', 'video/mp4', 2097152, NOW()),
(@s_admin_a2, 'DOCUMENT', 'Prompt实验报告-Admin.pdf', 'demo/prompt-report-admin.pdf', 'application/pdf', 98304, NOW()),
(@s_admin_a3, 'DOCUMENT', 'CodeSandbox调试记录-Admin.pdf', 'demo/code-sandbox-admin.pdf', 'application/pdf', 65536, NOW()),
(@s_student_a1, 'DOCUMENT', '智能体方案设计-学生A.docx', 'demo/agent-plan-student-a.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 32768, NOW()),
(@s_student_a2, 'DOCUMENT', 'Prompt实验报告-学生A.pdf', 'demo/prompt-report-student-a.pdf', 'application/pdf', 90112, NOW());

INSERT INTO assignment_submission_file (submission_id, file_type, original_name, stored_path, mime_type, file_size, created_at)
SELECT @s_student_b3, 'VIDEO', 'CodeSandbox演示-学生B.mp4', 'demo/code-sandbox-student-b.mp4', 'video/mp4', 3145728, NOW()
WHERE @student_b <> @student_a;
