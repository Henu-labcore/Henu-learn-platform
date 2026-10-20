-- =========================================================
-- 2026-10-20 新增模块：
-- 知识点掌握度计算与薄弱知识点识别（学习能力画像）
--
-- 合入分支：lijiaqing
-- 设计说明：
-- 1. 共新增 5 张表（不含计划表，计划表与 caiyaqi 的 learning_plan 撞名，后续另议）；
-- 2. 种子知识点 10 个，覆盖平台全部 6 大学习分类；
-- 3. 学习资源关联与考核关联均使用现有真实数据（item_pk / assignment_id）；
-- 4. 幂等：可重复执行。
-- =========================================================

USE labcore;

-- =========================================================
-- 1. 知识点表
-- =========================================================
CREATE TABLE IF NOT EXISTS knowledge_point (
    knowledge_point_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识点主键',
    code VARCHAR(64) NOT NULL COMMENT '知识点编码',
    name VARCHAR(120) NOT NULL COMMENT '知识点名称',
    description TEXT NULL COMMENT '知识点说明',
    category VARCHAR(100) NULL COMMENT '知识点分类',
    difficulty VARCHAR(20) NOT NULL DEFAULT 'INTERMEDIATE' COMMENT '难度：BASIC、INTERMEDIATE、ADVANCED',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE、INACTIVE',
    warning_threshold DECIMAL(5, 2) NOT NULL DEFAULT 60.00 COMMENT '薄弱预警阈值，掌握度低于该值时预警',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    created_by BIGINT NULL COMMENT '创建人用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (knowledge_point_id),
    UNIQUE KEY uk_knowledge_point_code (code),
    KEY idx_knowledge_point_name (name),
    KEY idx_knowledge_point_category (category),
    KEY idx_knowledge_point_status (status),
    KEY idx_knowledge_point_created_by (created_by),
    CONSTRAINT fk_knowledge_point_created_by
        FOREIGN KEY (created_by) REFERENCES sys_user (user_id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识点表';

-- =========================================================
-- 2. 学习资源与知识点关联表
-- =========================================================
CREATE TABLE IF NOT EXISTS learning_item_knowledge_point (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联记录主键',
    item_pk BIGINT NOT NULL COMMENT '学习资源主键',
    knowledge_point_id BIGINT NOT NULL COMMENT '知识点主键',
    weight DECIMAL(5, 2) NOT NULL DEFAULT 1.00 COMMENT '该知识点在学习资源中的权重',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_knowledge_point (item_pk, knowledge_point_id),
    KEY idx_likp_knowledge_point (knowledge_point_id),
    CONSTRAINT fk_likp_learning_item
        FOREIGN KEY (item_pk) REFERENCES learning_item (item_pk)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_likp_knowledge_point
        FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point (knowledge_point_id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学习资源与知识点关联表';

-- =========================================================
-- 3. 考核与知识点关联表
-- =========================================================
CREATE TABLE IF NOT EXISTS assignment_knowledge_point (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联记录主键',
    assignment_id BIGINT NOT NULL COMMENT '考核主键',
    knowledge_point_id BIGINT NOT NULL COMMENT '知识点主键',
    weight DECIMAL(5, 2) NOT NULL DEFAULT 1.00 COMMENT '该知识点在考核中的权重',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assignment_knowledge_point (assignment_id, knowledge_point_id),
    KEY idx_akp_knowledge_point (knowledge_point_id),
    CONSTRAINT fk_akp_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignment (assignment_id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_akp_knowledge_point
        FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point (knowledge_point_id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '考核与知识点关联表';

-- =========================================================
-- 4. 知识点前置关系表（供后续计划模块使用，先建表不接逻辑）
-- =========================================================
CREATE TABLE IF NOT EXISTS knowledge_point_prerequisite (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '前置关系主键',
    knowledge_point_id BIGINT NOT NULL COMMENT '当前知识点主键',
    prerequisite_id BIGINT NOT NULL COMMENT '前置知识点主键',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_knowledge_prerequisite (knowledge_point_id, prerequisite_id),
    KEY idx_kpp_prerequisite (prerequisite_id),
    CONSTRAINT fk_kpp_knowledge_point
        FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point (knowledge_point_id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_kpp_prerequisite
        FOREIGN KEY (prerequisite_id) REFERENCES knowledge_point (knowledge_point_id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识点前置关系表';

-- =========================================================
-- 5. 学生知识点掌握度表
-- =========================================================
CREATE TABLE IF NOT EXISTS student_knowledge_mastery (
    mastery_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '掌握度记录主键',
    student_id BIGINT NOT NULL COMMENT '学生用户ID',
    knowledge_point_id BIGINT NOT NULL COMMENT '知识点主键',
    learning_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '学习行为得分，范围0到100',
    assessment_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '考核表现得分，范围0到100',
    mastery_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '综合掌握度，范围0到100',
    risk_level VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT '风险等级：UNKNOWN、LOW、MEDIUM、HIGH',
    completed_learning_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已完成关联学习资源数量',
    total_learning_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关联学习资源总数量',
    graded_assignment_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已有成绩的关联考核数量',
    total_assignment_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关联考核总数量',
    last_learning_time DATETIME NULL COMMENT '最近学习时间',
    last_assessment_time DATETIME NULL COMMENT '最近考核或批改时间',
    calculated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近计算时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (mastery_id),
    UNIQUE KEY uk_student_knowledge_mastery (student_id, knowledge_point_id),
    KEY idx_skm_knowledge_point (knowledge_point_id),
    KEY idx_skm_risk_level (risk_level),
    KEY idx_skm_mastery_score (mastery_score),
    CONSTRAINT fk_skm_student
        FOREIGN KEY (student_id) REFERENCES sys_user (user_id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_skm_knowledge_point
        FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point (knowledge_point_id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生知识点掌握度与薄弱预警表';

-- =========================================================
-- 6. 种子知识点（10 个，覆盖 6 大分类）
-- =========================================================
INSERT INTO knowledge_point (
    code, name, description, category, difficulty, status,
    warning_threshold, sort_order, created_by
) VALUES
    ('KP-AI-001', '智能体架构设计',
     '掌握智能体的角色划分、任务规划、执行流程、状态管理与模块协作关系。',
     '智能体', 'BASIC', 'ACTIVE', 65.00, 10,
     (SELECT user_id FROM sys_user WHERE username = 'admin' LIMIT 1)),
    ('KP-AI-002', 'Prompt工程',
     '掌握提示词结构设计、上下文组织、约束表达、输出格式控制与提示词效果评估。',
     '智能体', 'INTERMEDIATE', 'ACTIVE', 60.00, 20,
     (SELECT user_id FROM sys_user WHERE username = 'admin' LIMIT 1)),
    ('KP-AI-003', '工具调用与任务执行',
     '掌握智能体工具注册、参数构造、工具选择、调用结果处理和失败重试机制。',
     '智能体', 'INTERMEDIATE', 'ACTIVE', 60.00, 30,
     (SELECT user_id FROM sys_user WHERE username = 'admin' LIMIT 1)),
    ('KP-AI-004', '代码沙箱与执行安全',
     '掌握代码执行隔离、权限控制、资源限制、异常处理及不可信代码安全防护。',
     '智能体安全', 'ADVANCED', 'ACTIVE', 60.00, 40,
     (SELECT user_id FROM sys_user WHERE username = 'admin' LIMIT 1)),
    ('KP-AI-005', '智能体记忆机制',
     '掌握短期记忆、长期记忆、会话上下文、记忆检索与记忆更新策略。',
     '智能体', 'INTERMEDIATE', 'ACTIVE', 65.00, 50,
     (SELECT user_id FROM sys_user WHERE username = 'admin' LIMIT 1)),
    ('KP-CR-001', '对称加密原理与模式',
     '掌握对称加密算法原理及 ECB/CBC/CFB/OFB/CTR/GCM 等分组密码工作模式。',
     '现代密码学', 'INTERMEDIATE', 'ACTIVE', 60.00, 60,
     (SELECT user_id FROM sys_user WHERE username = 'admin' LIMIT 1)),
    ('KP-CL-001', '古典密码基础',
     '掌握凯撒、维吉尼亚、栅栏等古典密码的加密原理、破译思路与历史演变。',
     '古典密码学', 'BASIC', 'ACTIVE', 60.00, 70,
     (SELECT user_id FROM sys_user WHERE username = 'admin' LIMIT 1)),
    ('KP-PR-001', '密码协议设计',
     '掌握密钥管理、认证协议、签名协议的设计原理与安全属性。',
     '密码协议设计专项', 'INTERMEDIATE', 'ACTIVE', 60.00, 80,
     (SELECT user_id FROM sys_user WHERE username = 'admin' LIMIT 1)),
    ('KP-BC-001', '区块链安全基础',
     '掌握区块链核心共识协议、数字货币协议与智能合约安全漏洞及防御。',
     '区块链安全专项', 'INTERMEDIATE', 'ACTIVE', 60.00, 90,
     (SELECT user_id FROM sys_user WHERE username = 'admin' LIMIT 1)),
    ('KP-WM-001', '数字水印技术',
     '掌握数字水印综述、频域水印系统实现（DCT+DWT）与综合应用流程。',
     '数字水印', 'INTERMEDIATE', 'ACTIVE', 60.00, 100,
     (SELECT user_id FROM sys_user WHERE username = 'admin' LIMIT 1))
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    category = VALUES(category),
    difficulty = VALUES(difficulty),
    status = VALUES(status),
    warning_threshold = VALUES(warning_threshold),
    sort_order = VALUES(sort_order),
    created_by = VALUES(created_by);

-- =========================================================
-- 7. 种子关联：学习资源 ↔ 知识点（权重）
-- =========================================================
INSERT INTO learning_item_knowledge_point (item_pk, knowledge_point_id, weight)
SELECT li.item_pk, kp.knowledge_point_id, w.w
FROM (
    SELECT '57' AS item, 'KP-AI-001' AS code, 1.00 AS w
    UNION ALL SELECT '56', 'KP-AI-001', 1.00
    UNION ALL SELECT '53', 'KP-AI-002', 1.00
    UNION ALL SELECT '54', 'KP-AI-003', 1.00
    UNION ALL SELECT '55', 'KP-AI-005', 1.00
    UNION ALL SELECT '42', 'KP-CR-001', 2.00
    UNION ALL SELECT '49', 'KP-CR-001', 1.00
    UNION ALL SELECT '1',  'KP-CL-001', 1.00
    UNION ALL SELECT '3',  'KP-CL-001', 1.00
    UNION ALL SELECT '15', 'KP-PR-001', 1.00
    UNION ALL SELECT '16', 'KP-PR-001', 1.00
    UNION ALL SELECT '18', 'KP-BC-001', 1.00
    UNION ALL SELECT '20', 'KP-BC-001', 1.00
    UNION ALL SELECT '11', 'KP-WM-001', 1.00
    UNION ALL SELECT '12', 'KP-WM-001', 1.00
) w
JOIN learning_item li ON li.item_pk = CAST(w.item AS UNSIGNED)
JOIN knowledge_point kp ON kp.code = w.code
ON DUPLICATE KEY UPDATE weight = VALUES(weight);

-- =========================================================
-- 8. 种子关联：考核 ↔ 知识点（权重）
-- =========================================================
INSERT INTO assignment_knowledge_point (assignment_id, knowledge_point_id, weight)
SELECT a.assignment_id, kp.knowledge_point_id, 1.00
FROM (
    SELECT '1' AS assignment, 'KP-AI-001' AS code
    UNION ALL SELECT '2', 'KP-AI-002'
    UNION ALL SELECT '3', 'KP-AI-004'
) w
JOIN assignment a ON a.assignment_id = CAST(w.assignment AS UNSIGNED)
JOIN knowledge_point kp ON kp.code = w.code
ON DUPLICATE KEY UPDATE weight = VALUES(weight);

-- =========================================================
-- 8.5 种子关联：知识点前置关系
-- =========================================================
INSERT INTO knowledge_point_prerequisite (knowledge_point_id, prerequisite_id)
SELECT kp.knowledge_point_id, pre.knowledge_point_id
FROM (
    SELECT 'KP-AI-002' AS code, 'KP-AI-001' AS pre_code
    UNION ALL SELECT 'KP-AI-003', 'KP-AI-001'
    UNION ALL SELECT 'KP-AI-004', 'KP-AI-001'
) w
JOIN knowledge_point kp ON kp.code = w.code
JOIN knowledge_point pre ON pre.code = w.pre_code
ON DUPLICATE KEY UPDATE prerequisite_id = VALUES(prerequisite_id);

-- =========================================================
-- 9. 完成提示
-- =========================================================
SELECT '迁移完成：knowledge_point 表' AS info, COUNT(*) AS 知识点数 FROM knowledge_point;
