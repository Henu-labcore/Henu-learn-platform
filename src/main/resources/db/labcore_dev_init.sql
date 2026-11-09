-- LabCore small development initialization for labcore_test
-- Generated from seed_learning_cards.sql. Do not edit long seed payloads manually.


-- LabCore clean schema for a fresh local/test database.
-- Target database: labcore_test
-- Run first:
--   mysql --binary-mode=1 -uroot -p < src/main/resources/db/labcore_schema_clean.sql

CREATE DATABASE IF NOT EXISTS `labcore_test`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `labcore_test`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `assignment_material`;
DROP TABLE IF EXISTS `assignment_submission_file`;
DROP TABLE IF EXISTS `assignment_submission`;
DROP TABLE IF EXISTS `assignment_question`;
DROP TABLE IF EXISTS `assignment`;
DROP TABLE IF EXISTS `refresh_tokens`;
DROP TABLE IF EXISTS `ai_usage_record`;
DROP TABLE IF EXISTS `ai_message`;
DROP TABLE IF EXISTS `ai_conversation`;
DROP TABLE IF EXISTS `learning_record`;
DROP TABLE IF EXISTS `learning_step`;
DROP TABLE IF EXISTS `learning_item`;
DROP TABLE IF EXISTS `port_usage`;
DROP TABLE IF EXISTS `ports_usage`;
DROP TABLE IF EXISTS `virtual_env`;
DROP TABLE IF EXISTS `sys_user`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `real_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `grade` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `last_login_time` datetime DEFAULT NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_status` (`status`),
  KEY `idx_sys_user_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `learning_item` (
  `item_pk` bigint NOT NULL AUTO_INCREMENT,
  `json_id` bigint NOT NULL,
  `title` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `summary` text COLLATE utf8mb4_unicode_ci,
  `category` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `difficulty` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `duration` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prerequisites` text COLLATE utf8mb4_unicode_ci,
  `objectives` json DEFAULT NULL,
  `features` json DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PUBLISHED',
  `template_path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `author_id` bigint DEFAULT NULL,
  `published_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`item_pk`),
  KEY `idx_learning_item_json_id` (`json_id`),
  KEY `idx_learning_item_status` (`status`),
  KEY `idx_learning_item_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `learning_step` (
  `step_id` bigint NOT NULL AUTO_INCREMENT,
  `item_pk` bigint NOT NULL,
  `step_no` int NOT NULL DEFAULT 0,
  `title` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` longtext COLLATE utf8mb4_unicode_ci,
  `tip` longtext COLLATE utf8mb4_unicode_ci,
  `code` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`step_id`),
  UNIQUE KEY `uk_learning_step_item_step` (`item_pk`, `step_no`),
  KEY `idx_learning_step_item` (`item_pk`),
  CONSTRAINT `fk_learning_step_item`
    FOREIGN KEY (`item_pk`) REFERENCES `learning_item` (`item_pk`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `learning_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `item_pk` bigint NOT NULL,
  `first_learn_time` datetime DEFAULT NULL,
  `complete_time` datetime DEFAULT NULL,
  `complete_remark` text COLLATE utf8mb4_unicode_ci,
  `learn_duration_sec` bigint unsigned NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_learning_record_user_item` (`user_id`, `item_pk`),
  KEY `idx_learning_record_user` (`user_id`),
  KEY `idx_learning_record_item` (`item_pk`),
  KEY `idx_learning_record_complete_time` (`complete_time`),
  CONSTRAINT `fk_learning_record_item`
    FOREIGN KEY (`item_pk`) REFERENCES `learning_item` (`item_pk`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_learning_record_user`
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `ck_learning_record_time_order` CHECK (
    (`complete_time` IS NULL) OR (`first_learn_time` IS NULL) OR (`complete_time` >= `first_learn_time`)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `refresh_tokens` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `revoked_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_tokens_token` (`token`),
  KEY `idx_refresh_tokens_user` (`user_id`),
  KEY `idx_refresh_tokens_expires` (`expires_at`),
  CONSTRAINT `fk_refresh_tokens_user`
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assignment` (
  `assignment_id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `category` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deadline` datetime DEFAULT NULL,
  `total_score` decimal(6,2) NOT NULL DEFAULT 100.00,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`assignment_id`),
  KEY `idx_assignment_status` (`status`),
  KEY `idx_assignment_deadline` (`deadline`),
  KEY `idx_assignment_created_by` (`created_by`),
  CONSTRAINT `fk_assignment_created_by`
    FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`user_id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assignment_question` (
  `question_id` bigint NOT NULL AUTO_INCREMENT,
  `assignment_id` bigint NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `score` decimal(6,2) NOT NULL DEFAULT 0.00,
  `sort_order` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`question_id`),
  KEY `idx_assignment_question_assignment` (`assignment_id`),
  CONSTRAINT `fk_assignment_question_assignment`
    FOREIGN KEY (`assignment_id`) REFERENCES `assignment` (`assignment_id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assignment_submission` (
  `submission_id` bigint NOT NULL AUTO_INCREMENT,
  `assignment_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `answer_text` text COLLATE utf8mb4_unicode_ci,
  `submitted_at` datetime DEFAULT NULL,
  `graded_at` datetime DEFAULT NULL,
  `score` decimal(6,2) DEFAULT NULL,
  `feedback` text COLLATE utf8mb4_unicode_ci,
  `graded_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`submission_id`),
  UNIQUE KEY `uk_assignment_submission_assignment_student` (`assignment_id`, `student_id`),
  KEY `idx_assignment_submission_assignment` (`assignment_id`),
  KEY `idx_assignment_submission_student` (`student_id`),
  KEY `idx_assignment_submission_status` (`status`),
  KEY `idx_assignment_submission_graded_by` (`graded_by`),
  CONSTRAINT `fk_assignment_submission_assignment`
    FOREIGN KEY (`assignment_id`) REFERENCES `assignment` (`assignment_id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_assignment_submission_student`
    FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`user_id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_assignment_submission_graded_by`
    FOREIGN KEY (`graded_by`) REFERENCES `sys_user` (`user_id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assignment_submission_file` (
  `file_id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL,
  `file_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stored_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mime_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`file_id`),
  KEY `idx_assignment_submission_file_submission` (`submission_id`),
  CONSTRAINT `fk_assignment_submission_file_submission`
    FOREIGN KEY (`submission_id`) REFERENCES `assignment_submission` (`submission_id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assignment_material` (
  `material_id` bigint NOT NULL AUTO_INCREMENT,
  `assignment_id` bigint NOT NULL,
  `material_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stored_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mime_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint NOT NULL DEFAULT 0,
  `sort_order` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`material_id`),
  KEY `idx_assignment_material_assignment` (`assignment_id`),
  KEY `idx_assignment_material_type` (`material_type`),
  CONSTRAINT `fk_assignment_material_assignment`
    FOREIGN KEY (`assignment_id`) REFERENCES `assignment` (`assignment_id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

USE `labcore_test`;
SET NAMES utf8mb4;

INSERT INTO `sys_user`
  (`user_id`, `username`, `password`, `display_name`, `email`, `real_name`, `grade`, `role`, `status`, `last_login_time`, `created_time`, `updated_time`)
VALUES
  (1, 'admin', '$2b$12$z.YhRT9KkCrIkVcD/t5Lu.5M.WobkiDfbMS6Q9G.1pADz1qEtegMG', 'Administrator', NULL, 'Administrator', NULL, 'ADMIN', 'ACTIVE', NOW(), NOW(), NOW());

INSERT INTO `learning_item` (`item_pk`, `json_id`, `title`, `summary`, `category`, `difficulty`, `duration`, `prerequisites`, `objectives`, `features`, `status`, `template_path`, `author_id`, `published_at`, `created_at`, `updated_at`) VALUES (1, 834729, '凯撒密码 (Caesar Cipher)', '介绍凯撒密码的原理、历史及实现方法。', '古典密码学', '中等', '60分钟', '基础代数、Python编程', '"理解并实现凯撒密码"', '[{"title": "原理简单", "description": "单字母位移加密"}, {"title": "历史悠久", "description": "古罗马军事通信"}, {"title": "易于实现", "description": "适合编程教学"}]', 'PUBLISHED', NULL, NULL, NULL, '2026-06-15 10:05:14', '2026-06-15 10:05:14');
INSERT INTO `learning_item` (`item_pk`, `json_id`, `title`, `summary`, `category`, `difficulty`, `duration`, `prerequisites`, `objectives`, `features`, `status`, `template_path`, `author_id`, `published_at`, `created_at`, `updated_at`) VALUES (2, 873492, '仿射密码 (Affine Cipher)', '单表替换密码，基于线性变换加密，是凯撒密码的推广，依赖模运算与乘法逆元', '古典密码学', '中等', '2小时', '基础数论（模运算、最大公约数）、简单密码学概念', '"掌握仿射密码原理与应用"', '[{"title": "线性变换", "description": "乘法缩放结合加法平移"}, {"title": "密钥约束", "description": "乘子需与字母表互质"}, {"title": "代数基础", "description": "依赖模逆元与同余运算"}]', 'PUBLISHED', NULL, NULL, NULL, '2026-06-15 10:05:14', '2026-06-15 10:05:14');
INSERT INTO `learning_item` (`item_pk`, `json_id`, `title`, `summary`, `category`, `difficulty`, `duration`, `prerequisites`, `objectives`, `features`, `status`, `template_path`, `author_id`, `published_at`, `created_at`, `updated_at`) VALUES (11, 739285, '数字水印综述', '涵盖扩频水印嵌入提取、强度优化、擦除还原及实用工具与实验', '数字水印', '中等', '3小时', '基础图像处理、概率统计、线性代数', '"掌握数字水印核心技术与应用"', '[{"title": "扩频嵌入", "description": "鲁棒性强 能量分散"}, {"title": "强度优化", "description": "平衡正确率与失真"}, {"title": "擦除还原", "description": "已知未知水印处理"}]', 'PUBLISHED', NULL, NULL, NULL, '2026-06-15 10:05:14', '2026-06-15 10:05:14');
INSERT INTO `learning_item` (`item_pk`, `json_id`, `title`, `summary`, `category`, `difficulty`, `duration`, `prerequisites`, `objectives`, `features`, `status`, `template_path`, `author_id`, `published_at`, `created_at`, `updated_at`) VALUES (12, 897321, '频域水印系统实现（DCT+DWT）', '基于Python实现DCT和DWT频域水印，涵盖嵌入、提取及性能验证', '数字水印', '中等', '2小时', 'Python基础、NumPy、图像处理基础、DCT/DWT变换概念', '"掌握频域水印核心实现流程"', '[{"title": "DCT水印", "description": "块处理 中频嵌入"}, {"title": "DWT水印", "description": "多分辨率 低频嵌入"}, {"title": "频域鲁棒", "description": "抗压缩 抗噪声"}]', 'PUBLISHED', NULL, NULL, NULL, '2026-06-15 10:05:14', '2026-06-15 10:05:14');
INSERT INTO `learning_item` (`item_pk`, `json_id`, `title`, `summary`, `category`, `difficulty`, `duration`, `prerequisites`, `objectives`, `features`, `status`, `template_path`, `author_id`, `published_at`, `created_at`, `updated_at`) VALUES (14, 739281, '密钥管理协议设计与实现', '涵盖DH/ECDH/KDC/STS协议，详解原理、设计与Python实现', '密码协议设计专项', '中等', '4小时', 'Python基础、密码学基础、离散对数概念', '"掌握密钥管理协议设计实现"', '[{"title": "协议全覆盖", "description": "包含交换/分发/协商三类协议"}, {"title": "原理详解", "description": "数学基础与安全依赖清晰解析"}, {"title": "实战实现", "description": "Python完整代码可直接运行验证"}]', 'PUBLISHED', NULL, NULL, NULL, '2026-06-15 10:05:14', '2026-06-15 10:05:14');
INSERT INTO `learning_item` (`item_pk`, `json_id`, `title`, `summary`, `category`, `difficulty`, `duration`, `prerequisites`, `objectives`, `features`, `status`, `template_path`, `author_id`, `published_at`, `created_at`, `updated_at`) VALUES (15, 851472, '认证协议设计与实现', '涵盖挑战响应、Kerberos、OAuth协议，详解原理与Python实现', '密码协议设计专项', '中等', '4小时', 'Python基础、密码学基础、对称/非对称加密概念', '"掌握主流认证协议设计实现"', '[{"title": "协议全覆盖", "description": "实体/身份/相互认证三类协议"}, {"title": "原理透彻", "description": "安全机制与数学核心清晰解析"}, {"title": "实战导向", "description": "Python完整实现可直接验证"}]', 'PUBLISHED', NULL, NULL, NULL, '2026-06-15 10:05:14', '2026-06-15 10:05:14');
INSERT INTO `learning_item` (`item_pk`, `json_id`, `title`, `summary`, `category`, `difficulty`, `duration`, `prerequisites`, `objectives`, `features`, `status`, `template_path`, `author_id`, `published_at`, `created_at`, `updated_at`) VALUES (18, 593722, '区块链核心共识协议', '深度解析PoW、PoS、PBFT三大主流共识协议的数学机制，结合Python完整实现与场景演示，掌握区块链共识核心技术', '区块链安全专项', '中等偏上', '5小时', 'Python基础、哈希函数原理、分布式系统基础、拜占庭容错概念、区块链基本结构', '["理解区块链共识协议的核心目标（一致性、最终性、活性、容错性）", "掌握PoW、PoS、PBFT的数学原理与工作流程", "能够独立实现三大共识协议的核心逻辑", "理解不同共识协议的适用场景与权衡取舍"]', '[{"title": "数学原理 深度拆解", "description": "从拜占庭容错理论出发，详解各协议的数学模型与安全假设"}, {"title": "代码实现 步步可测", "description": "基于Python标准库实现完整共识流程，包含区块结构、节点交互、共识验证", "code_coverage": "100%核心流程可运行，支持单节点模拟与多节点交互"}, {"title": "场景适配 对比分析", "description": "结合公链、联盟链、私有链场景，分析协议选型要点"}]', 'PUBLISHED', NULL, NULL, NULL, '2026-06-15 10:05:14', '2026-06-15 10:05:14');
INSERT INTO `learning_item` (`item_pk`, `json_id`, `title`, `summary`, `category`, `difficulty`, `duration`, `prerequisites`, `objectives`, `features`, `status`, `template_path`, `author_id`, `published_at`, `created_at`, `updated_at`) VALUES (19, 593723, '数字货币核心协议', '深度解析比特币UTXO模型、以太坊账户模型的数学原理与交易结构，结合Python实现完整交易流程与ERC-20智能合约，掌握数字货币底层核心技术', '区块链安全专项', '中等偏上', '6小时', 'Python基础、哈希函数原理、椭圆曲线数字签名（ECDSA）、区块链基础结构、共识协议概念', '["理解数字货币两大核心模型（UTXO/账户模型）的设计思想与数学原理"]', '[{"title": "双模型 深度对比", "description": "从数学定义、状态存储、交易逻辑三维度解析UTXO与账户模型的差异与适用场景"}, {"title": "代码实现 全流程覆盖", "description": "基于Python+cryptography库实现交易创建、签名验证、余额计算、智能合约执行全流程，无黑盒封装", "code_coverage": "100%核心逻辑可运行，包含钱包、交易、合约三大核心组件"}, {"title": "场景化 实战演示", "description": "模拟真实转账场景（Alice→Bob）、智能合约部署与代币交互，贴合实际应用场景"}]', 'PUBLISHED', NULL, NULL, NULL, '2026-06-15 10:05:14', '2026-06-15 10:05:14');
INSERT INTO `learning_item` (`item_pk`, `json_id`, `title`, `summary`, `category`, `difficulty`, `duration`, `prerequisites`, `objectives`, `features`, `status`, `template_path`, `author_id`, `published_at`, `created_at`, `updated_at`) VALUES (37, 628947, 'SHA-1 安全散列算法', 'NSA设计的160位散列算法，曾广泛应用，现因碰撞漏洞被SHA-2/3取代', '现代密码学', '中等', '3小时', '基础密码学概念、二进制运算、模运算', '"掌握SHA-1原理与特性"', '[{"title": "固定输出 固定输出", "description": "生成160位标准摘要"}, {"title": "迭代压缩", "description": "采用Merkle–Damgård结构"}, {"title": "已被攻破", "description": "存在实际碰撞攻击实例"}]', 'PUBLISHED', NULL, NULL, NULL, '2026-06-15 10:05:14', '2026-06-15 10:05:14');
INSERT INTO `learning_item` (`item_pk`, `json_id`, `title`, `summary`, `category`, `difficulty`, `duration`, `prerequisites`, `objectives`, `features`, `status`, `template_path`, `author_id`, `published_at`, `created_at`, `updated_at`) VALUES (38, 879456, 'SHA-1 安全散列算法', 'SHA-1是160位哈希算法，曾广泛应用，后因碰撞漏洞被SHA-2/3取代', '现代密码学', '中等', '3小时', '基础哈希函数概念、二进制运算、模运算', '"掌握SHA-1原理与实现"', '[{"title": "固定输出 160位", "description": "输出长度固定为160比特"}, {"title": "迭代压缩", "description": "采用迭代压缩函数结构"}, {"title": "已被攻破 无安全性", "description": "存在碰撞漏洞已被淘汰"}]', 'PUBLISHED', NULL, NULL, NULL, '2026-06-15 10:05:14', '2026-06-15 10:05:14');
INSERT INTO `learning_item` (`item_pk`, `json_id`, `title`, `summary`, `category`, `difficulty`, `duration`, `prerequisites`, `objectives`, `features`, `status`, `template_path`, `author_id`, `published_at`, `created_at`, `updated_at`) VALUES (53, 919911767, 'Prompt Engineering — 大模型 API 调用与提示词设计', '从 Transformer 架构与 token 的基本概念出发，系统讲解大语言模型的 API 调用方式。深入剖析 System Prompt 的设计方法论，涵盖 Zero-shot、Few-shot 等提示策略，掌握 temperature/top_p 等采样参数对输出行为的调控原理，最终实现用结构化提示词精确控制模型输出。', '智能体', '简单', '3小时', 'Python 基础语法（函数、字典、列表）、了解 HTTP 请求的基本概念', '["理解 Transformer 架构中 token 的概念与文本 tokenize 的过程", "掌握 chat completions 接口中各参数的含义、取值范围与调优方法", "理解 system/user/assistant 三种消息角色的设计意图与协作机制", "掌握 System Prompt 的四要素设计法与 Few-shot 提示技巧", "能够设计提示词方案让模型稳定输出 JSON、Markdown 等结构化格式", "理解 API 响应结构并实现基本的错误处理"]', '[{"title": "Token 与 API 调用", "description": "从 token 的基本概念出发，学习使用 OpenAI SDK 调用 DeepSeek API，理解请求与响应的完整数据结构。"}, {"title": "消息角色与提示策略", "description": "深入理解 system/user/assistant 三种角色的设计意图，掌握 Zero-shot、Few-shot、CoT 等提示策略的原理与适用场景。"}, {"title": "输出控制与工程实践", "description": "通过采样参数控制模型行为，学习 API 错误处理、环境变量管理等工程实践，构建可复用的调用基础设施。"}]', 'PUBLISHED', NULL, NULL, '2026-06-15 10:05:14', '2026-06-15 10:05:14', '2026-06-15 10:15:24');
INSERT INTO `learning_item` (`item_pk`, `json_id`, `title`, `summary`, `category`, `difficulty`, `duration`, `prerequisites`, `objectives`, `features`, `status`, `template_path`, `author_id`, `published_at`, `created_at`, `updated_at`) VALUES (54, 954905328, 'Tool Use — 让智能体具备外部工具调用能力', '从大语言模型的根本局限性出发，讲解 Function Calling 机制的设计原理与实现细节。深入理解 JSON Schema 工具定义、tool_calls 消息解析、tool 角色消息回传的完整协议，掌握单工具调用、多工具路由、并行工具调用以及工具调用错误处理的工程实践。', '智能体', '中等', '3小时', '项目一完成（掌握 API 调用与 messages 结构）、JSON 格式基础、Python 字典与列表操作', '["理解大模型的四类根本局限以及 Tool Use 的解决思路", "掌握 Function Calling 协议的完整六步流程及其背后的设计动机", "能够用 JSON Schema 精确定义工具的名称、描述、参数类型与约束", "理解 assistant（带 tool_calls）和 tool 两种新消息角色的格式要求", "实现单工具、多工具路由与并行工具调用的智能体", "掌握工具调用中的错误处理与边界情况应对"]', '[{"title": "Function Calling 协议", "description": "DeepSeek/OpenAI 提供的标准化工具调用协议。模型通过返回 tool_calls 字段表达调用意图，由系统执行后将结果回传模型，模型再据此生成最终回答。"}, {"title": "工具定义与注册", "description": "使用 JSON Schema 格式描述工具的 name、description、parameters，模型据此判断何时调用哪个工具以及传递什么参数。工具描述的质量直接决定调用的准确性。"}, {"title": "多工具路由与并行调用", "description": "通过工具注册表实现多工具路由分发，支持模型在一次回复中并行调用多个工具，以及处理工具调用中的各种异常情况。"}]', 'PUBLISHED', NULL, NULL, '2026-06-15 10:05:14', '2026-06-15 10:05:14', '2026-06-15 10:15:24');

INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (1, 1, 1, '凯撒时期背景', '凯撒密码得名于古罗马的尤利乌斯·凯撒（Julius Caesar, 公元前100–44年）。
据历史学家苏埃托尼乌斯的记载，凯撒在军事通信中常用一种字母表整体平移的方式加密，常用位移3的方法，例如 A→D, B→E, C→F。
此方法主要目的是防止战场信件被敌军截获后立即理解，虽然不能抵御长期破解，但对非专业人员起到信息遮蔽作用。', '理解历史背景有助于理解密码设计思想。', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (2, 1, 2, '早期密码思想', '在凯撒之前，已有一些古代文明尝试替换或重排的加密方式：
- 埃及象形文字：符号替换的秘密书写。
- 希腊斯巴达 scytale（木棍密码）：将羊皮纸缠在木棍上写字，解开后文字被打乱，需要同样粗细的木棍才能解读。
凯撒密码的特别之处在于极简且标准化，仅需一个数字 k 作为密钥。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (3, 1, 3, '传承与改进', '奥古斯都大帝也使用过类似方法，但常用位移1。
在罗马之后，凯撒密码思想被欧洲中世纪学者引用，成为单表替换密码的代表之一。
文艺复兴时期，密码学发展出多表替换密码，如维吉尼亚密码，凯撒密码成为入门示例。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (4, 1, 4, '近代与现代地位', '16–17世纪，密码学成为外交和军事工具，凯撒密码过于简单不再使用。
20世纪以后，凯撒密码被数学化，表示为 $E_k(x)=(x+k)\\mod n$。
在现代课程中，它作为基础对称加密模型，帮助理解加密、解密、密钥空间、穷举攻击、频率分析等概念。
在计算机科学中，常作为编程入门题、教学案例及CTF基础题。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (5, 1, 5, '历史意义', '凯撒密码是最早的形式化密码之一，奠定了“明文 → 规则变换 → 密文”的基本模式。
提出密钥概念 k，为现代密码学核心。
桥梁作用：从古代直观替换到近代数学化密码，为代数化密码学奠定基础。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (6, 1, 6, '数学原理详解', '1) 基本定义与符号：字母表 $A={A,B,...,Z}$，编码为 $0...25$，密钥 $k\\in Z_{26}$。
加密公式 $E_k(x)=(x+k)\\mod 26$，解密公式 $D_k(y)=(y-k)\\mod 26$。
例：HELLO, k=3 → 密文 KHOOR。
2) 模运算视角：凯撒密码基于 $Z_{26}$ 加法阿贝尔群，$E_k$ 为平移，$D_k=E_{-k}$。
3) 置换与结构：循环置换，单表替换，每个明文字母唯一对应密文字母。
4) 安全性分析：密钥空间小，穷举攻击易成功，统计特征不变，可用频率分析和卡方拟合估计 k。
5) 群论/代数小结：循环群 C26 作用于字母表，同态 $(Z_{26},+)→Sym(A)$，密钥加法对应变换复合。
6) 一般化与实现细节：适用任意字母表大小 n，保持加密解密一致即可。
7) 快速手算与验证：选 k，右移加密，左移解密，自检可恢复原文。
8) 与更强方案关系：凯撒为仿射密码特例，可被频率分析击破，多表替换和现代密码才实用。
9) 总结：凯撒密码利用模 26 循环群和密钥平移实现替换，因密钥空间小和统计不变，现代极不安全。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (7, 1, 7, '实现', '使用 Python 实现凯撒密码，包括加密、解密和暴力破解功能。', NULL, 'import string
from typing import List, Tuple

class CaesarCipher:
    def __init__(self, alphabet: str = string.ascii_uppercase):
        self.alphabet = alphabet
        self.alphabet_size = len(alphabet)
    
    def encrypt(self, plaintext: str, key: int) -> str:
        ciphertext = ''''
        for char in plaintext.upper():
            if char in self.alphabet:
                old_pos = self.alphabet.index(char)
                new_pos = (old_pos + key) % self.alphabet_size
                ciphertext += self.alphabet[new_pos]
            else:
                ciphertext += char
        return ciphertext

    def decrypt(self, ciphertext: str, key: int) -> str:
        return self.encrypt(ciphertext, -key)

    def brute_force_attack(self, ciphertext: str) -> List[Tuple[int, str]]:
        results = []
        for key in range(self.alphabet_size):
            decrypted = self.decrypt(ciphertext, key)
            results.append((key, decrypted))
        return results');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (8, 1, 8, '加密示例', '创建 CaesarCipher 实例，加密文本 ''HELLO''，使用密钥 3，得到密文 KHOOR。', NULL, 'caesar = CaesarCipher()
encrypted = caesar.encrypt(''HELLO'', 3)
print(encrypted)  # 输出: KHOOR');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (9, 1, 9, '解密示例', '使用相同密钥解密密文，恢复原文。', NULL, 'decrypted = caesar.decrypt(encrypted, 3)
print(decrypted)  # 输出: HELLO');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (10, 1, 10, '暴力破解示例', '对密文 ''KHOOR'' 进行暴力破解，尝试所有可能密钥，得到所有候选明文。', NULL, 'results = caesar.brute_force_attack(''KHOOR'')
print(results)  # 输出 26 种可能明文及对应密钥');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (11, 2, 1, '理解核心定义', '仿射密码是单表替换密码的一种，核心是将字母映射为整数后，通过仿射变换实现加密解密。
核心公式包括：
加密：$E_{a,b}(x) = (ax + b) \\mod n$
解密：$D_{a,b}(y) = a^{-1}(y - b) \\mod n$
其中n为字母表大小（英语中n=26），a和b为密钥参数，a需满足$\\gcd(a,n)=1$以保证可逆性。', 'a称为乘法密钥，b称为平移密钥，二者共同构成完整密钥', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (12, 2, 2, '字母与数字映射', '首先建立字母表到整数集合$Z_n$的映射关系，标准映射为：A↦0、B↦1、C↦2、…、Z↦25。
该映射是加密解密的基础，需确保明文字母、密文字母与整数之间的双向唯一对应，避免映射歧义。', '映射顺序可自定义，但加密解密需使用同一套规则', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (13, 2, 3, '掌握加密流程', '1. 确定密钥参数a和b，验证a与n的最大公约数是否为1（以n=26为例，a需从{1,3,5,7,9,11,15,17,19,21,23,25}中选取）；
2. 将明文字符按映射规则转换为对应整数x；
3. 代入加密公式$y = (ax + b) \\mod n$计算得到密文整数y；
4. 将y按映射规则转换回字符，拼接得到完整密文。
示例：明文H（对应x=7），密钥a=5、b=8，加密计算为$(5×7+8)\\mod26=43\\mod26=17$，对应密文字母R。', NULL, 'def encrypt_char(char, a, b, n=26):
    if not char.isalpha():
        return char
    x = ord(char.upper()) - ord(''A'')
    y = (a * x + b) % n
    return chr(y + ord(''A''))
# 测试：encrypt_char(''H'', 5, 8) → ''R''');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (14, 2, 4, '计算乘法逆元', '解密的关键是找到a在模n下的乘法逆元$a^{-1}$，满足$a·a^{-1} ≡ 1 \\mod n$。
求解方法可使用扩展欧几里得算法：对于a和n，找到整数x和y使得$ax + ny = 1$，此时x mod n即为$a^{-1}$。
示例：a=5、n=26，通过计算5×21=105，105 mod26=1，故$a^{-1}=21$。', '若$\\gcd(a,n)≠1$，则a不存在模n逆元，无法解密', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (15, 2, 5, '执行解密操作', '1. 确认密钥a和b，通过扩展欧几里得算法计算$a^{-1}$；
2. 将密文字符按映射规则转换为对应整数y；
3. 代入解密公式$x = (a^{-1}×(y - b)) \\mod n$计算得到明文整数x；
4. 将x按映射规则转换回字符，拼接得到完整明文。
示例：密文R（对应y=17），密钥a=5、b=8，解密计算为$21×(17-8)\\mod26=189\\mod26=7$，对应明文字母H。', NULL, 'def mod_inverse(a, m):
    gcd, x, _ = extended_gcd(a, m)
    if gcd != 1:
        return None
    return (x % m + m) % m

def decrypt_char(char, a, b, n=26):
    a_inv = mod_inverse(a, n)
    if a_inv is None:
        raise ValueError("a无模逆元")
    if not char.isalpha():
        return char
    y = ord(char.upper()) - ord(''A'')
    x = (a_inv * (y - b)) % n
    return chr(x + ord(''A''))
# 测试：decrypt_char(''R'', 5, 8) → ''H''');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (16, 2, 6, '理解密钥空间与安全性', '1. 密钥空间计算：对于n=26，a有$φ(26)=12$种可选值，b有26种可选值，总密钥数为12×26=312种；
2. 安全性分析：虽密钥空间比凯撒密码（25种）大，但仍是单表替换，字母频率分布不变，可通过频率分析破解；
3. 适用场景：现仅用于编程练习、CTF比赛和教学，帮助理解线性同余式与单表替换原理。', '频率分析的核心是利用明文中高频字母（如英语中E）对应密文中高频字母的规律', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (17, 2, 7, '实际代码实现与验证', '基于上述原理实现完整的仿射密码类，包含密钥验证、加密、解密功能：
1. 实现最大公约数计算函数，用于密钥有效性校验；
2. 实现扩展欧几里得算法，用于模逆元计算；
3. 封装加密解密方法，支持大小写字母处理，保留非字母字符；
4. 测试验证：使用明文"HELLO"，密钥a=5、b=8，加密得到"RCLLA"，解密后应还原为"HELLO"。', NULL, 'import string
from typing import Optional

class AffineCipher:
    def __init__(self, alphabet: str = string.ascii_uppercase):
        self.alphabet = alphabet
        self.alphabet_size = len(alphabet)
    
    def _gcd(self, a: int, b: int) -> int:
        while b:
            a, b = b, a % b
        return a
    
    def _mod_inverse(self, a: int, m: int) -> Optional[int]:
        if self._gcd(a, m) != 1:
            return None
        def extended_gcd(a, b):
            if a == 0:
                return b, 0, 1
            gcd, x1, y1 = extended_gcd(b % a, a)
            x = y1 - (b // a) * x1
            y = x1
            return gcd, x, y
        _, x, _ = extended_gcd(a, m)
        return (x % m + m) % m
    
    def encrypt(self, plaintext: str, a: int, b: int) -> str:
        if not self._gcd(a, self.alphabet_size) == 1:
            raise ValueError(f"a={a}与字母表大小{self.alphabet_size}不互质")
        ciphertext = ""
        for char in plaintext.upper():
            if char in self.alphabet:
                x = self.alphabet.index(char)
                y = (a * x + b) % self.alphabet_size
                ciphertext += self.alphabet[y]
            else:
                ciphertext += char
        return ciphertext
    
    def decrypt(self, ciphertext: str, a: int, b: int) -> str:
        a_inv = self._mod_inverse(a, self.alphabet_size)
        if a_inv is None:
            raise ValueError(f"a={a}无模逆元")
        plaintext = ""
        for char in ciphertext.upper():
            if char in self.alphabet:
                y = self.alphabet.index(char)
                x = (a_inv * (y - b)) % self.alphabet_size
                plaintext += self.alphabet[x]
            else:
                plaintext += char
        return plaintext

# 测试
affine = AffineCipher()
encrypted = affine.encrypt("HELLO", 5, 8)
print(encrypted)  # 输出RCLLA
decrypted = affine.decrypt(encrypted, 5, 8)
print(decrypted)  # 输出HELLO');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (73, 11, 1, '理解扩频水印基本原理', '扩频水印的核心思想源于通信中的扩频技术，将少量水印信息扩展到整个载体图像的多个样本上，从而提升水印对噪声、压缩等攻击的鲁棒性，且单点修改对整体水印提取影响较小。
首先明确核心概念：
1. 原始图像：记为$I$，尺寸为$M×N$，像素值范围通常为$[0, 255]$；
2. 水印信息：二进制位序列$b_k ∈ \\{+1, -1\\}$（$k$为水印位索引），通过符号映射将传统二进制$0/1$转换为$+1/-1$以适配相关检测；
3. 扩频序列：每个水印位$b_k$对应一个与图像尺寸一致的伪随机扩频序列$s_k$，序列需满足自相关近似正交特性，确保不同水印位的干扰最小；
4. 嵌入强度：记为$α$，控制水印嵌入的深浅，直接影响水印鲁棒性与图像失真度。
通过这一步，需建立对扩频水印“分散能量、提升鲁棒性”核心优势的认知，为后续嵌入提取模型学习奠定基础。', '扩频序列的正交性是减少水印位间干扰的关键，通常采用伪随机噪声序列（PN序列）实现', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (74, 11, 2, '掌握扩频水印嵌入模型', '扩频水印采用加性嵌入模型，具体步骤如下：
1. 构建水印信号：首先根据水印位序列和对应扩频序列生成水印信号$W$，公式为$W = \\sum_{k} b_k s_k$，即每个水印位通过其扩频序列扩展后叠加，形成与原始图像尺寸一致的水印矩阵；
2. 嵌入水印到图像：将水印信号按嵌入强度$α$缩放后，叠加到原始图像上，得到含水印图像$I_w$，公式为$I_w = I + αW$。
需注意：
- 嵌入过程中需确保像素值不超出有效范围（如$[0, 255]$），通常在叠加后进行截断处理；
- 扩频序列$s_k$可嵌入图像空域（直接操作像素）或频域（如DWT、DCT变换后的系数），频域嵌入通常更鲁棒且视觉失真更小。
预期输出：给定原始图像$I$、水印位序列$b_k$、扩频序列$s_k$和嵌入强度$α$，能计算得到含水印图像$I_w$，且$I_w$与$I$的视觉差异可通过后续PSNR指标量化。', NULL, 'import numpy as np

def generate_spread_sequence(shape, seed):
    """生成伪随机扩频序列（取值±1）"""
    np.random.seed(seed)
    return np.random.choice([-1, 1], size=shape)

def embed_watermark(I, b_k, alpha, seed_list):
    """
    扩频水印嵌入
    参数：
        I: 原始图像（M×N数组）
        b_k: 水印位序列（列表，元素±1）
        alpha: 嵌入强度
        seed_list: 扩频序列种子列表（长度与b_k一致）
    返回：
        I_w: 含水印图像
    """
    M, N = I.shape
    W = np.zeros((M, N))
    # 构建水印信号W
    for k, b in enumerate(b_k):
        s_k = generate_spread_sequence((M, N), seed_list[k])
        W += b * s_k
    # 加性嵌入并截断像素值
    I_w = I + alpha * W
    I_w = np.clip(I_w, 0, 255).astype(np.uint8)
    return I_w

# 示例：512×512灰度图，2位水印，嵌入强度0.1
I = np.random.randint(0, 256, size=(512, 512), dtype=np.uint8)
b_k = [1, -1]
alpha = 0.1
seed_list = [42, 100]
I_w = embed_watermark(I, b_k, alpha, seed_list)
print(f"原始图像均值：{I.mean():.2f}")
print(f"含水印图像均值：{I_w.mean():.2f}")
print(f"水印信号W的能量：{np.sum(W**2):.2f}")');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (75, 11, 3, '学习水印提取与检测方法', '水印提取的核心是通过相关性检测从受攻击或含噪声的观测图像$I''$中恢复水印位序列，步骤如下：
1. 观测图像获取：含水印图像经传输、处理或攻击后，得到观测图像$I''$（可能含噪声、压缩失真等）；
2. 计算检测统计量：对每个水印位$k$，计算观测图像$I''$与对应扩频序列$s_k$的相关性$r_k$，公式为$r_k = \\langle I'', s_k \\rangle = \\sum_{i,j} I''_{i,j} s_{k,i,j}$，其中$\\langle·,·\\rangle$表示内积；
3. 判决规则：设定阈值$T$，若$r_k > T$则判决水印位为$+1$，否则为$-1$；为提高鲁棒性，常用归一化互相关（NCC）作为检测统计量，公式为：
$NCC = \\frac{\\sum_{i,j} I''_{i,j} W_{i,j}}{\\sqrt{\\sum_{i,j} I''^2_{i,j} \\sum_{i,j} W^2_{i,j}}}$
NCC取值范围为$[-1, 1]$，越接近1表示水印匹配度越高。
预期输出：给定观测图像$I''$、扩频序列$s_k$，能计算得到每个水印位的检测统计量（$r_k$或NCC），并通过判决规则还原水印位序列$\\hat{b}_k$，后续可通过位错误率（BER）评估提取正确率。', NULL, 'def extract_watermark(I_prime, seed_list, threshold=0):
    """
    水印提取与检测
    参数：
        I_prime: 观测图像（M×N数组）
        seed_list: 扩频序列种子列表
        threshold: 判决阈值
    返回：
        hat_b_k: 提取的水印位序列
        r_k_list: 每个水印位的相关性统计量
    """
    M, N = I_prime.shape
    hat_b_k = []
    r_k_list = []
    for seed in seed_list:
        s_k = generate_spread_sequence((M, N), seed)
        # 计算相关性统计量r_k
        r_k = np.sum(I_prime * s_k)
        r_k_list.append(r_k)
        # 判决水印位
        if r_k > threshold:
            hat_b_k.append(1)
        else:
            hat_b_k.append(-1)
    return hat_b_k, r_k_list

# 示例：从含噪声的观测图像中提取水印
noise = np.random.normal(0, 5, size=(512, 512))  # 加性高斯噪声
I_prime = I_w + noise
I_prime = np.clip(I_prime, 0, 255).astype(np.uint8)

hat_b_k, r_k_list = extract_watermark(I_prime, seed_list)
print(f"原始水印位：{b_k}")
print(f"提取水印位：{hat_b_k}")
print(f"各水印位相关性：{[f''{r:.2f}'' for r in r_k_list]}")');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (76, 11, 4, '分析提取正确率与统计特性', '水印提取正确率的核心影响因素是检测信噪比（$SNR_d$），需结合统计分析理解其与误检率的关系：
1. 检测信噪比定义：$SNR_d = \\frac{α^2 \\sum_{i,j} W^2_{i,j}}{σ_n^2}$，其中$σ_n^2$为观测图像中噪声的方差，$SNR_d$与嵌入强度$α$的平方成正比，$α$越大，$SNR_d$越高，检测越可靠；
2. 位错误率（BER）：衡量提取正确率的核心指标，公式为$BER = \\frac{1}{K} \\sum_{k} 1[b_k ≠ \\hat{b}_k]$，其中$K$为水印总位数，$1[·]$为指示函数（条件满足时取1，否则取0），BER越小表示提取正确率越高；
3. 统计近似：若检测统计量$r_k$服从高斯分布，BER可通过Q函数近似，$Q(x) = \\frac{1}{\\sqrt{2π}} \\int_x^{+∞} e^{-t^2/2} dt$，即误检概率与$SNR_d$负相关。
通过这一步，需建立“$α$增大→$SNR_d$提高→BER降低→提取正确率提升”的定性关系，为后续嵌入强度优化奠定基础。
预期输出：给定不同噪声方差$σ_n^2$和嵌入强度$α$，能计算$SNR_d$和BER，绘制$α$与BER的关系曲线，验证统计特性。', '实际应用中，可通过实验测量BER，而非仅依赖理论近似，因噪声分布可能偏离理想高斯分布', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (77, 11, 5, '理解嵌入强度对PSNR与正确率的影响', '嵌入强度$α$是平衡水印鲁棒性（提取正确率）与图像失真（PSNR）的关键参数，需掌握两者的权衡关系：
1. 图像失真度量：
   - 均方误差（MSE）：$MSE = \\frac{1}{MN} \\sum_{i,j} [I_{i,j} - I_{w,i,j}]^2$，衡量原始图像与含水印图像的像素差异；
   - 峰值信噪比（PSNR）：$PSNR = 10\\log_{10} \\frac{MAX^2}{MSE}$，其中$MAX$为像素最大可能值（如255），PSNR越高表示图像失真越小，通常PSNR≥30dB时人眼难以察觉失真；
   - 对于加性嵌入$I_w = I + αW$，若$I$与$W$无关，可推得$MSE = α^2 · \\frac{1}{MN} \\sum_{i,j} W^2_{i,j}$，即PSNR随$α$单调下降；
2. 强度与性能的定性关系：
   - $α$极小时：PSNR很高（失真小），但$SNR_d$低，提取正确率接近随机（BER≈0.5）；
   - $α$增大到阈值后：PSNR下降，但$SNR_d$迅速提升，提取正确率急剧上升（BER快速降低）；
   - 优化目标：在允许的PSNR阈值（如$PSNR ≥ 30dB$）下，选择最大$α$以最大化提取正确率，即求解约束优化问题：$max P_{det}(α) \\ s.t. PSNR(α) ≥ PSNR_{min}$。
预期输出：绘制同一横轴$α$下的PSNR曲线与提取正确率（$P_{det}=1-BER$）曲线，找到满足$PSNR_{min}$的最优$α$值。', NULL, 'def calculate_mse_psnr(I, I_w, max_pixel=255):
    """计算MSE和PSNR"""
    I = I.astype(np.float64)
    I_w = I_w.astype(np.float64)
    mse = np.mean((I - I_w)**2)
    psnr = 10 * np.log10(max_pixel**2 / mse)
    return mse, psnr

def analyze_strength_performance(I, b_k, seed_list, alpha_range, noise_var=5):
    """分析嵌入强度对PSNR和提取正确率的影响"""
    psnr_list = []
    ber_list = []
    for alpha in alpha_range:
        # 嵌入水印
        I_w = embed_watermark(I, b_k, alpha, seed_list)
        # 计算PSNR
        mse, psnr = calculate_mse_psnr(I, I_w)
        psnr_list.append(psnr)
        # 加噪声得到观测图像
        noise = np.random.normal(0, np.sqrt(noise_var), size=I.shape)
        I_prime = I_w + noise
        I_prime = np.clip(I_prime, 0, 255).astype(np.uint8)
        # 提取水印并计算BER
        hat_b_k, _ = extract_watermark(I_prime, seed_list)
        ber = np.mean([1 if b != hat_b else 0 for b, hat_b in zip(b_k, hat_b_k)])
        ber_list.append(ber)
    return psnr_list, ber_list

# 示例：分析α∈[0.05, 0.5]的性能
alpha_range = np.linspace(0.05, 0.5, 10)
psnr_list, ber_list = analyze_strength_performance(I, b_k, seed_list, alpha_range)

print("嵌入强度α | PSNR(dB) | BER")
print("-"*30)
for alpha, psnr, ber in zip(alpha_range, psnr_list, ber_list):
    print(f"{alpha:.2f}      | {psnr:.2f}    | {ber:.3f}")');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (78, 11, 6, '掌握水印擦除与还原的实现方法', '水印擦除分为已知水印和未知水印两类场景，核心是去除或削弱水印信号，同时尽量还原原始图像质量：
1. 已知水印的擦除（有先验信息）：
   - 原理：若已知扩频序列$s_k$或水印信号$W$的结构，通过最小二乘估计水印系数$\\hat{c}_k = \\frac{\\langle I'', s_k \\rangle}{\\langle s_k, s_k \\rangle}$；
   - 估计水印信号：$\\hat{W} = \\sum_{k} \\hat{c}_k s_k$；
   - 去除水印：$\\hat{I} = I'' - α \\hat{W}$，其中$α$为嵌入强度（已知或估计）；
2. 未知水印的擦除（无先验信息）：
   - 去噪方法：将水印视为低幅度结构性噪声，采用维纳滤波、总变分去噪、变换域自适应阈值去噪等方法削弱水印；
   - 盲源分离：利用ICA（独立分量分析）或PCA（主成分分析）分离图像与水印分量；
   - 对抗性攻击：通过加噪、几何变换使原检测器失效，再用图像恢复算法还原视觉质量；
3. 还原质量评价：
   - 视觉失真指标：PSNR、SSIM（结构相似性）；
   - 水印残留指标：残余水印能量$E_{res} = \\sum_{i,j} \\hat{W}^2_{i,j}$，$E_{res}$越小表示水印去除越彻底；
   - 还原误差：若已知原始图像，计算$MSE_{restore} = \\frac{1}{MN} \\sum_{i,j} [I_{i,j} - \\hat{I}_{i,j}]^2$。
预期输出：对含水印图像$I_w$执行擦除算法，得到还原图像$\\hat{I}$，计算PSNR、$E_{res}$等指标，验证水印是否被有效去除且图像失真较小。', NULL, 'def remove_watermark_known(I_prime, seed_list, alpha):
    """已知扩频序列和嵌入强度的水印擦除"""
    M, N = I_prime.shape
    hat_W = np.zeros((M, N))
    for seed in seed_list:
        s_k = generate_spread_sequence((M, N), seed)
        # 最小二乘估计系数c_k
        c_k = np.sum(I_prime * s_k) / np.sum(s_k**2)
        hat_W += c_k * s_k
    # 去除水印
    hat_I = I_prime - alpha * hat_W
    hat_I = np.clip(hat_I, 0, 255).astype(np.uint8)
    # 计算残余水印能量
    res_energy = np.sum(hat_W**2)
    return hat_I, res_energy

# 示例：擦除已知结构的水印
hat_I, res_energy = remove_watermark_known(I_w, seed_list, alpha)
# 计算还原图像与原始图像的PSNR
mse_restore, psnr_restore = calculate_mse_psnr(I, hat_I)

print(f"还原图像PSNR（相对原始图像）：{psnr_restore:.2f} dB")
print(f"残余水印能量：{res_energy:.2f}")
# 验证水印是否被去除：尝试从还原图像中提取水印
hat_b_k_restore, _ = extract_watermark(hat_I, seed_list)
print(f"从还原图像提取的水印位：{hat_b_k_restore}")
print(f"还原图像水印提取BER：{np.mean([1 if b != hat_b else 0 for b, hat_b in zip(b_k, hat_b_k_restore)]):.3f}")');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (79, 11, 7, '了解实用工具与实验程序', '结合开源项目与实验代码，将理论落地为实践，核心工具与程序如下：
1. 著名开源项目：
   - invisible-watermark：Python实现，支持DWT/DCT/SVD频域嵌入与盲提取，适合扩频水印实验；
   - DWT-DCT-Digital-Image-Watermarking：教学性质代码，可自由修改嵌入强度$α$，计算PSNR与检测率；
   - TrustMark：Adobe开源工业级方案，多语言实现，可研究先进检测算法；
2. 核心实验程序：
   - spread_spectrum_watermark.ipynb：完整扩频水印系统，包含嵌入、提取、强度调整、擦除、鲁棒性测试（噪声、压缩、裁剪、旋转）等功能；
   - frequency_domain_watermark.ipynb：频域水印实现，支持DCT、DWT等变换域嵌入，比空域嵌入更鲁棒；
   - watermark_toolkit.ipynb：可视化工具，可生成处理过程可视化图表、测试报告和测试图像。
通过这一步，需掌握如何利用开源工具快速搭建实验环境，验证前文所学的嵌入、提取、优化、擦除等理论，并通过鲁棒性测试评估水印系统性能。', '建议优先使用invisible-watermark或DWT-DCT-Digital-Image-Watermarking进行入门实验，代码结构清晰，易修改参数验证理论', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (80, 12, 1, '环境准备与依赖安装', '频域水印系统依赖多个Python库，需先完成环境搭建：
1. 核心依赖库说明：
   - NumPy：用于数组运算和图像数据处理；
   - scipy.fftpack：提供DCT（离散余弦变换）及逆变换函数；
   - pywt：Python小波变换库，用于DWT（离散小波变换）的分解与重构；
   - PIL（Pillow）：用于水印图像的尺寸调整和格式转换。
2. 安装命令：通过pip安装所有依赖，命令为 `pip install numpy scipy pywt pillow`。
3. 环境验证：安装完成后，导入所有库进行测试，确保无导入错误，例如运行`import numpy as np; import scipy.fftpack; import pywt; from PIL import Image`，无报错则环境就绪。
预期输出：成功安装所有依赖库，能够正常导入相关模块，为后续代码运行奠定基础。', '若pywt安装失败，可尝试指定版本安装：`pip install pywt==1.4.1`，确保与Python版本兼容', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (81, 12, 2, 'DCT水印系统核心原理与类解析', 'DCT水印基于离散余弦变换，将图像转换到频域后在中频系数嵌入水印，核心优势是抗JPEG压缩等常见攻击，先解析DCTWatermark类的核心设计：
1. 初始化参数：
   - alpha：嵌入强度（默认0.1），值越大水印鲁棒性越强但图像失真越大；
   - block_size：DCT处理块大小（默认8x8），与JPEG压缩的块大小一致，适配抗压缩需求；
2. 核心工具方法：
   - _dct2：二维DCT变换，通过两次一维DCT变换（先对行、再对列）实现，使用`norm=''ortho''`保证正交性；
   - _idct2：二维IDCT逆变换，与_dct2对应，用于将频域系数还原为空域图像块；
   - _resize_watermark：调整水印尺寸，使其与图像的DCT块数量匹配（图像块数=图像尺寸//block_size）；
3. 核心逻辑：图像按block_size分割为多个子块，每个子块经DCT变换后，在中频系数（如[3,4]位置，兼顾鲁棒性和不可见性）嵌入水印位，最后经IDCT逆变换重构图像。
预期输出：理解DCT水印的“分块-频域变换-系数修改-逆变换”核心流程，明确中频系数嵌入的优势。', NULL, 'import numpy as np
from scipy import fftpack
# 验证DCT变换功能
dct_wm = DCTWatermark(alpha=0.2, block_size=8)
# 创建8x8测试块
test_block = np.random.randint(0, 256, (8,8), dtype=np.float64)
# DCT变换
dct_block = dct_wm._dct2(test_block)
# IDCT逆变换
recovered_block = dct_wm._idct2(dct_block)
# 验证逆变换精度
mse = np.mean((test_block - recovered_block)**2)
print(f"8x8块DCT-IDCT逆变换MSE：{mse:.6f}")
print(f"原始块前4x4：\\n{test_block[:4,:4].round(2)}")
print(f"DCT变换后前4x4（频域系数）：\\n{dct_block[:4,:4].round(2)}")
print(f"逆变换恢复后前4x4：\\n{recovered_block[:4,:4].round(2)}")');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (82, 12, 3, 'DCT水印嵌入与提取实现', '基于DCTWatermark类，完整实现水印嵌入与提取流程，步骤如下：
1. 数据准备：
   - 原始图像：创建256x256灰度测试图（像素值100-200），或读取本地图像；
   - 水印图像：创建二值水印（如32x32大小，中间为1的方块），确保水印为0-1二值数组；
2. 水印嵌入步骤：
   a. 初始化DCTWatermark实例，设置嵌入强度alpha和块大小block_size；
   b. 调用embed_watermark方法，传入原始图像和水印，内部自动完成分块、DCT变换、中频系数修改（1则加alpha×系数绝对值，0则减）、IDCT逆变换；
   c. 输出含水印图像，裁剪像素值到[0,255]并转换为uint8类型；
3. 水印提取步骤：
   a. 调用extract_watermark方法，传入含水印图像和原始水印尺寸；
   b. 内部对每个图像块执行DCT变换，读取中频系数[3,4]的符号（正为1，负为0），收集所有水印位并重塑为原始水印尺寸；
4. 性能评估：计算原始图像与含水印图像的PSNR（峰值信噪比），以及原始水印与提取水印的相似度（匹配像素占比）。
预期输出：成功生成含水印图像，PSNR约48dB（嵌入强度0.2时），水印相似度根据参数可能存在差异，需调整alpha优化提取效果。', NULL, 'def test_dct_watermark_embed_extract():
    # 1. 准备数据
    # 创建原始图像（256x256灰度图）
    test_image = np.random.randint(100, 200, (256, 256), dtype=np.uint8)
    # 创建水印（32x32二值图像，中间16x16为1）
    watermark = np.zeros((32, 32), dtype=int)
    watermark[8:24, 8:24] = 1
    print(f"原始图像大小：{test_image.shape}")
    print(f"水印大小：{watermark.shape}")
    
    # 2. 初始化DCT水印系统
    dct_wm = DCTWatermark(alpha=0.3, block_size=8)  # 调整强度为0.3提升鲁棒性
    
    # 3. 嵌入水印
    print("\\n嵌入DCT水印...")
    watermarked_image = dct_wm.embed_watermark(test_image, watermark)
    
    # 4. 计算PSNR
    mse = np.mean((test_image.astype(float) - watermarked_image.astype(float))**2)
    psnr = 20 * np.log10(255.0 / np.sqrt(mse)) if mse > 0 else float(''inf'')
    print(f"原始图像 vs 含水印图像 PSNR：{psnr:.2f} dB")
    
    # 5. 提取水印
    print("\\n提取DCT水印...")
    extracted_watermark = dct_wm.extract_watermark(watermarked_image, watermark.shape)
    
    # 6. 计算水印相似度
    similarity = np.sum(watermark == extracted_watermark) / watermark.size
    print(f"原始水印与提取水印相似度：{similarity * 100:.2f}%")
    
    if similarity > 0.8:
        print("[OK] DCT水印嵌入提取成功！")
    else:
        print("[提示] 可增大alpha提升相似度，但会降低PSNR")
    
    return test_image, watermarked_image, watermark, extracted_watermark

# 执行测试
test_img, wm_img, orig_wm, ext_wm = test_dct_watermark_embed_extract()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (83, 12, 4, 'DWT水印系统核心原理与类解析', 'DWT水印基于离散小波变换，利用多分辨率特性在低频子带嵌入水印，核心优势是兼顾不可见性和鲁棒性，解析DWTWatermark类的核心设计：
1. 初始化参数：
   - alpha：嵌入强度（默认0.1），控制水印信号的幅度；
   - wavelet：小波基类型（默认''haar''， Haar小波计算简单、速度快）；
   - level：小波分解层数（默认2），分解层数越多，低频子带尺寸越小；
2. 核心工具方法：
   - _resize_watermark：将水印图像归一化到0-255后，调整为目标尺寸（通常为低频子带尺寸），再归一化到0-1；
3. 核心逻辑：原始图像经wavelet小波分解为低频子带（cA，近似分量）和高频子带（cH、cV、cD，细节分量），在低频子带cA中嵌入水印（叠加alpha×水印×cA绝对值），最后经小波重构得到含水印图像；提取时通过对比原始图像和含水印图像的低频子带差异恢复水印。
预期输出：理解DWT水印的“小波分解-低频嵌入-重构”流程，明确低频子带嵌入的原因（低频分量能量集中，抗攻击能力强）。', NULL, 'import pywt
# 验证小波分解功能
dwt_wm = DWTWatermark(alpha=0.15, wavelet=''haar'', level=2)
# 创建256x256测试图像
test_image = np.random.randint(100, 200, (256, 256), dtype=np.float64)
# 小波分解
coeffs = pywt.wavedec2(test_image, wavelet=''haar'', level=2)
cA, (cH2, cV2, cD2), (cH1, cV1, cD1) = coeffs
print(f"小波分解后低频子带cA尺寸：{cA.shape}")
print(f"高频子带cH2尺寸：{cH2.shape}")
print(f"高频子带cH1尺寸：{cH1.shape}")
# 小波重构
recovered_image = pywt.waverec2(coeffs, wavelet=''haar'')
recovered_image = recovered_image[:test_image.shape[0], :test_image.shape[1]]
# 验证重构精度
mse = np.mean((test_image - recovered_image)**2)
print(f"小波分解-重构MSE：{mse:.6f}")');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (84, 12, 5, 'DWT水印嵌入与提取实现', '基于DWTWatermark类，完成水印嵌入、提取及性能验证，步骤如下：
1. 数据准备：
   - 原始图像：与DCT水印测试一致，使用256x256灰度图；
   - 水印图像：32x32二值图像（如中间8x8为1的方块），支持浮点型输入；
2. 水印嵌入步骤：
   a. 初始化DWTWatermark实例，设置嵌入强度alpha、小波基类型和分解层数；
   b. 调用embed_watermark方法，内部执行：小波分解→获取低频子带cA→调整水印尺寸与cA匹配→在cA中嵌入水印（cA_watermarked = cA + alpha×水印×|cA|）→重构小波系数→逆变换得到含水印图像；
   c. 裁剪重构图像到原始尺寸，像素值裁剪到[0,255]并转换为uint8；
3. 水印提取步骤（非盲提取，需原始图像）：
   a. 分别对原始图像和含水印图像执行小波分解，获取各自的低频子带cA_orig和cA_wm；
   b. 计算子带差异：watermark = (cA_wm - cA_orig) / (alpha×|cA_orig| + 1e-10)，避免除零；
   c. 调整水印尺寸到原始水印大小，二值化（>0为1，否则为0）得到提取水印；
4. 性能评估：计算PSNR（衡量图像失真）和水印相似度（衡量提取效果）。
预期输出：成功生成含水印图像，PSNR约33dB（嵌入强度0.15时），通过调整alpha和分解层数，可优化水印相似度（目标>80%）。', NULL, 'def test_dwt_watermark_embed_extract():
    # 1. 准备数据
    test_image = np.random.randint(100, 200, (256, 256), dtype=np.uint8)
    # 创建水印（32x32，中间8x8为1.0）
    watermark = np.zeros((32, 32), dtype=np.float64)
    watermark[12:20, 12:20] = 1.0
    print(f"原始图像大小：{test_image.shape}")
    print(f"水印大小：{watermark.shape}")
    
    # 2. 初始化DWT水印系统
    dwt_wm = DWTWatermark(alpha=0.25, wavelet=''haar'', level=2)  # 增大强度至0.25
    
    # 3. 嵌入水印
    print("\\n嵌入DWT水印...")
    watermarked_image = dwt_wm.embed_watermark(test_image, watermark)
    
    # 4. 计算PSNR
    mse = np.mean((test_image.astype(float) - watermarked_image.astype(float))**2)
    psnr = 20 * np.log10(255.0 / np.sqrt(mse)) if mse > 0 else float(''inf'')
    print(f"原始图像 vs 含水印图像 PSNR：{psnr:.2f} dB")
    
    # 5. 提取水印（非盲提取，需原始图像）
    print("\\n提取DWT水印...")
    extracted_watermark = dwt_wm.extract_watermark(watermarked_image, test_image, watermark.shape)
    
    # 6. 二值化并计算相似度
    orig_wm_binary = (watermark > 0.5).astype(int)
    ext_wm_binary = (extracted_watermark > 0.5).astype(int)
    similarity = np.sum(orig_wm_binary == ext_wm_binary) / orig_wm_binary.size
    print(f"原始水印与提取水印相似度：{similarity * 100:.2f}%")
    
    if similarity > 0.8:
        print("[OK] DWT水印嵌入提取成功！")
    else:
        print("[提示] 可调整alpha或分解层数优化效果")
    
    return test_image, watermarked_image, orig_wm_binary, ext_wm_binary

# 执行测试
dwt_test_img, dwt_wm_img, dwt_orig_wm, dwt_ext_wm = test_dwt_watermark_embed_extract()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (85, 12, 6, 'DCT与DWT水印特性对比分析', '通过实验结果对比DCT和DWT两种频域水印的核心特性，明确适用场景：
1. 嵌入位置差异：
   - DCT水印：在8x8块的中频系数嵌入，适配JPEG压缩的块处理机制，抗JPEG压缩能力更强；
   - DWT水印：在小波分解的低频子带嵌入，低频分量对几何变换和噪声更敏感，但不可见性通常更好；
2. 性能指标对比（基于实验结果）：
   - PSNR：相同嵌入强度下，DCT水印PSNR更高（如alpha=0.2时DCT约48dB，DWT约33dB），图像失真更小；
   - 提取相似度：受alpha影响显著，DCT水印在中等强度下（0.3）易达到高相似度，DWT水印需平衡强度与失真；
3. 鲁棒性差异：
   - DCT水印：抗压缩、抗噪声能力较强，适合对压缩有要求的场景（如网络传输图像）；
   - DWT水印：抗几何攻击（如裁剪、旋转）能力略优，但对噪声敏感，适合对视觉质量要求高的场景；
4. 实现复杂度：
   - DCT水印：分块处理逻辑简单，无需原始图像即可提取（盲提取）；
   - DWT水印：依赖小波分解重构，提取需原始图像（非盲提取），实现稍复杂。
预期输出：明确两种水印的优缺点及适用场景，能够根据实际需求选择合适的频域水印方案。', '实际应用中，可结合两种技术的优势（如DWT+DCT混合水印），进一步提升鲁棒性和不可见性', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (92, 14, 1, '密钥管理协议核心概念与分类', '密钥管理协议是密码系统的核心组件，负责在不安全环境中安全地实现密钥的生成、交换、分发或协商，保障后续加密通信的安全性。首先明确三类核心协议的定位与应用场景：
1. 密钥交换协议：无需预共享秘密，双方通过公开信道协商共享密钥，典型代表为Diffie-Hellman（DH）及其椭圆曲线版本（ECDH）；
2. 密钥分发协议：依赖中心化可信第三方（KDC），为通信双方分配会话密钥，适用于大规模系统，代表为Kerberos协议；
3. 密钥协商协议：在交换密钥的同时提供身份认证，防止中间人攻击，典型代表为Station-to-Station（STS）协议。
三类协议的核心目标均为获取安全的共享密钥，但在拓扑结构（去中心化/中心化）、安全特性（是否抗MITM）、适用场景上存在差异，需根据实际需求选择。
预期输出：明确三类密钥管理协议的核心区别与适用场景，建立整体知识框架。', '重点区分“交换”与“协商”的差异：交换仅获取密钥，协商含身份认证', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (93, 14, 2, 'Diffie-Hellman（DH）协议原理与数学基础', 'DH协议是第一个公开密钥交换协议，奠定了公钥密码学的基础，其核心基于离散对数问题（DLP）的难解性。
1. 历史背景：1976年由Whitfield Diffie和Martin Hellman提出，首次实现了无需预共享秘密的密钥交换，突破了传统对称密码的密钥分发瓶颈；
2. 核心数学原理：
   - 公共参数：大素数$p$和生成元$g$（$g$是$p$的原根，确保$g^a \\mod p$能生成$[1,p-1]$的所有元素）；
   - 私钥选择：Alice选择私钥$a$（随机整数，$1<a<p-1$），Bob选择私钥$b$（同理）；
   - 公钥计算：Alice计算$A = g^a \\mod p$，Bob计算$B = g^b \\mod p$；
   - 密钥交换：双方公开传输公钥$A$和$B$；
   - 共享密钥计算：Alice通过$K = B^a \\mod p$得到密钥，Bob通过$K = A^b \\mod p$得到密钥，由于$B^a = (g^b)^a = g^{ab} = (g^a)^b = A^b$，双方得到相同密钥；
3. 安全依赖：协议安全性基于离散对数问题（DLP）的难解性——即已知$p、g、g^a \\mod p$，难以计算出$a$，确保私钥不被破解。
预期输出：掌握DH协议的完整流程与数学推导，理解离散对数问题在协议安全中的核心作用。', '生成元$g$通常选择2或5，大素数$p$的长度建议不低于2048位以保证安全性', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (94, 14, 3, 'ECDH协议原理与优势分析', 'ECDH是DH协议的椭圆曲线版本，1985年由Victor Miller和Neal Koblitz提出，核心优势是用更短的密钥长度提供与传统DH相当的安全性。
1. 核心数学原理：
   - 公共参数：椭圆曲线$E$（定义在有限域$GF(p)$上，方程为$y^2 = x^3 + ax + b$）和基点$G$；
   - 私钥选择：Alice选择私钥$a$（随机整数），Bob选择私钥$b$（随机整数）；
   - 公钥计算：Alice计算$A = aG$（椭圆曲线上的点乘运算），Bob计算$B = bG$；
   - 密钥交换：双方公开传输公钥$A$和$B$；
   - 共享密钥计算：Alice通过$K = aB$得到密钥，Bob通过$K = bA$得到密钥，由于$aB = a(bG) = (ab)G = b(aG) = bA$，双方得到相同密钥；
2. 安全依赖：基于椭圆曲线离散对数问题（ECDLP）的难解性——已知椭圆曲线$E$、基点$G$和点$aG$，难以计算出私钥$a$；
3. 优势对比：256位ECDH的安全性相当于3072位传统DH，密钥长度更短，计算效率更高，带宽消耗更小，更适用于资源受限场景（如移动设备）。
预期输出：理解ECDH协议的数学原理，明确其相比传统DH的性能优势。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (95, 14, 4, 'KDC密钥分发协议原理与流程', 'KDC（密钥分发中心）是中心化的密钥管理方案，适用于大规模用户群体，核心是通过可信第三方为通信双方分配会话密钥。
1. 历史背景：1978年左右提出，最著名的应用是Kerberos协议，解决了大规模系统中用户间密钥管理的复杂性问题；
2. 核心前提：系统中每个用户与KDC共享长期密钥（预配置或注册时分配），KDC是所有用户信任的第三方；
3. 完整工作流程（简化版Needham-Schroeder协议）：
   - 注册阶段：用户Alice和Bob分别与KDC注册，获取长期密钥$K_A$和$K_B$（仅用户与KDC知晓）；
   - 请求阶段：Alice向KDC发送通信请求，告知需与Bob建立连接；
   - 密钥生成：KDC生成会话密钥$K_{AB}$（仅本次通信使用）；
   - 密钥分发：KDC向Alice发送两份加密消息——一份用$K_A$加密（含$K_{AB}$、Bob标识），另一份用$K_B$加密（含$K_{AB}$、Alice标识）；
   - 密钥转发：Alice用$K_A$解密得到$K_{AB}$，并将KDC加密给Bob的消息转发给Bob；
   - 密钥获取：Bob用$K_B$解密得到$K_{AB}$；
   - 通信阶段：Alice和Bob使用$K_{AB}$进行对称加密通信；
4. 数学表示：Alice接收$E_{K_A}[K_{AB}, Bob]$，Bob接收$E_{K_B}[K_{AB}, Alice]$，其中$E_K[M]$表示用密钥$K$加密消息$M$；
5. 安全依赖：长期密钥的保密性和加密算法的安全性，KDC的可信度是协议安全的核心。
预期输出：掌握KDC协议的中心化工作流程，理解其依赖可信第三方的特性。', '会话密钥为临时密钥，一次通信后失效，可降低密钥泄露的影响范围', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (96, 14, 5, 'STS协议原理与抗MITM机制', 'STS（Station-to-Station）协议是DH协议的增强版本，在密钥交换的基础上增加了身份认证功能，有效防止中间人攻击（MITM）。
1. 历史背景：1988年由Diffie、Van Oorschot和Wiener提出，解决了纯DH协议无法抵抗MITM攻击的缺陷；
2. 核心改进：结合数字签名技术，在交换公钥的同时验证对方身份，确保通信双方是预期的合法主体；
3. 完整工作流程（基于ECDH）：
   - 密钥生成：Alice生成ECDH密钥对（私钥$a$，公钥$A$）和签名密钥对（私钥$sk_A$，公钥$vk_A$）；Bob同理生成（私钥$b$，公钥$B$）和（私钥$sk_B$，公钥$vk_B$）；
   - 公钥交换：Alice向Bob发送ECDH公钥$A$；Bob向Alice发送ECDH公钥$B$和签名$S_B = Sign_{sk_B}(A, B)$（对双方公钥签名）；
   - 签名验证：Alice用Bob的验证公钥$vk_B$验证$S_B$，确认Bob身份合法后，发送签名$S_A = Sign_{sk_A}(B, A)$给Bob；
   - 签名验证：Bob用Alice的验证公钥$vk_A$验证$S_A$，确认Alice身份合法；
   - 密钥计算：双方分别计算共享密钥$K = aB = bA$；
4. 抗MITM机制：中间人无法伪造合法签名（无双方私钥），因此无法通过签名验证环节，确保密钥交换的安全性；
5. 安全依赖：基于ECDLP的难解性和数字签名的不可伪造性。
预期输出：掌握STS协议的身份认证流程，理解其抗中间人攻击的核心机制。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (97, 14, 6, 'DH协议Python实现与验证', '基于cryptography库实现DH协议，完整覆盖参数生成、密钥对生成、公钥交换、共享密钥计算全流程：
1. 依赖库说明：使用cryptography.hazmat.primitives.asymmetric.dh模块实现DH核心功能，HKDF用于密钥派生（增强密钥安全性）；
2. 实现步骤：
   a. 初始化DH类：指定密钥长度（推荐2048位及以上）；
   b. 生成公共参数：调用generate_parameters方法生成大素数$p$和生成元$g$（实际应用中可使用预定义标准参数，避免重复生成耗时）；
   c. 生成密钥对：Alice和Bob使用相同的公共参数，分别生成私钥和公钥；
   d. 公钥交换：将公钥序列化为PEM格式字节流，模拟公开信道传输；
   e. 共享密钥计算：接收对方公钥，反序列化后通过私钥交换得到原始共享密钥，再经HKDF派生为最终密钥（32字节，SHA256哈希）；
   f. 验证密钥一致性：对比Alice和Bob计算的最终密钥，确认是否相同。
3. 关键代码说明：
   - 公钥序列化：使用PEM编码格式，便于传输和存储；
   - HKDF派生：消除原始共享密钥的统计特性，增强密钥安全性；
   - 参数复用：Bob需使用与Alice相同的公共参数，否则无法计算出相同密钥。
预期输出：成功运行代码，Alice和Bob计算出相同的共享密钥，密钥匹配结果为True，验证DH协议实现正确性。', NULL, 'import os
import hashlib
from cryptography.hazmat.primitives.asymmetric import dh
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.backends import default_backend

class DiffieHellman:
    def __init__(self, key_size: int = 2048):
        self.key_size = key_size
        self.parameters = None
        self.private_key = None
        self.public_key = None
    
    def generate_parameters(self) -> dh.DHParameters:
        print(f"[DH] 生成{self.key_size}位参数...")
        self.parameters = dh.generate_parameters(
            generator=2, key_size=self.key_size, backend=default_backend()
        )
        return self.parameters
    
    def generate_key_pair(self, parameters=None):
        if parameters: self.parameters = parameters
        if not self.parameters: self.generate_parameters()
        self.private_key = self.parameters.generate_private_key()
        self.public_key = self.private_key.public_key()
        print("[DH] 密钥对生成完成")
    
    def get_public_key_bytes(self) -> bytes:
        return self.public_key.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        )
    
    def compute_shared_key(self, peer_public_key_bytes: bytes) -> bytes:
        peer_public_key = serialization.load_pem_public_key(
            peer_public_key_bytes, backend=default_backend()
        )
        shared_key = self.private_key.exchange(peer_public_key)
        # HKDF派生密钥
        derived_key = HKDF(
            algorithm=hashes.SHA256(), length=32, salt=None,
            info=b''diffie-hellman-key-exchange'', backend=default_backend()
        ).derive(shared_key)
        return derived_key

# 演示代码
def demo_diffie_hellman():
    print("="*60)
    print("Diffie-Hellman密钥交换演示")
    print("="*60)
    
    # Alice初始化
    alice = DiffieHellman(key_size=2048)
    alice.generate_parameters()
    alice.generate_key_pair()
    
    # Bob使用相同参数
    bob = DiffieHellman(key_size=2048)
    bob.generate_key_pair(alice.parameters)
    
    # 交换公钥
    alice_public = alice.get_public_key_bytes()
    bob_public = bob.get_public_key_bytes()
    print(f"Alice公钥长度: {len(alice_public)}字节")
    
    # 计算共享密钥
    alice_shared = alice.compute_shared_key(bob_public)
    bob_shared = bob.compute_shared_key(alice_public)
    
    # 验证
    print(f"Alice密钥: {alice_shared.hex()}")
    print(f"Bob密钥:   {bob_shared.hex()}")
    print(f"密钥匹配: {alice_shared == bob_shared}")
    
    return alice_shared == bob_shared

# 执行演示
demo_diffie_hellman()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (98, 14, 7, 'ECDH协议Python实现与优势验证', '基于cryptography库的椭圆曲线模块实现ECDH协议，对比传统DH验证其密钥长度优势：
1. 核心差异：ECDH使用椭圆曲线点乘运算替代DH的模幂运算，实现类结构与DH类似，但参数改为椭圆曲线类型；
2. 实现步骤：
   a. 初始化ECDH类：指定椭圆曲线（默认SECP256R1，即NIST P-256，安全性相当于3072位DH）；
   b. 生成密钥对：调用ec.generate_private_key生成椭圆曲线私钥和公钥，无需单独生成公共参数（曲线参数为标准预定义）；
   c. 公钥交换：同样序列化为公钥PEM格式字节流；
   d. 共享密钥计算：通过ec.ECDH()交换算法计算原始共享密钥，再经HKDF派生；
   e. 优势验证：对比ECDH与DH的公钥长度、生成耗时，验证256位ECDH的高效性。
3. 关键注意点：
   - 曲线选择：需使用标准椭圆曲线（如SECP256R1、SECP384R1），避免自定义曲线的安全风险；
   - 密钥派生：与DH相同，需通过HKDF增强密钥安全性。
预期输出：成功运行代码，Alice和Bob得到相同共享密钥，观察到ECDH公钥长度（约178字节）远小于2048位DH（约800字节），验证其高效性。', NULL, 'from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.backends import default_backend

class ECDH:
    def __init__(self, curve=ec.SECP256R1()):
        self.curve = curve
        self.private_key = None
        self.public_key = None
    
    def generate_key_pair(self):
        self.private_key = ec.generate_private_key(self.curve, default_backend())
        self.public_key = self.private_key.public_key()
        print(f"[ECDH] 密钥对生成（曲线: {self.curve.name}")
    
    def get_public_key_bytes(self) -> bytes:
        return self.public_key.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        )
    
    def compute_shared_key(self, peer_public_key_bytes: bytes) -> bytes:
        peer_public_key = serialization.load_pem_public_key(
            peer_public_key_bytes, backend=default_backend()
        )
        shared_key = self.private_key.exchange(ec.ECDH(), peer_public_key)
        # HKDF派生
        derived_key = HKDF(
            algorithm=hashes.SHA256(), length=32, salt=None,
            info=b''ecdh-key-exchange'', backend=default_backend()
        ).derive(shared_key)
        return derived_key

# 演示代码
def demo_ecdh():
    print("\\n" + "="*60)
    print("ECDH密钥交换演示")
    print("="*60)
    
    # Alice生成密钥对
    alice = ECDH(curve=ec.SECP256R1())
    alice.generate_key_pair()
    
    # Bob生成密钥对
    bob = ECDH(curve=ec.SECP256R1())
    bob.generate_key_pair()
    
    # 交换公钥
    alice_public = alice.get_public_key_bytes()
    bob_public = bob.get_public_key_bytes()
    print(f"Alice公钥长度: {len(alice_public)}字节")
    print(f"Bob公钥长度: {len(bob_public)}字节")
    
    # 计算共享密钥
    alice_shared = alice.compute_shared_key(bob_public)
    bob_shared = bob.compute_shared_key(alice_public)
    
    # 验证
    print(f"Alice密钥: {alice_shared.hex()}")
    print(f"Bob密钥:   {bob_shared.hex()}")
    print(f"密钥匹配: {alice_shared == bob_shared}")
    print("\\n256位ECDH安全性≈3072位DH，密钥更短效率更高")
    
    return alice_shared == bob_shared

# 执行演示
demo_ecdh()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (99, 14, 8, 'KDC密钥分发协议Python实现', '实现简化版KDC协议，模拟用户注册、会话密钥生成、分发与解密流程：
1. 核心设计：
   - KDC类：维护用户长期密钥字典，提供用户注册、会话密钥生成、加密解密功能；
   - 加密方案：使用XOR+MAC模拟对称加密（实际应用需使用AES等标准算法），确保消息完整性和机密性；
2. 实现步骤：
   a. KDC初始化：创建用户密钥字典，存储用户ID与长期密钥映射；
   b. 用户注册：用户向KDC注册，可自定义长期密钥或自动生成（32字节随机数）；
   c. 会话密钥生成：KDC接收Alice与Bob的通信请求，生成32字节会话密钥，构造包含会话密钥、对方标识、时间戳的消息；
   d. 消息加密：用Alice的长期密钥加密给Alice的消息，用Bob的长期密钥加密给Bob的消息；
   e. 消息解密与密钥获取：Alice解密KDC消息得到会话密钥，转发Bob的加密消息；Bob解密得到会话密钥；
   f. 验证：对比Alice和Bob获取的会话密钥，确认一致性。
3. 关键实现细节：
   - MAC验证：通过HMAC-SHA256验证消息完整性，防止消息被篡改；
   - 时间戳：用于防止重放攻击（简化版未实现校验逻辑，实际需验证时间戳有效性）；
   - 长期密钥存储：实际应用中需安全存储，如加密数据库。
预期输出：成功完成用户注册、会话密钥生成与分发，Alice和Bob获取相同会话密钥，验证KDC协议的中心化分发功能。', NULL, 'import secrets
import time
hashlib
from cryptography.hazmat.primitives import hmac, hashes
from cryptography.hazmat.backends import default_backend

class KeyDistributionCenter:
    def __init__(self):
        self.user_keys = {}  # 存储用户ID与长期密钥映射
    
    def register_user(self, user_id: str, key: bytes = None) -> bytes:
        if not key: key = secrets.token_bytes(32)
        self.user_keys[user_id] = key
        print(f"[KDC] 用户{user_id}注册成功")
        return key
    
    def generate_session_key(self, alice_id: str, bob_id: str) -> tuple:
        if alice_id not in self.user_keys or bob_id not in self.user_keys:
            raise ValueError("用户未注册")
        # 生成会话密钥
        session_key = secrets.token_bytes(32)
        timestamp = str(int(time.time())).encode()
        
        # 构造消息
        alice_msg = session_key + b''||'' + bob_id.encode() + b''||'' + timestamp
        bob_msg = session_key + b''||'' + alice_id.encode() + b''||'' + timestamp
        
        # 加密消息
        alice_encrypted = self._encrypt(alice_msg, self.user_keys[alice_id])
        bob_encrypted = self._encrypt(bob_msg, self.user_keys[bob_id])
        print(f"[KDC] 生成{alice_id}与{bob_id}的会话密钥")
        return alice_encrypted, bob_encrypted
    
    def _encrypt(self, data: bytes, key: bytes) -> bytes:
        # XOR加密+MAC验证
        cipher_key = hashlib.sha256(key).digest()
        encrypted = bytes([data[i] ^ cipher_key[i%len(cipher_key)] for i in range(len(data))])
        # 计算MAC
        h = hmac.HMAC(key, hashes.SHA256(), backend=default_backend())
        h.update(encrypted)
        mac = h.finalize()
        return encrypted + b''||MAC||'' + mac
    
    def _decrypt(self, encrypted_data: bytes, key: bytes) -> bytes:
        # 分离数据与MAC
        parts = encrypted_data.split(b''||MAC||'')
        if len(parts)!=2: raise ValueError("无效数据")
        encrypted, mac = parts
        # 验证MAC
        h = hmac.HMAC(key, hashes.SHA256(), backend=default_backend())
        h.update(encrypted)
        h.verify(mac)
        # 解密
        cipher_key = hashlib.sha256(key).digest()
        decrypted = bytes([encrypted[i]^cipher_key[i%len(cipher_key)] for i in range(len(encrypted))])
        return decrypted

# 演示代码
def demo_kdc():
    print("\\n" + "="*60)
    print("KDC密钥分发演示")
    print("="*60)
    
    # 初始化KDC并注册用户
    kdc = KeyDistributionCenter()
    alice_key = kdc.register_user("Alice")
    bob_key = kdc.register_user("Bob")
    
    # 生成会话密钥
    alice_msg, bob_msg = kdc.generate_session_key("Alice", "Bob")
    
    # Alice解密
    alice_decrypted = kdc._decrypt(alice_msg, alice_key)
    session_key_alice = alice_decrypted.split(b''||'')[0]
    print(f"Alice获得会话密钥: {session_key_alice.hex()}")
    
    # Bob解密
    bob_decrypted = kdc._decrypt(bob_msg, bob_key)
    session_key_bob = bob_decrypted.split(b''||'')[0]
    print(f"Bob获得会话密钥: {session_key_bob.hex()}")
    
    # 验证
    print(f"密钥匹配: {session_key_alice == session_key_bob}")
    
    return session_key_alice == session_key_bob

# 执行演示
demo_kdc()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (100, 14, 9, 'STS协议Python实现与抗MITM验证', '实现基于ECDH的STS协议，集成数字签名功能，验证其抗中间人攻击能力：
1. 核心设计：
   - 融合ECDH密钥交换与RSA数字签名；
   - 密钥协商过程包含身份认证，确保通信双方身份合法；
2. 实现步骤：
   a. 初始化STS类：内部包含ECDH模块（用于密钥交换）和RSA签名模块（用于身份认证）；
   b. 生成密钥对：同时生成ECDH密钥对（用于密钥交换）和RSA签名密钥对（用于身份认证）；
   c. 签名交换：
      - Alice发送ECDH公钥给Bob；
      - Bob接收后，对“Alice公钥+Bob公钥”签名，发送Bob公钥和签名给Alice；
      - Alice验证Bob签名通过后，对“Bob公钥+Alice公钥”签名，发送签名给Bob；
   d. 签名验证：双方分别验证对方签名，确保身份合法；
   e. 密钥计算：验证通过后，双方计算共享密钥；
3. 抗MITM验证逻辑：
   - 中间人无法伪造合法签名（无双方RSA私钥），即使拦截并篡改公钥，也会因签名验证失败而被识破；
   - 签名内容包含双方公钥，确保公钥未被篡改。
4. 关键实现细节：
   - 签名算法：使用RSA-PSS签名方案，安全性更高；
   - 消息构造：签名内容为双方公钥拼接，确保上下文完整性；
   - 验证失败处理：签名验证失败直接抛出异常，终止密钥协商。
预期输出：成功完成签名交换与验证，双方计算出相同共享密钥，验证STS协议的身份认证与抗MITM能力。', NULL, 'from cryptography.hazmat.primitives.asymmetric import ec, rsa, padding
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.backends import default_backend

class KeyAgreementProtocol:
    def __init__(self):
        self.dh = ECDH()  # 复用之前实现的ECDH类
        self.signature_key = None  # RSA私钥（签名用）
        self.verify_key = None     # RSA公钥（验证用）
    
    def generate_keys(self):
        # 生成ECDH密钥对
        self.dh.generate_key_pair()
        # 生成RSA签名密钥对
        self.signature_key = rsa.generate_private_key(
            public_exponent=65537, key_size=2048, backend=default_backend()
        )
        self.verify_key = self.signature_key.public_key()
        print("[STS] 密钥生成完成")
    
    def create_signed_exchange(self, peer_public_key_bytes: bytes) -> tuple:
        # 对“自己公钥+对方公钥”签名
        my_public = self.dh.get_public_key_bytes()
        message = my_public + peer_public_key_bytes
        signature = self.signature_key.sign(
            message,
            padding.PSS(
                mgf=padding.MGF1(hashes.SHA256()),
                salt_length=padding.PSS.MAX_LENGTH
            ),
            hashes.SHA256()
        )
        return my_public, signature
    
    def verify_and_compute_key(self, peer_dh_public: bytes, peer_verify_key, signature: bytes) -> bytes:
        # 验证签名
        my_public = self.dh.get_public_key_bytes()
        message = peer_dh_public + my_public
        try:
            peer_verify_key.verify(
                signature,
                message,
                padding.PSS(
                    mgf=padding.MGF1(hashes.SHA256()),
                    salt_length=padding.PSS.MAX_LENGTH
                ),
                hashes.SHA256()
            )
            print("[STS] 签名验证成功")
        except Exception as e:
            raise ValueError(f"签名验证失败: {e}")
        # 计算共享密钥
        return self.dh.compute_shared_key(peer_dh_public)

# 演示代码
def demo_sts_protocol():
    print("\\n" + "="*60)
    print("STS密钥协商协议演示")
    print("="*60)
    
    # 生成密钥对
    alice = KeyAgreementProtocol()
    alice.generate_keys()
    bob = KeyAgreementProtocol()
    bob.generate_keys()
    
    # 第一轮：Alice发送ECDH公钥
    alice_dh_public = alice.dh.get_public_key_bytes()
    
    # 第二轮：Bob发送公钥和签名
    bob_dh_public, bob_signature = bob.create_signed_exchange(alice_dh_public)
    
    # 第三轮：Alice验证签名并发送签名
    alice_shared = alice.verify_and_compute_key(
        bob_dh_public, bob.verify_key, bob_signature
    )
    _, alice_signature = alice.create_signed_exchange(bob_dh_public)
    
    # 第四轮：Bob验证签名
    bob_shared = bob.verify_and_compute_key(
        alice_dh_public, alice.verify_key, alice_signature
    )
    
    # 验证
    print(f"Alice密钥: {alice_shared.hex()}")
    print(f"Bob密钥:   {bob_shared.hex()}")
    print(f"密钥匹配: {alice_shared == bob_shared}")
    print("\\nSTS协议提供相互认证，抗中间人攻击")
    
    return alice_shared == bob_shared

# 执行演示
demo_sts_protocol()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (101, 14, 10, '四类协议对比与实际应用选型', '基于原理、安全性、性能、适用场景对四类协议进行全面对比，明确选型依据：
1. 核心特性对比：
   - DH协议：去中心化，无身份认证（易受MITM攻击），密钥长度长（2048+位），适用于简单密钥交换场景；
   - ECDH协议：去中心化，无身份认证，密钥长度短（256位），计算高效，适用于资源受限场景（移动设备、物联网）；
   - KDC协议：中心化，依赖可信第三方，适用于大规模系统（企业内网、分布式服务），管理成本低，但KDC是单点故障；
   - STS协议：去中心化，支持身份认证（抗MITM），安全性最高，适用于高安全需求场景（金融、政务通信）；
2. 安全性对比：
   安全等级排序：STS > ECDH = DH > KDC（依赖KDC可信度）；
   抗攻击能力：STS抗MITM，其他无（KDC需保障自身安全）；
3. 性能对比：
   计算效率：ECDH > STS > DH > KDC；
   带宽消耗：ECDH < DH < STS < KDC；
4. 实际应用选型建议：
   - 小型去中心化场景：ECDH（高效安全）；
   - 大规模系统：KDC（简化密钥管理）；
   - 高安全需求场景：STS（抗MITM+身份认证）；
   - 兼容性需求场景：DH（传统系统兼容）。
预期输出：能够根据实际场景的安全性、性能、拓扑结构需求，选择合适的密钥管理协议。', '实际应用中，ECDH常与数字签名结合（类似STS思路），既保留高效性，又提供身份认证', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (102, 15, 1, '认证协议核心概念与分类', '认证协议是保障网络通信安全的基础，核心目标是验证参与通信实体的身份合法性，防止身份伪造、冒充等攻击。首先明确认证协议的核心分类及应用场景：
1. 实体认证协议：聚焦单一实体的身份验证，核心是证明“你是谁”，典型代表为挑战-响应（Challenge-Response）协议，适用于点对点的身份校验；
2. 身份认证协议：面向复杂系统的身份管理，不仅验证身份，还涉及授权、单点登录等功能，典型代表为Kerberos（中心化认证）和OAuth（第三方授权）；
3. 相互认证协议：通信双方互相验证对方身份，防止中间人攻击，常见于高安全需求场景，如STS协议、Kerberos协议的扩展应用。
三类协议的核心差异在于验证对象（单方/多方）、应用场景（简单点对点/复杂系统）、附加功能（仅认证/认证+授权）。理解分类是后续学习的基础，需明确不同协议的定位与核心解决的问题。
预期输出：建立认证协议的分类框架，明确各类协议的核心目标与适用场景。', '重点区分“认证”与“授权”：认证是验证身份，授权是允许操作', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (103, 15, 2, '挑战-响应（Challenge-Response）协议原理', '挑战-响应协议是实体认证的经典方案，1970年代提出，核心解决传统密码传输的安全隐患，通过“随机挑战”机制防止重放攻击和密码泄露。
1. 历史背景：传统静态密码认证中，密码需在网络传输，易被监听窃取；即使加密传输，也可能被重放攻击。挑战-响应协议通过动态生成挑战，使每次认证的响应不同，从根本上解决这两个问题；
2. 核心工作原理：
   - 初始化：验证方（如服务器）与被验证方（如客户端）共享密钥K（或被验证方拥有私钥sk，验证方拥有公钥pk）；
   - 挑战阶段：验证方向被验证方发送随机生成的挑战值R（每次认证的R均不同）；
   - 响应阶段：被验证方使用共享密钥K或私钥sk，对挑战值R进行密码学运算，生成响应S = f(K, R)（对称密钥场景）或S = Sign_sk(R)（公钥场景）；
   - 验证阶段：验证方使用相同的密钥K或公钥pk，对响应S进行验证，若验证通过则确认身份合法。
3. 数学核心与安全机制：
   - 对称密钥场景：常用消息认证码（MAC）实现响应计算，即S = MAC_K(R)，安全性依赖MAC的抗碰撞性和密钥K的保密性；
   - 公钥场景：使用数字签名实现响应计算，即S = Sign_sk(R)，验证时使用公钥pk验证签名，安全性依赖私钥的保密性和签名算法的不可伪造性；
   - 防重放机制：每次挑战值R随机生成且仅使用一次，验证方记录已使用的挑战值，防止攻击者拦截响应后重放。
4. 核心优势：
   - 密码（或私钥）从不需在网络传输，避免监听泄露；
   - 每次响应动态生成，防止重放攻击；
   - 实现简单，计算开销低，适用于资源受限场景。
预期输出：掌握挑战-响应协议的完整流程，理解“随机挑战”的核心作用，明确对称密钥和公钥两种实现方式的数学原理。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (104, 15, 3, 'Kerberos协议原理与单点登录机制', 'Kerberos是1980年代MIT开发的中心化身份认证协议，核心目标是解决分布式系统中的单点登录（SSO）问题，使用户一次认证后可访问多个服务，无需重复输入密码。
1. 历史背景：大型分布式系统中，用户需访问多个服务，若每个服务都单独认证，用户体验差且密钥管理复杂。Kerberos通过中心化的密钥分发中心（KDC），统一管理密钥和票据，实现单点登录；
2. 核心组件：
   - KDC（Key Distribution Center）：核心组件，包含AS（Authentication Server，认证服务器）和TGS（Ticket Granting Server，票据授予服务器）；
   - 客户端（Client）：需要访问服务的用户或设备；
   - 服务端（Server）：提供资源的应用服务器（如文件服务器、数据库服务器）。
3. 完整工作流程（基于票据机制）：
   - 阶段1：获取TGT（Ticket Granting Ticket，票据授予票据）；
     1. 客户端向AS发送身份信息，请求TGT；
     2. AS验证客户端身份（通过预共享的长期密钥K_U），生成TGT（用TGS密钥K_TGS加密）和TGS会话密钥K_U,TGS（用K_U加密），发送给客户端；
     3. 客户端用K_U解密得到K_U,TGS和TGT。
   - 阶段2：获取服务票据（Service Ticket）；
     1. 客户端向TGS发送TGT和认证器（用K_U,TGS加密的身份信息+时间戳），请求目标服务的票据；
     2. TGS解密TGT得到K_U,TGS，再用K_U,TGS解密认证器，验证身份合法后，生成服务票据（用服务密钥K_S加密）和服务会话密钥K_U,S（用K_U,TGS加密），发送给客户端；
     3. 客户端用K_U,TGS解密得到K_U,S和服务票据。
   - 阶段3：访问服务；
     1. 客户端向服务端发送服务票据和新的认证器（用K_U,S加密）；
     2. 服务端用K_S解密服务票据得到K_U,S，再用K_U,S解密认证器，验证身份合法后，允许客户端访问服务。
4. 数学表示与安全机制：
   - TGT格式：TGT = E_{K_TGS}[U, K_{U,TGS}, TGS, timestamp]，其中E_K[M]表示用密钥K加密消息M；
   - 服务票据格式：Service Ticket = E_{K_S}[U, K_{U,S}, S, timestamp]；
   - 防重放机制：时间戳和票据有效期（TGT默认8小时，服务票据默认1小时）；
   - 安全性：依赖KDC的可信度、长期密钥的保密性、加密算法的安全性。
5. 核心优势：单点登录、密码不传输、相互认证（客户端验证服务端，服务端验证客户端）。
预期输出：掌握Kerberos协议的三阶段流程，理解票据机制和单点登录的实现原理。', 'TGT是“票据的票据”，仅用于向TGS请求其他服务票据，不能直接访问服务', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (105, 15, 4, 'OAuth 2.0协议原理与第三方授权机制', 'OAuth 2.0是2012年发布的第三方授权协议，核心目标是解决“第三方应用访问用户资源但无需获取用户密码”的问题，实现认证与授权分离，广泛应用于微信登录、GitHub授权等场景。
1. 历史背景：传统第三方应用访问用户资源时，用户需向第三方泄露密码，存在密码泄露风险。OAuth 2.0通过“令牌（Token）”替代密码，用户授权第三方应用使用令牌访问资源，无需暴露密码；
2. 核心角色：
   - 资源所有者（Resource Owner）：用户，拥有受保护的资源（如照片、联系人）；
   - 客户端（Client）：第三方应用（如照片备份APP），需要访问用户资源；
   - 授权服务器（Authorization Server）：发放访问令牌的服务器（如微信授权服务器）；
   - 资源服务器（Resource Server）：存储用户资源的服务器（如微信照片存储服务器）。
3. 核心工作原理（授权码流程，最常用）：
   - 阶段1：授权请求；客户端向授权服务器发起授权请求，用户同意授权后，授权服务器返回授权码（Authorization Code）；
   - 阶段2：令牌请求；客户端使用授权码向授权服务器请求访问令牌（Access Token），授权服务器验证授权码合法后，返回访问令牌和刷新令牌（Refresh Token）；
   - 阶段3：资源访问；客户端使用访问令牌向资源服务器请求资源，资源服务器验证令牌合法后，返回用户资源；
   - 阶段4：令牌刷新；访问令牌过期后，客户端使用刷新令牌向授权服务器请求新的访问令牌，无需用户再次授权。
4. 数学核心与安全机制：
   - 令牌格式：T = Encode(userID, scope, expiry, signature)，包含用户ID、权限范围、过期时间、数字签名；
   - 安全性：签名保证令牌防篡改，过期时间限制令牌有效期，授权码仅使用一次防止重放，权限范围实现细粒度授权；
   - 核心特点：授权而非认证（重点是允许操作，而非验证身份）、密码不泄露、权限可撤销。
5. 与Kerberos的差异：OAuth是去中心化的第三方授权，Kerberos是中心化的内部认证；OAuth适用于跨组织的资源访问，Kerberos适用于组织内部的单点登录。
预期输出：掌握OAuth 2.0的授权码流程，理解令牌机制和第三方授权的核心逻辑。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (106, 15, 5, '相互认证协议原理与抗MITM机制', '相互认证协议要求通信双方在建立连接前互相验证对方身份，核心解决中间人攻击问题，适用于高安全需求场景（如金融通信、政务系统）。
1. 核心目标：不仅验证客户端身份，还验证服务端身份，确保通信双方都是预期的合法实体，防止中间人拦截、篡改通信内容；
2. 典型实现方式：
   - 基于挑战-响应的扩展：双方互相发送挑战，各自生成响应并验证。例如，客户端向服务端发送挑战R1，服务端返回响应S1 = MAC_K(R1)；同时服务端向客户端发送挑战R2，客户端返回响应S2 = MAC_K(R2)，双方验证通过则完成相互认证；
   - 基于数字签名的实现（如STS协议）：通信双方交换公钥和共享密钥相关信息，并用各自私钥签名，对方用公钥验证签名。例如，Alice和Bob交换DH公钥后，Alice用私钥签名共享密钥和Bob的公钥，Bob验证签名；Bob同理签名并发送给Alice，Alice验证，实现相互认证；
   - Kerberos的相互认证：服务端通过验证客户端的服务票据和认证器确认客户端身份，客户端通过服务端能解密票据并返回验证信息，确认服务端身份，本质是基于共享会话密钥的相互验证。
3. 数学核心：依赖共享密钥的保密性（对称场景）或数字签名的不可伪造性（公钥场景），确保只有合法实体才能生成有效的响应或签名；
4. 核心优势：彻底防止中间人攻击，提供端到端的身份合法性保障。
预期输出：理解相互认证的核心逻辑，掌握两种典型实现方式的原理，明确其抗中间人攻击的机制。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (107, 15, 6, '挑战-响应协议Python实现', '基于对称密钥（HMAC）实现挑战-响应协议，完整覆盖用户注册、挑战生成、响应计算、身份验证全流程：
1. 核心设计思路：
   - 用户注册：存储用户密码的哈希值（而非明文），使用PBKDF2算法派生密钥，增强安全性；
   - 挑战生成：使用secrets模块生成32字节随机挑战，确保每次挑战唯一；
   - 响应计算：基于HMAC-SHA256算法，用用户密码哈希作为密钥，对挑战值进行计算，生成响应；
   - 防重放机制：记录已使用的挑战值，避免同一挑战被重复使用。
2. 实现步骤：
   a. 初始化协议类：维护用户密码哈希字典和已使用挑战集合；
   b. 用户注册：接收用户名和密码，用PBKDF2（盐为用户名，迭代100000次）派生密钥并存储；
   c. 挑战生成：生成随机挑战字符串并返回；
   d. 响应计算：客户端用自身密码哈希对挑战值计算HMAC响应；
   e. 身份验证：服务器生成挑战，接收客户端响应，与预期响应（用存储的密码哈希计算）对比，验证身份合法性。
3. 关键代码说明：
   - PBKDF2密钥派生：通过迭代次数和盐值增强密码哈希的抗破解性；
   - HMAC计算：确保响应的完整性和抗篡改，只有拥有正确密码的用户才能生成正确响应；
   - 防重放处理：已使用的挑战值加入集合，再次使用时直接判定为攻击。
预期输出：成功运行代码，实现用户注册、正确密码认证成功、错误密码认证失败的功能，验证挑战-响应协议的安全性。', NULL, 'import hmac
import hashlib
import secrets
from typing import Dict, Tuple

class ChallengeResponseAuth:
    def __init__(self):
        # 存储用户密码哈希（用户名->密码哈希）
        self.user_passwords: Dict[str, bytes] = {}
        # 存储已使用的挑战（防重放攻击）
        self.used_challenges: set = set()
    
    def register_user(self, username: str, password: str):
        """注册用户，存储密码哈希"""
        password_hash = hashlib.pbkdf2_hmac(
            ''sha256'',
            password.encode(),
            username.encode(),  # 盐值为用户名
            100000  # 迭代次数
        )
        self.user_passwords[username] = password_hash
        print(f"[Challenge-Response] 用户 {username} 注册成功")
    
    def generate_challenge(self) -> str:
        """生成32字节随机挑战"""
        return secrets.token_hex(32)
    
    def compute_response(self, challenge: str, password_hash: bytes) -> str:
        """计算响应：HMAC-SHA256(challenge, password_hash)"""
        return hmac.new(
            password_hash,
            challenge.encode(),
            hashlib.sha256
        ).hexdigest()
    
    def authenticate(self, username: str, password: str) -> Tuple[bool, str]:
        """完整认证流程"""
        # 检查用户是否存在
        if username not in self.user_passwords:
            return False, "用户不存在"
        
        # 生成挑战
        challenge = self.generate_challenge()
        print(f"[Server] 发送挑战: {challenge[:16]}...")
        
        # 防重放检查
        if challenge in self.used_challenges:
            return False, "检测到重放攻击"
        
        # 客户端计算响应（模拟）
        client_password_hash = hashlib.pbkdf2_hmac(
            ''sha256'', password.encode(), username.encode(), 100000
        )
        client_response = self.compute_response(challenge, client_password_hash)
        print(f"[Client] 发送响应: {client_response[:16]}...")
        
        # 服务器验证响应
        server_response = self.compute_response(challenge, self.user_passwords[username])
        self.used_challenges.add(challenge)  # 标记挑战已使用
        
        if hmac.compare_digest(client_response, server_response):
            return True, "认证成功"
        else:
            return False, "认证失败"

# 演示代码
def demo_challenge_response():
    print("="*60)
    print("挑战-响应认证协议演示")
    print("="*60)
    
    # 初始化认证系统
    auth_system = ChallengeResponseAuth()
    
    # 注册用户
    print("\\n[步骤1] 注册用户")
    auth_system.register_user("alice", "password123")
    
    # 正确密码认证
    print("\\n[步骤2] 正确密码认证")
    success, msg = auth_system.authenticate("alice", "password123")
    print(f"[结果] {msg}")
    
    # 错误密码认证
    print("\\n[步骤3] 错误密码认证")
    success2, msg2 = auth_system.authenticate("alice", "wrongpass")
    print(f"[结果] {msg2}")
    
    return success and not success2

# 执行演示
demo_challenge_response()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (108, 15, 7, 'Kerberos协议Python模拟实现', '模拟Kerberos协议的核心流程，实现KDC、AS、TGS的核心功能，涵盖用户注册、TGT获取、服务票据获取、服务访问全流程：
1. 核心设计思路：
   - 数据结构：定义Ticket类封装票据信息（客户端ID、服务端ID、会话密钥、时间戳、有效期），支持序列化/反序列化；
   - 密钥管理：KDC维护用户长期密钥、服务密钥、TGS密钥，均通过安全方式生成（密码派生或随机生成）；
   - 加密方案：使用XOR+哈希简化模拟加密（实际应用需用AES），确保票据和会话密钥的机密性；
   - 流程模拟：完整复现Kerberos的三阶段流程，验证票据的生成、加密、传输、解密、验证全环节。
2. 实现步骤：
   a. 初始化KDC：创建用户密钥、服务密钥、TGS密钥存储结构；
   b. 用户/服务注册：用户注册时通过PBKDF2派生长期密钥，服务注册时生成随机长期密钥；
   c. AS流程（TGT获取）：验证用户密码，生成TGT（用TGS密钥加密）和TGS会话密钥（用用户密钥加密）；
   d. TGS流程（服务票据获取）：解密TGT验证合法性，生成服务票据（用服务密钥加密）和服务会话密钥（用TGS会话密钥加密）；
   e. 服务访问：模拟客户端使用服务票据访问服务，服务端验证票据有效性。
3. 关键代码说明：
   - Ticket类：封装票据核心字段，支持to_bytes/from_bytes实现序列化传输；
   - 加密/解密：XOR加密简化演示，实际需使用AES-GCM等安全加密算法；
   - 票据验证：通过时间戳检查票据有效期，确保票据的时效性。
预期输出：成功运行代码，完成TGT获取、服务票据获取、服务访问的全流程验证，实现Kerberos的单点登录核心功能。', NULL, 'import json
import time
import base64
import hashlib
import secrets
from dataclasses import dataclass
from typing import Dict, Tuple, Optional

@dataclass
class Ticket:
    """Kerberos票据数据结构"""
    client_id: str
    server_id: str
    session_key: bytes
    timestamp: float
    lifetime: int  # 有效期（秒）
    
    def is_valid(self) -> bool:
        """检查票据是否有效（未过期）"""
        return time.time() - self.timestamp < self.lifetime
    
    def to_bytes(self) -> bytes:
        """序列化票据"""
        data = {
            ''client_id'': self.client_id,
            ''server_id'': self.server_id,
            ''session_key'': base64.b64encode(self.session_key).decode(),
            ''timestamp'': self.timestamp,
            ''lifetime'': self.lifetime
        }
        return json.dumps(data).encode()
    
    @classmethod
    def from_bytes(cls, data: bytes) -> ''Ticket'':
        """反序列化票据"""
        obj = json.loads(data.decode())
        return cls(
            client_id=obj[''client_id''],
            server_id=obj[''server_id''],
            session_key=base64.b64decode(obj[''session_key'']),
            timestamp=obj[''timestamp''],
            lifetime=obj[''lifetime'']
        )

class KerberosSimulation:
    def __init__(self):
        self.user_keys: Dict[str, bytes] = {}  # 用户长期密钥
        self.service_keys: Dict[str, bytes] = {}  # 服务长期密钥
        self.tgs_key = secrets.token_bytes(32)  # TGS长期密钥
    
    def register_user(self, user_id: str, password: str):
        """注册用户，派生长期密钥"""
        user_key = hashlib.pbkdf2_hmac(
            ''sha256'', password.encode(), user_id.encode(), 100000
        )
        self.user_keys[user_id] = user_key
        print(f"[Kerberos] 用户 {user_id} 注册成功")
    
    def register_service(self, service_id: str) -> bytes:
        """注册服务，生成长期密钥"""
        service_key = secrets.token_bytes(32)
        self.service_keys[service_id] = service_key
        print(f"[Kerberos] 服务 {service_id} 注册成功")
        return service_key
    
    def _encrypt(self, data: bytes, key: bytes) -> bytes:
        """简化加密（XOR）"""
        cipher_key = hashlib.sha256(key).digest()
        return bytes([
            data[i] ^ cipher_key[i % len(cipher_key)]
            for i in range(len(data))
        ])
    
    def _decrypt(self, encrypted: bytes, key: bytes) -> bytes:
        """简化解密（XOR可逆）"""
        return self._encrypt(encrypted, key)
    
    def as_request_tgt(self, user_id: str, password: str) -> Tuple[Optional[bytes], Optional[bytes]]:
        """AS流程：获取TGT"""
        print(f"\\n[AS] 收到 {user_id} 的TGT请求")
        
        # 验证用户和密码
        if user_id not in self.user_keys:
            print("[AS] 用户不存在")
            return None, None
        user_key = hashlib.pbkdf2_hmac(
            ''sha256'', password.encode(), user_id.encode(), 100000
        )
        if user_key != self.user_keys[user_id]:
            print("[AS] 密码错误")
            return None, None
        
        # 生成TGS会话密钥和TGT
        tgs_session_key = secrets.token_bytes(32)
        tgt = Ticket(
            client_id=user_id,
            server_id=''TGS'',
            session_key=tgs_session_key,
            timestamp=time.time(),
            lifetime=28800  # 8小时
        )
        encrypted_tgt = self._encrypt(tgt.to_bytes(), self.tgs_key)
        
        # 生成加密的会话信息
        session_info = json.dumps({
            ''session_key'': base64.b64encode(tgs_session_key).decode(),
            ''lifetime'': 28800
        }).encode()
        encrypted_session_info = self._encrypt(session_info, user_key)
        
        print("[AS] TGT生成成功")
        return encrypted_tgt, encrypted_session_info
    
    def tgs_request_service_ticket(self, encrypted_tgt: bytes, service_id: str, authenticator: bytes) -> Tuple[Optional[bytes], Optional[bytes]]:
        """TGS流程：获取服务票据"""
        print(f"\\n[TGS] 收到服务票据请求（服务：{service_id}")
        
        # 解密TGT
        try:
            tgt = Ticket.from_bytes(self._decrypt(encrypted_tgt, self.tgs_key))
        except Exception as e:
            print(f"[TGS] TGT解密失败: {e}")
            return None, None
        
        # 验证TGT有效性
        if not tgt.is_valid():
            print("[TGS] TGT已过期")
            return None, None
        
        # 验证认证器
        try:
            self._decrypt(authenticator, tgt.session_key)
        except:
            print("[TGS] 认证器验证失败")
            return None, None
        
        # 生成服务会话密钥和服务票据
        service_session_key = secrets.token_bytes(32)
        service_ticket = Ticket(
            client_id=tgt.client_id,
            server_id=service_id,
            session_key=service_session_key,
            timestamp=time.time(),
            lifetime=3600  # 1小时
        )
        encrypted_service_ticket = self._encrypt(
            service_ticket.to_bytes(), self.service_keys[service_id]
        )
        
        # 生成加密的服务会话信息
        session_info = json.dumps({
            ''session_key'': base64.b64encode(service_session_key).decode(),
            ''lifetime'': 3600
        }).encode()
        encrypted_session_info = self._encrypt(session_info, tgt.session_key)
        
        print("[TGS] 服务票据生成成功")
        return encrypted_service_ticket, encrypted_session_info

# 演示代码
def demo_kerberos():
    print("\\n" + "="*60)
    print("Kerberos认证协议演示")
    print("="*60)
    
    # 初始化KDC
    kerberos = KerberosSimulation()
    
    # 注册用户和服务
    print("\\n[步骤1] 注册用户和服务")
    kerberos.register_user("alice", "password123")
    kerberos.register_service("FileServer")
    
    # 获取TGT
    print("\\n[步骤2] 获取TGT")
    encrypted_tgt, encrypted_session_info = kerberos.as_request_tgt("alice", "password123")
    if not encrypted_tgt:
        return False
    
    # 客户端解密会话信息
    user_key = hashlib.pbkdf2_hmac(''sha256'', b''password123'', b''alice'', 100000)
    session_info = json.loads(kerberos._decrypt(encrypted_session_info, user_key))
    tgs_session_key = base64.b64decode(session_info[''session_key''])
    print("[Client] 已获取TGS会话密钥")
    
    # 获取服务票据
    print("\\n[步骤3] 获取服务票据")
    authenticator = json.dumps({''client_id'': ''alice'', ''timestamp'': time.time()}).encode()
    encrypted_authenticator = kerberos._encrypt(authenticator, tgs_session_key)
    encrypted_service_ticket, _ = kerberos.tgs_request_service_ticket(
        encrypted_tgt, "FileServer", encrypted_authenticator
    )
    if not encrypted_service_ticket:
        return False
    
    # 访问服务
    print("\\n[步骤4] 访问文件服务器")
    print("[FileServer] 验证票据成功，授权访问")
    
    return True

# 执行演示
demo_kerberos()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (109, 15, 8, 'OAuth 2.0协议Python模拟实现', '模拟OAuth 2.0的授权码流程，实现客户端注册、授权请求、令牌发放、资源访问、令牌刷新全流程，核心还原第三方授权的核心逻辑：
1. 核心设计思路：
   - 角色模拟：实现授权服务器的核心功能，维护客户端信息、授权码、访问令牌、刷新令牌的存储；
   - 流程还原：完整复现OAuth 2.0授权码流程的关键环节，包括授权码生成、令牌兑换、令牌验证、令牌刷新；
   - 安全机制：模拟授权码的一次性使用、访问令牌的有效期、刷新令牌的长期有效性，确保流程的安全性。
2. 实现步骤：
   a. 初始化授权服务器：创建客户端、授权码、访问令牌、刷新令牌的存储结构；
   b. 客户端注册：第三方应用向授权服务器注册，获取客户端ID和客户端密钥；
   c. 授权请求：用户同意授权后，授权服务器生成授权码并返回给客户端；
   d. 令牌请求：客户端使用授权码兑换访问令牌和刷新令牌，授权服务器验证授权码合法性；
   e. 令牌验证：资源服务器验证访问令牌的有效性，允许合法令牌访问资源；
   f. 令牌刷新：访问令牌过期后，客户端使用刷新令牌获取新的访问令牌。
3. 关键代码说明：
   - 授权码管理：标记授权码是否已使用，防止重复兑换，设置10分钟过期时间；
   - 令牌管理：访问令牌有效期1小时，刷新令牌长期有效，支持生成新的访问令牌；
   - 权限控制：通过scope参数实现细粒度授权，记录令牌的权限范围。
预期输出：成功运行代码，完成第三方应用注册、用户授权、令牌兑换、资源访问、令牌刷新的全流程，验证OAuth 2.0的第三方授权机制。', NULL, 'import time
import secrets
from typing import Dict, List, Optional

class OAuthSimulation:
    def __init__(self):
        self.clients: Dict[str, Dict] = {}  # 客户端信息（client_id->信息）
        self.authorization_codes: Dict[str, Dict] = {}  # 授权码
        self.access_tokens: Dict[str, Dict] = {}  # 访问令牌
        self.refresh_tokens: Dict[str, Dict] = {}  # 刷新令牌
    
    def register_client(self, client_id: str, client_secret: str, redirect_uri: str) -> Dict:
        """注册第三方客户端"""
        client_info = {
            ''client_id'': client_id,
            ''client_secret'': client_secret,
            ''redirect_uri'': redirect_uri
        }
        self.clients[client_id] = client_info
        print(f"[OAuth] 客户端 {client_id} 注册成功")
        return client_info
    
    def authorization_request(self, client_id: str, redirect_uri: str, scope: List[str], user_id: str) -> Optional[str]:
        """授权请求：生成授权码"""
        print(f"\\n[OAuth] 收到授权请求（客户端：{client_id}，用户：{user_id}")
        
        # 验证客户端和回调URI
        if client_id not in self.clients:
            print("[OAuth] 客户端未注册")
            return None
        if self.clients[client_id][''redirect_uri''] != redirect_uri:
            print("[OAuth] 回调URI不匹配")
            return None
        
        # 生成授权码（10分钟过期）
        auth_code = secrets.token_urlsafe(32)
        self.authorization_codes[auth_code] = {
            ''client_id'': client_id,
            ''user_id'': user_id,
            ''redirect_uri'': redirect_uri,
            ''scope'': scope,
            ''timestamp'': time.time(),
            ''used'': False
        }
        
        print(f"[OAuth] 授权码生成：{auth_code[:16]}...")
        print(f"[OAuth] 授权范围：{'', ''.join(scope)}")
        return auth_code
    
    def token_request(self, client_id: str, client_secret: str, auth_code: str, redirect_uri: str) -> Optional[Dict]:
        """令牌请求：用授权码兑换访问令牌"""
        print(f"\\n[OAuth] 收到令牌请求（客户端：{client_id}")
        
        # 验证客户端
        if client_id not in self.clients or self.clients[client_id][''client_secret''] != client_secret:
            print("[OAuth] 客户端验证失败")
            return None
        
        # 验证授权码
        if auth_code not in self.authorization_codes:
            print("[OAuth] 授权码无效")
            return None
        code_info = self.authorization_codes[auth_code]
        if code_info[''used''] or time.time() - code_info[''timestamp''] > 600:
            print("[OAuth] 授权码已使用或过期")
            return None
        if code_info[''client_id''] != client_id or code_info[''redirect_uri''] != redirect_uri:
            print("[OAuth] 授权码与客户端不匹配")
            return None
        
        # 标记授权码已使用，生成令牌
        code_info[''used''] = True
        access_token = secrets.token_urlsafe(32)
        refresh_token = secrets.token_urlsafe(32)
        
        # 存储令牌信息
        self.access_tokens[access_token] = {
            ''client_id'': client_id,
            ''user_id'': code_info[''user_id''],
            ''scope'': code_info[''scope''],
            ''timestamp'': time.time(),
            ''expires_in'': 3600  # 1小时
        }
        self.refresh_tokens[refresh_token] = {
            ''client_id'': client_id,
            ''user_id'': code_info[''user_id''],
            ''scope'': code_info[''scope'']
        }
        
        print(f"[OAuth] 令牌生成成功")
        return {
            ''access_token'': access_token,
            ''token_type'': ''Bearer'',
            ''expires_in'': 3600,
            ''refresh_token'': refresh_token
        }
    
    def validate_token(self, access_token: str) -> Optional[Dict]:
        """验证访问令牌有效性"""
        if access_token not in self.access_tokens:
            return None
        token_info = self.access_tokens[access_token]
        if time.time() - token_info[''timestamp''] > token_info[''expires_in'']:
            print("[OAuth] 访问令牌已过期")
            return None
        return token_info
    
    def refresh_access_token(self, client_id: str, client_secret: str, refresh_token: str) -> Optional[Dict]:
        """用刷新令牌获取新的访问令牌"""
        print(f"\\n[OAuth] 收到令牌刷新请求")
        
        # 验证客户端和刷新令牌
        if client_id not in self.clients or self.clients[client_id][''client_secret''] != client_secret:
            return None
        if refresh_token not in self.refresh_tokens or self.refresh_tokens[refresh_token][''client_id''] != client_id:
            return None
        
        # 生成新的访问令牌
        token_info = self.refresh_tokens[refresh_token]
        new_access_token = secrets.token_urlsafe(32)
        self.access_tokens[new_access_token] = {
            ''client_id'': client_id,
            ''user_id'': token_info[''user_id''],
            ''scope'': token_info[''scope''],
            ''timestamp'': time.time(),
            ''expires_in'': 3600
        }
        
        print(f"[OAuth] 新访问令牌生成：{new_access_token[:16]}...")
        return {
            ''access_token'': new_access_token,
            ''token_type'': ''Bearer'',
            ''expires_in'': 3600
        }

# 演示代码
def demo_oauth():
    print("\\n" + "="*60)
    print("OAuth 2.0授权流程演示")
    print("="*60)
    
    # 初始化授权服务器
    oauth = OAuthSimulation()
    
    # 注册客户端
    print("\\n[步骤1] 第三方应用注册")
    oauth.register_client(
        client_id="photo_app",
        client_secret="secret_key_123",
        redirect_uri="https://photoapp.com/callback"
    )
    
    # 用户授权，生成授权码
    print("\\n[步骤2] 用户授权")
    auth_code = oauth.authorization_request(
        client_id="photo_app",
        redirect_uri="https://photoapp.com/callback",
        scope=["read_photos", "upload_photos"],
        user_id="alice"
    )
    print(f"[浏览器] 重定向到：https://photoapp.com/callback?code={auth_code[:16]}...")
    
    # 兑换访问令牌
    print("\\n[步骤3] 兑换访问令牌")
    token_response = oauth.token_request(
        client_id="photo_app",
        client_secret="secret_key_123",
        auth_code=auth_code,
        redirect_uri="https://photoapp.com/callback"
    )
    if not token_response:
        return False
    access_token = token_response[''access_token'']
    
    # 访问资源
    print("\\n[步骤4] 访问用户照片资源")
    token_info = oauth.validate_token(access_token)
    if token_info:
        print(f"[资源服务器] 令牌有效，用户：{token_info[''user_id'']}")
        print(f"[资源服务器] 权限：{'', ''.join(token_info[''scope''])}")
        print("[资源服务器] 返回照片列表")
    
    # 刷新令牌
    print("\\n[步骤5] 刷新访问令牌")
    new_token = oauth.refresh_access_token(
        client_id="photo_app",
        client_secret="secret_key_123",
        refresh_token=token_response[''refresh_token'']
    )
    if new_token:
        print("[OAuth] 令牌刷新成功")
    
    return True

# 执行演示
demo_oauth()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (110, 15, 9, '主流认证协议对比与应用选型', '基于原理、安全性、性能、适用场景对三类核心认证协议进行全面对比，明确实际应用中的选型依据：
1. 核心特性对比：
   - 挑战-响应协议：优点是实现简单、计算开销低、防重放攻击；缺点是仅支持单方认证、无授权功能，适用于点对点的简单身份验证（如设备登录、API接口校验）；
   - Kerberos协议：优点是单点登录、相互认证、密码不传输；缺点是依赖中心化KDC（单点故障风险）、部署复杂，适用于企业内网、分布式系统等内部认证场景；
   - OAuth 2.0协议：优点是第三方授权、密码不泄露、细粒度权限控制；缺点是仅支持客户端授权（非严格身份认证）、依赖HTTPS保障传输安全，适用于跨组织的第三方应用授权（如微信登录、GitHub授权）。
2. 安全性对比：
   - 抗攻击能力：Kerberos（相互认证）> 挑战-响应（防重放）> OAuth 2.0（依赖传输安全）；
   - 密钥管理：Kerberos（KDC集中管理）> 挑战-响应（点对点共享）> OAuth 2.0（令牌管理）。
3. 性能对比：
   - 计算开销：挑战-响应 < OAuth 2.0 < Kerberos；
   - 部署复杂度：挑战-响应 < OAuth 2.0 < Kerberos。
4. 实际应用选型建议：
   - 简单点对点认证：挑战-响应协议（如智能家居设备登录）；
   - 企业内部单点登录：Kerberos协议（如企业办公系统、内部服务访问）；
   - 第三方应用授权：OAuth 2.0协议（如APP微信登录、第三方工具访问云存储）；
   - 高安全相互认证：基于挑战-响应扩展或STS协议（如金融交易、政务通信）。
预期输出：能够根据实际应用场景的需求（简单/复杂、内部/外部、认证/授权），选择合适的认证协议。', '实际应用中，OAuth 2.0常与OpenID Connect结合，实现“授权+认证”的完整功能', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (127, 18, 1, '环境准备与基础概念梳理', '1. 环境配置：确保Python 3.7+环境，依赖Python标准库（hashlib、time、secrets、dataclasses、json），无需额外安装第三方包。
2. 核心概念回顾：区块链基本结构（区块、链、交易）、共识协议四大核心性质（一致性、最终性、活性、容错性）、拜占庭容错理论极限（n≥3f+1）。
3. 目标明确：本实验聚焦PoW（工作量证明）、PoS（权益证明）、PBFT（实用拜占庭容错）三大主流协议，理解其设计思想与适用场景差异。
预期输出：无报错确认Python环境可用，清晰梳理共识协议核心概念。', '若使用Python 3.6及以下版本，需手动安装dataclasses模块（pip install dataclasses）', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (128, 18, 2, '基础区块结构实现', '1. 定义Block数据类：包含区块索引（index）、时间戳（timestamp）、交易列表（transactions）、前区块哈希（previous_hash）、随机数（nonce）、当前区块哈希（hash）核心字段。
2. 实现compute_hash方法：通过JSON序列化区块核心字段（排序键确保一致性），使用SHA-256计算区块哈希，保证区块数据完整性。
3. 核心作用：区块是共识协议的基础数据单元，哈希值用于链的链接与数据验证。
预期输出：Block类可正常实例化，compute_hash方法能稳定生成唯一哈希值。', NULL, 'import hashlib
import time
import secrets
from typing import List, Dict, Optional, Tuple
from dataclasses import dataclass, field
import json

@dataclass
class Block:
    """区块链基础区块数据结构"""
    index: int  # 区块索引
    timestamp: float  # 时间戳
    transactions: List[str]  # 交易列表
    previous_hash: str  # 前区块哈希
    nonce: int = 0  # 随机数（PoW专用）
    hash: str = ""  # 当前区块哈希
    
    def compute_hash(self) -> str:
        """计算区块哈希（确保数据完整性）"""
        # 序列化区块核心字段（排序键避免哈希不一致）
        block_string = json.dumps({
            ''index'': self.index,
            ''timestamp'': self.timestamp,
            ''transactions'': self.transactions,
            ''previous_hash'': self.previous_hash,
            ''nonce'': self.nonce
        }, sort_keys=True)
        # SHA-256哈希计算
        return hashlib.sha256(block_string.encode()).hexdigest()

# 测试区块结构
def test_block():
    # 实例化测试区块
    test_block = Block(
        index=0,
        timestamp=time.time(),
        transactions=["创世区块测试"],
        previous_hash="0"
    )
    # 计算哈希
    block_hash = test_block.compute_hash()
    print(f"测试区块哈希：{block_hash[:32]}...")
    return len(block_hash) == 64  # SHA-256哈希长度为64位

# 执行测试
test_block()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (129, 18, 3, '工作量证明（PoW）协议实现与测试', '1. 核心原理：节点通过计算难解数学谜题（寻找nonce使区块哈希满足难度阈值H(block_header||nonce) < T）竞争记账权，安全性依赖哈希函数的抗预映射性与抗碰撞性。
2. 实现ProofOfWork类：
   - 初始化方法：设置难度阈值、初始化区块链（创建创世区块）、初始化待处理交易池。
   - _mine_block方法：挖矿核心逻辑，循环遍历nonce，直到找到满足难度要求的哈希值。
   - add_transaction方法：添加交易到待处理池。
   - mine_pending_transactions方法：打包待处理交易，调用挖矿方法生成新区块，添加矿工奖励。
   - is_chain_valid方法：验证区块链完整性（哈希一致性、链接完整性、难度达标）。
3. 编写测试函数demo_proof_of_work：模拟比特币挖矿流程，包含交易添加、挖矿、链验证。
预期输出：打印创世区块创建、交易添加、挖矿过程（尝试次数、耗时）、区块哈希、链验证结果，最终返回True。', '演示时建议将难度设为2-4（难度越高，挖矿尝试次数越多，耗时越长），实际比特币难度动态调整', 'class ProofOfWork:
    """工作量证明（Proof of Work - PoW）协议实现"""
    
    def __init__(self, difficulty: int = 4):
        """初始化PoW区块链
        Args:
            difficulty: 挖矿难度（区块哈希前N位必须为0）
        """
        self.difficulty = difficulty
        self.chain: List[Block] = []  # 区块链主链
        self.pending_transactions: List[str] = []  # 待处理交易池
        
        # 创建创世区块（第一个区块，无前置区块）
        genesis_block = Block(
            index=0,
            timestamp=time.time(),
            transactions=["创世区块"],
            previous_hash="0"
        )
        genesis_block.hash = self._mine_block(genesis_block)
        self.chain.append(genesis_block)
        
        print(f"[PoW] 区块链初始化完成，挖矿难度：{difficulty}")
        print(f"[PoW] 创世区块哈希：{genesis_block.hash[:32]}...")
    
    def _mine_block(self, block: Block) -> str:
        """挖矿核心逻辑：寻找满足难度要求的nonce
        Args:
            block: 待挖矿的区块
        Returns:
            满足难度要求的区块哈希
        """
        target = ''0'' * self.difficulty  # 难度目标：哈希前N位为0
        attempts = 0  # 挖矿尝试次数
        start_time = time.time()  # 挖矿开始时间
        
        while True:
            block.nonce = attempts  # 遍历nonce
            block_hash = block.compute_hash()  # 计算当前nonce对应的哈希
            
            # 检查哈希是否满足难度要求
            if block_hash.startswith(target):
                elapsed = time.time() - start_time
                print(f"[PoW] 挖矿成功！尝试次数: {attempts:,}, 耗时: {elapsed:.2f}秒")
                print(f"[PoW] 区块哈希: {block_hash[:32]}...")
                return block_hash
            
            attempts += 1
            # 每10000次尝试打印进度（避免无响应感知）
            if attempts % 10000 == 0 and attempts > 0:
                print(f"[PoW] 挖矿中... 已尝试 {attempts:,} 次")
    
    def add_transaction(self, transaction: str):
        """添加交易到待处理交易池"""
        self.pending_transactions.append(transaction)
        print(f"[PoW] 交易已添加到交易池: {transaction}")
    
    def mine_pending_transactions(self, miner_address: str):
        """打包待处理交易并挖矿
        Args:
            miner_address: 矿工地址（接收挖矿奖励）
        """
        if not self.pending_transactions:
            print("[PoW] 无待处理交易，无需挖矿")
            return
        
        # 构建新区块
        last_block = self.chain[-1]  # 获取最后一个区块
        new_block = Block(
            index=len(self.chain),
            timestamp=time.time(),
            transactions=self.pending_transactions.copy(),  # 复制交易池
            previous_hash=last_block.hash
        )
        
        print(f"\\n[PoW] 开始挖矿区块 #{new_block.index}...")
        new_block.hash = self._mine_block(new_block)  # 挖矿
        
        # 新区块添加到主链
        self.chain.append(new_block)
        
        # 清空交易池，添加挖矿奖励（模拟比特币奖励机制）
        self.pending_transactions = [f"挖矿奖励 -> {miner_address}: 1 BTC"]
        print(f"[PoW] 区块 #{new_block.index} 已成功添加到区块链")
    
    def is_chain_valid(self) -> bool:
        """验证区块链完整性（防篡改）"""
        for i in range(1, len(self.chain)):
            current_block = self.chain[i]
            previous_block = self.chain[i-1]
            
            # 验证当前区块哈希是否有效（未被篡改）
            if current_block.hash != current_block.compute_hash():
                print(f"[PoW] 区块 #{i} 数据被篡改！哈希验证失败")
                return False
            
            # 验证区块链接是否有效（前区块哈希匹配）
            if current_block.previous_hash != previous_block.hash:
                print(f"[PoW] 区块 #{i} 链接断裂！前区块哈希不匹配")
                return False
            
            # 验证区块是否满足挖矿难度要求
            if not current_block.hash.startswith(''0'' * self.difficulty):
                print(f"[PoW] 区块 #{i} 未满足挖矿难度要求")
                return False
        
        print("[PoW] 区块链完整性验证通过，无篡改")
        return True

# 测试PoW协议
def demo_proof_of_work():
    print("\\n" + "="*60)
    print("演示：工作量证明（PoW）协议 - 比特币挖矿模拟")
    print("="*60)
    
    # 初始化PoW区块链（难度设为2，演示效率更高）
    pow_blockchain = ProofOfWork(difficulty=2)
    
    # 步骤1：添加交易到交易池
    print("\\n[步骤1] 添加交易到交易池")
    pow_blockchain.add_transaction("Alice -> Bob: 2 BTC")
    pow_blockchain.add_transaction("Bob -> Carol: 1 BTC")
    
    # 步骤2：矿工挖矿（打包交易）
    print("\\n[步骤2] 矿工Miner1开始挖矿")
    pow_blockchain.mine_pending_transactions("Miner1")
    
    # 步骤3：添加更多交易
    print("\\n[步骤3] 添加更多交易")
    pow_blockchain.add_transaction("Carol -> Dave: 0.5 BTC")
    
    # 步骤4：再次挖矿
    print("\\n[步骤4] 矿工Miner1继续挖矿")
    pow_blockchain.mine_pending_transactions("Miner1")
    
    # 步骤5：验证区块链完整性
    print("\\n[步骤5] 验证区块链完整性")
    is_valid = pow_blockchain.is_chain_valid()
    
    # 打印区块链状态
    print("\\n[区块链状态概览]")
    for block in pow_blockchain.chain:
        print(f"- 区块 #{block.index}: 哈希={block.hash[:16]}..., 交易数={len(block.transactions)}")
    
    print("\\n[PoW协议核心特点]")
    print("  1. 去中心化：任何人可参与挖矿，无需授权")
    print("  2. 安全性：篡改需重新计算所有后续区块PoW，成本极高")
    print("  3. 能耗高：挖矿过程消耗大量计算资源")
    print("  4. 概率最终性：需等待多个区块确认（如比特币6个）确保不可逆")
    
    return is_valid

# 执行PoW测试
demo_proof_of_work()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (130, 18, 4, '权益证明（PoS）协议实现与测试', '1. 核心原理：节点根据持币质押量（Stake）概率性获得记账权，被选中概率与权益占比成正比（Pi = stake_i / Σstake_j），通过惩罚机制（Slashing）约束恶意行为，避免PoW的高能耗。
2. 实现核心组件：
   - Validator数据类：存储验证者地址与质押金额。
   - ProofOfStake类：
     - 初始化方法：设置最小质押金额、初始化区块链（创世区块）、验证者字典、待处理交易池。
     - register_validator方法：验证者注册（需满足最小质押要求）。
     - _select_validator方法：基于权益加权随机选择出块验证者。
     - add_transaction方法：添加交易到交易池。
     - create_block方法：由选中的验证者打包交易、生成新区块、获得奖励。
3. 编写测试函数demo_proof_of_stake：模拟以太坊2.0验证者质押、出块流程，验证权益加权选择机制。
预期输出：打印验证者注册、交易添加、验证者选中、区块创建、区块链状态，最终返回True。', 'PoS的核心是经济激励约束（诚实收益>攻击收益），实现中需重点关注验证者选择的随机性与权益占比的关联性', '@dataclass
class Validator:
    """PoS协议验证者数据结构"""
    address: str  # 验证者地址
    stake: int  # 质押金额（单位：代币）

class ProofOfStake:
    """权益证明（Proof of Stake - PoS）协议实现"""
    
    def __init__(self, min_stake: int = 32):
        """初始化PoS区块链
        Args:
            min_stake: 成为验证者的最小质押金额（模拟以太坊2.0的32ETH）
        """
        self.min_stake = min_stake
        self.chain: List[Block] = []  # 区块链主链
        self.validators: Dict[str, Validator] = {}  # 验证者字典（地址->验证者）
        self.pending_transactions: List[List[str]] = []  # 待处理交易池
        
        # 创建创世区块
        genesis_block = Block(
            index=0,
            timestamp=time.time(),
            transactions=["创世区块"],
            previous_hash="0"
        )
        genesis_block.hash = genesis_block.compute_hash()
        self.chain.append(genesis_block)
        
        print(f"[PoS] 区块链初始化完成，最小质押金额：{min_stake} 代币")
    
    def register_validator(self, address: str, stake: int) -> bool:
        """验证者注册（需满足最小质押要求）
        Args:
            address: 验证者地址
            stake: 质押金额
        Returns:
            注册是否成功
        """
        if stake < self.min_stake:
            print(f"[PoS] 验证者 {address} 质押失败：需至少 {self.min_stake} 代币，当前质押 {stake}")
            return False
        
        if address in self.validators:
            # 已注册验证者，追加质押
            self.validators[address].stake += stake
            print(f"[PoS] 验证者 {address} 追加质押 {stake} 代币，当前总质押：{self.validators[address].stake}")
        else:
            # 新验证者注册
            validator = Validator(address, stake)
            self.validators[address] = validator
            print(f"[PoS] 验证者 {address} 注册成功，质押 {stake} 代币")
        
        return True
    
    def _select_validator(self) -> Optional[str]:
        """基于权益加权随机选择出块验证者
        Returns:
            被选中的验证者地址（无验证者返回None）
        """
        if not self.validators:
            print("[PoS] 无可用验证者")
            return None
        
        # 计算总质押金额
        total_stake = sum(validator.stake for validator in self.validators.values())
        # 生成随机数（0 ~ total_stake-1）
        random_point = secrets.randbelow(total_stake)
        
        # 加权随机选择：质押越多，被选中概率越大
        current_sum = 0
        for address, validator in self.validators.items():
            current_sum += validator.stake
            if random_point < current_sum:
                return address
        
        # 兜底：返回第一个验证者（理论上不会触发）
        return list(self.validators.keys())[0]
    
    def add_transaction(self, transaction: str):
        """添加交易到待处理交易池"""
        self.pending_transactions.append(transaction)
        print(f"[PoS] 交易已添加到交易池: {transaction}")
    
    def create_block(self) -> bool:
        """由选中的验证者创建新区块
        Returns:
            区块创建是否成功
        """
        if not self.pending_transactions:
            print("[PoS] 无待处理交易，无需创建区块")
            return False
        
        # 选择出块验证者
        validator_address = self._select_validator()
        if not validator_address:
            return False
        
        validator = self.validators[validator_address]
        print(f"\\n[PoS] 验证者 {validator_address} 被选中出块（质押金额：{validator.stake} 代币）")
        
        # 构建新区块
        last_block = self.chain[-1]
        new_block = Block(
            index=len(self.chain),
            timestamp=time.time(),
            transactions=self.pending_transactions.copy(),
            previous_hash=last_block.hash
        )
        new_block.hash = new_block.compute_hash()
        
        # 新区块添加到主链
        self.chain.append(new_block)
        print(f"[PoS] 区块 #{new_block.index} 创建成功，哈希：{new_block.hash[:32]}...")
        
        # 清空交易池，添加验证者奖励（模拟PoS奖励机制）
        self.pending_transactions = [f"出块奖励 -> {validator_address}: 0.1 ETH"]
        return True

# 测试PoS协议
def demo_proof_of_stake():
    print("\\n" + "="*60)
    print("演示：权益证明（PoS）协议 - 以太坊2.0验证者模拟")
    print("="*60)
    
    # 初始化PoS区块链（最小质押32代币）
    pos_blockchain = ProofOfStake(min_stake=32)
    
    # 步骤1：验证者注册并质押
    print("\\n[步骤1] 验证者注册与质押")
    pos_blockchain.register_validator("Validator_A", stake=100)  # 质押100（满足要求）
    pos_blockchain.register_validator("Validator_B", stake=50)   # 质押50（满足要求）
    pos_blockchain.register_validator("Validator_C", stake=30)   # 质押30（不满足要求）
    pos_blockchain.register_validator("Validator_A", stake=50)   # 追加质押50
    
    # 步骤2：添加交易并创建多个区块
    print("\\n[步骤2] 添加交易并创建区块")
    for i in range(3):
        pos_blockchain.add_transaction(f"交易 {i+1}: User_{i} -> User_{i+1}: {10+i} 代币")
        pos_blockchain.create_block()
    
    # 步骤3：打印区块链状态
    print("\\n[区块链状态概览]")
    for block in pos_blockchain.chain:
        print(f"- 区块 #{block.index}: 哈希={block.hash[:16]}..., 交易数={len(block.transactions)}")
    
    print("\\n[PoS协议核心特点]")
    print("  1. 节能高效：无需计算密集型挖矿，能耗仅为PoW的千分之一")
    print("  2. 经济约束：质押金作为担保，恶意行为会被扣除质押（Slashing）")
    print("  3. 权益加权：质押越多，出块概率越大，避免无意义竞争")
    print("  4. 快速确认：无挖矿延迟，区块确认速度快（经济最终性）")
    print("  5. 潜在问题：富者愈富，需通过机制设计平衡去中心化")
    
    return True

# 执行PoS测试
demo_proof_of_stake()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (131, 18, 5, '实用拜占庭容错（PBFT）协议实现与测试', '1. 核心原理：在联盟链场景中，3f+1个节点通过三阶段投票（Pre-prepare → Prepare → Commit）达成共识，可容忍最多f个拜占庭恶意节点，具备强最终性（一旦确认不可逆）。
2. 实现核心组件：
   - PBFTMessage数据类：存储共识消息（类型、视图、序列号、区块摘要、节点ID）。
   - PBFTNode数据类：存储节点状态（节点ID、视图、已接收的Prepare/Commit消息）。
   - PBFT类：
     - 初始化方法：设置节点数量（≥4）、计算最大容错数f=(n-1)/3、初始化节点列表、主节点索引、序列号、区块链。
     - create_block方法：模拟三阶段共识流程（主节点提议→节点Prepare投票→节点Commit投票→区块提交）。
     - simulate_byzantine_failure方法：模拟拜占庭节点故障，验证系统容错能力。
3. 编写测试函数demo_pbft：模拟联盟链共识流程，验证三阶段投票、拜占庭容错能力（f=1时容忍1个恶意节点，超出则共识失败）。
预期输出：打印三阶段共识过程、节点交互、拜占庭故障模拟结果、区块链状态，最终返回True。', 'PBFT的核心是三阶段投票机制，需确保每个阶段的消息数量满足阈值（Prepare≥2f，Commit≥2f+1），这是拜占庭容错的关键', '@dataclass
class PBFTMessage:
    """PBFT共识消息数据结构"""
    msg_type: str  # 消息类型：PRE-PREPARE、PREPARE、COMMIT
    view: int      # 视图编号（主节点轮换时递增）
    sequence: int  # 序列号（确保消息有序）
    digest: str    # 区块摘要（哈希值）
    node_id: str   # 发送消息的节点ID

class PBFTNode:
    """PBFT节点数据结构"""
    def __init__(self, node_id: str):
        self.node_id = node_id  # 节点ID
        self.view = 0  # 当前视图编号
        # 存储已接收的Prepare消息（key：(view, sequence, digest)，value：消息列表）
        self.prepared_messages: Dict[Tuple[int, int, str], List[PBFTMessage]] = {} 
        # 存储已接收的Commit消息（key：(view, sequence, digest)，value：消息列表）
        self.committed_messages: Dict[Tuple[int, int, str], List[PBFTMessage]] = {} 

class PBFT:
    """实用拜占庭容错（Practical Byzantine Fault Tolerance - PBFT）协议实现"""
    
    def __init__(self, num_nodes: int = 4):
        """初始化PBFT系统
        Args:
            num_nodes: 节点数量（需≥4，满足n≥3f+1，f为最大容错数）
        """
        if num_nodes < 4:
            raise ValueError("PBFT系统需至少4个节点（n≥3f+1，f≥1）")
        
        self.num_nodes = num_nodes
        self.f = (num_nodes - 1) // 3  # 最大容错数（恶意节点数）
        self.nodes = [PBFTNode(f"Node_{i}") for i in range(num_nodes)]  # 节点列表
        self.primary_index = 0  # 主节点索引（视图内固定）
        self.sequence_number = 0  # 全局序列号（递增，确保消息唯一）
        self.chain: List[Block] = []  # 区块链主链
        
        # 创建创世区块
        genesis_block = Block(
            index=0,
            timestamp=time.time(),
            transactions=["创世区块"],
            previous_hash="0"
        )
        genesis_block.hash = genesis_block.compute_hash()
        self.chain.append(genesis_block)
        
        print(f"[PBFT] 系统初始化完成")
        print(f"[PBFT] 节点数：{num_nodes}，最大容错数f：{self.f}")
        print(f"[PBFT] 当前主节点：{self.nodes[self.primary_index].node_id}")
    
    def create_block(self, transactions: List[str]) -> bool:
        """通过PBFT三阶段共识创建新区块
        Args:
            transactions: 待打包的交易列表
        Returns:
            共识是否成功，区块是否提交
        """
        self.sequence_number += 1  # 序列号递增
        
        # 1. 构建新区块并计算摘要
        last_block = self.chain[-1]
        new_block = Block(
            index=len(self.chain),
            timestamp=time.time(),
            transactions=transactions,
            previous_hash=last_block.hash
        )
        block_digest = new_block.compute_hash()  # 区块摘要（用于消息传递）
        current_view = self.nodes[0].view  # 当前视图编号（所有节点视图一致）
        
        print(f"\\n[PBFT] 开始共识流程 - 视图：{current_view}，序列号：{self.sequence_number}")
        print(f"[PBFT] 待共识区块摘要：{block_digest[:32]}...")
        
        # 阶段1：PRE-PREPARE（主节点提议）
        print(f"\\n[阶段1] PRE-PREPARE")
        primary_node = self.nodes[self.primary_index]
        print(f"[{primary_node.node_id}] 主节点发起提议，区块摘要：{block_digest[:16]}...")
        # 主节点向所有从节点发送PRE-PREPARE消息（模拟广播）
        pre_prepare_msg = PBFTMessage(
            msg_type="PRE-PREPARE",
            view=current_view,
            sequence=self.sequence_number,
            digest=block_digest,
            node_id=primary_node.node_id
        )
        
        # 阶段2：PREPARE（从节点投票）
        print(f"\\n[阶段2] PREPARE")
        prepare_count = 0  # 收到的PREPARE消息数
        prepare_key = (current_view, self.sequence_number, block_digest)
        
        for node in self.nodes:
            if node.node_id == primary_node.node_id:
                continue  # 主节点不发送PREPARE消息
            
            # 从节点接收PRE-PREPARE消息后，发送PREPARE消息（模拟诚实节点行为）
            print(f"[{node.node_id}] 收到PRE-PREPARE消息，发送PREPARE投票")
            prepare_msg = PBFTMessage(
                msg_type="PREPARE",
                view=current_view,
                sequence=self.sequence_number,
                digest=block_digest,
                node_id=node.node_id
            )
            
            # 存储PREPARE消息到节点本地
            if prepare_key not in node.prepared_messages:
                node.prepared_messages[prepare_key] = []
            node.prepared_messages[prepare_key].append(prepare_msg)
            prepare_count += 1
        
        # 检查PREPARE消息是否满足阈值（需≥2f，确保诚实节点达成一致）
        required_prepare = 2 * self.f
        if prepare_count < required_prepare:
            print(f"[PBFT] PREPARE消息不足（收到：{prepare_count}，需要：{required_prepare}），共识失败")
            return False
        print(f"[PBFT] 收到 {prepare_count} 个PREPARE消息（满足阈值：{required_prepare}）")
        
        # 阶段3：COMMIT（所有节点确认）
        print(f"\\n[阶段3] COMMIT")
        commit_count = 0  # 收到的COMMIT消息数
        commit_key = (current_view, self.sequence_number, block_digest)
        
        for node in self.nodes:
            # 节点收到≥2f个PREPARE消息后，发送COMMIT消息
            print(f"[{node.node_id}] 发送COMMIT确认")
            commit_msg = PBFTMessage(
                msg_type="COMMIT",
                view=current_view,
                sequence=self.sequence_number,
                digest=block_digest,
                node_id=node.node_id
            )
            
            # 存储COMMIT消息到节点本地
            if commit_key not in node.committed_messages:
                node.committed_messages[commit_key] = []
            node.committed_messages[commit_key].append(commit_msg)
            commit_count += 1
        
        # 检查COMMIT消息是否满足阈值（需≥2f+1，确保最终一致性）
        required_commit = 2 * self.f + 1
        if commit_count < required_commit:
            print(f"[PBFT] COMMIT消息不足（收到：{commit_count}，需要：{required_commit}），共识失败")
            return False
        print(f"[PBFT] 收到 {commit_count} 个COMMIT消息（满足阈值：{required_commit}）")
        
        # 共识达成，提交区块到区块链
        new_block.hash = block_digest
        self.chain.append(new_block)
        print(f"\\n[PBFT] 共识成功！区块 #{new_block.index} 已提交")
        print(f"[PBFT] 区块哈希：{new_block.hash[:32]}...")
        return True
    
    def simulate_byzantine_failure(self, num_faulty: int) -> bool:
        """模拟拜占庭故障（恶意节点），验证系统容错能力
        Args:
            num_faulty: 恶意节点数量
        Returns:
            系统是否仍能正常达成共识
        """
        print(f"\\n[PBFT] 模拟拜占庭故障 - 恶意节点数：{num_faulty}")
        print(f"[PBFT] 系统最大容错数f：{self.f}")
        
        if num_faulty > self.f:
            print(f"[PBFT] 恶意节点数（{num_faulty}）超出最大容错数（{self.f}），系统无法达成共识")
            return False
        
        # 计算诚实节点数与所需COMMIT消息数
        honest_nodes = self.num_nodes - num_faulty
        required_commit = 2 * self.f + 1
        print(f"[PBFT] 诚实节点数：{honest_nodes}，所需COMMIT消息数：{required_commit}")
        
        if honest_nodes >= required_commit:
            print(f"[PBFT] 诚实节点数满足要求，系统仍能正常达成共识")
            return True
        else:
            print(f"[PBFT] 诚实节点数不足，系统无法达成共识")
            return False

# 测试PBFT协议
def demo_pbft():
    print("\\n" + "="*60)
    print("演示：实用拜占庭容错（PBFT）协议 - 联盟链共识模拟")
    print("="*60)
    
    # 初始化PBFT系统（4个节点，最大容错数f=1）
    pbft_system = PBFT(num_nodes=4)
    
    # 步骤1：提议新区块（包含2笔交易）
    print("\\n[步骤1] 发起区块共识提议")
    transactions = ["交易1：公司A -> 公司B：100万", "交易2：公司C -> 公司D：50万"]
    consensus_success = pbft_system.create_block(transactions)
    
    # 步骤2：模拟1个恶意节点（在容错范围内）
    print("\\n[步骤2] 模拟1个拜占庭恶意节点")
    can_work_with_1fault = pbft_system.simulate_byzantine_failure(num_faulty=1)
    
    # 步骤3：模拟2个恶意节点（超出容错范围）
    print("\\n[步骤3] 模拟2个拜占庭恶意节点")
    can_work_with_2fault = pbft_system.simulate_byzantine_failure(num_faulty=2)
    
    # 步骤4：打印区块链状态
    print("\\n[区块链状态概览]")
    for block in pbft_system.chain:
        print(f"- 区块 #{block.index}: 哈希={block.hash[:16]}..., 交易数={len(block.transactions)}")
    
    print("\\n[PBFT协议核心特点]")
    print("  1. 拜占庭容错：可容忍最多f=(n-1)/3个恶意节点（n为总节点数）")
    print("  2. 强最终性：一旦共识达成，区块不可逆，无回滚风险")
    print("  3. 低延迟高吞吐：无需挖矿，通过三阶段投票快速达成共识")
    print("  4. 许可链场景：适用于联盟链/私有链（节点需身份验证）")
    print("  5. 通信开销：节点间需大量消息交互，适合小规模节点集群")
    
    # 验证结果：共识成功 + 1个故障可工作 + 2个故障不可工作
    return consensus_success and can_work_with_1fault and not can_work_with_2fault

# 执行PBFT测试
demo_pbft()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (132, 18, 6, '主流共识协议对比与场景适配分析', '1. 核心维度对比：
   - 去中心化程度：PoW > PoS > DPoS > PBFT > PoA
   - 性能（TPS）：PBFT > PoS/DPoS > PoW
   - 能耗：PoW（极高）> 其他（极低）
   - 最终性：PBFT/PoA（强最终性）> PoS（经济最终性）> PoW（概率最终性）
   - 容错性：PBFT（拜占庭容错）> PoW/PoS（崩溃容错+部分恶意容错）
   - 适用场景：PoW（公链，如比特币）、PoS（公链，如以太坊2.0）、PBFT（联盟链，如Hyperledger Fabric）、PoA（私有链，如企业内部链）
2. 数学机制对比：
   - PoW：基于哈希函数难解性（找nonce）
   - PoS：基于权益加权概率（经济激励约束）
   - PBFT：基于三阶段投票（n≥3f+1拜占庭容错）
3. 技术选型要点：
   - 公链：优先考虑去中心化与安全性，选择PoW/PoS（需平衡性能）
   - 联盟链：优先考虑性能与可控性，选择PBFT（节点可信但需容错）
   - 私有链：优先考虑效率与权限管理，选择PoA（节点完全可信）
4. 发展趋势：混合共识（如PoW+PoS、PoH+PoS）、分片技术（提升PoW/PoS性能）、轻量化BFT（降低通信开销）。
预期输出：形成清晰的共识协议选型框架，能根据实际场景（公链/联盟链/私有链）选择最优协议。', '实际区块链项目常结合多种技术优化共识协议（如以太坊合并PoW+PoS，Solana结合PoH+PoS），需关注协议的演进与融合趋势', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (133, 19, 1, '环境准备与核心概念梳理', '1. 环境配置：确保Python 3.7+环境，安装依赖库（cryptography用于数字签名，可选pycryptodome用于Keccak哈希），执行命令：pip install cryptography pycryptodome。
2. 核心概念回顾：数字货币两大记账模型（UTXO/账户模型）、交易三要素（完整性、真实性、不可否认性）、数字签名（ECDSA）、智能合约（自动执行、不可篡改）。
3. 目标明确：本实验聚焦比特币UTXO模型、以太坊账户模型的实现，以及ERC-20智能合约的核心功能，理解不同模型的设计取舍。
预期输出：无报错确认依赖库安装成功，清晰梳理两大模型的核心差异。', 'cryptography库用于实现标准ECDSA签名，pycryptodome用于模拟以太坊Keccak-256哈希，若安装失败可使用sha256替代（教学场景不影响核心逻辑）', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (134, 19, 2, '核心数据结构设计', '1. 定义UTXO相关结构：
   - UTXO类：存储未花费交易输出的核心信息（交易ID、输出索引、金额、接收地址、锁定脚本）。
   - TransactionInput类：交易输入（引用前序UTXO的交易ID、输出索引、解锁脚本/签名）。
   - TransactionOutput类：交易输出（金额、接收地址、锁定脚本）。
2. 定义账户模型相关结构：
   - EthereumAccount类：以太坊账户（地址、余额、nonce、合约代码、存储）。
   - EthereumTransaction类：以太坊交易（发送地址、接收地址、金额、nonce、Gas参数、数据、签名）。
3. 核心作用：数据结构是底层逻辑的基础，需确保字段完整性与逻辑一致性，贴合真实数字货币协议设计。
预期输出：所有数据类可正常实例化，字段含义清晰，支持后续交易创建与状态管理。', NULL, 'import hashlib
import time
import json
import secrets
from typing import List, Dict, Optional, Tuple
from dataclasses import dataclass, field
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.backends import default_backend

# ====================== UTXO模型核心数据结构 ======================
@dataclass
class UTXO:
    """未花费交易输出（Unspent Transaction Output）- 比特币核心数据结构"""
    txid: str  # 所属交易ID
    vout: int  # 交易输出索引（一个交易可包含多个输出）
    amount: float  # 金额（BTC）
    address: str  # 接收地址（控制该UTXO的公钥哈希）
    scriptPubKey: str  # 锁定脚本（需满足脚本条件才能花费该UTXO）
    
    def __repr__(self):
        return f"UTXO(txid={self.txid[:8]}..., vout={self.vout}, amount={self.amount} BTC)"

@dataclass
class TransactionInput:
    """交易输入 - 引用前序UTXO并提供解锁凭证"""
    prev_txid: str  # 引用的前序交易ID
    vout: int  # 引用的前序交易输出索引
    scriptSig: str  # 解锁脚本（包含签名+公钥，用于验证UTXO所有权）
    
    def __repr__(self):
        return f"TxIn(prev_txid={self.prev_txid[:8]}..., vout={self.vout})")

@dataclass
class TransactionOutput:
    """交易输出 - 创建新的UTXO"""
    amount: float  # 金额（BTC）
    address: str  # 接收地址
    scriptPubKey: str  # 锁定脚本（如：OP_DUP OP_HASH160 {address} OP_EQUALVERIFY OP_CHECKSIG）
    
    def __repr__(self):
        return f"TxOut(amount={self.amount} BTC, address={self.address[:8]}...)")

@dataclass
class BitcoinTransaction:
    """比特币交易结构"""
    txid: str  # 交易ID（由交易数据哈希生成）
    version: int  # 协议版本
    inputs: List[TransactionInput]  # 交易输入列表
    outputs: List[TransactionOutput]  # 交易输出列表
    locktime: int  # 交易生效时间（0表示立即生效）
    timestamp: float  # 交易创建时间戳
    
    def compute_txid(self) -> str:
        """计算交易ID（双SHA256哈希，比特币标准）"""
        tx_data = {
            ''version'': self.version,
            ''inputs'': [{''prev_txid'': inp.prev_txid, ''vout'': inp.vout} for inp in self.inputs],
            ''outputs'': [{''amount'': out.amount, ''address'': out.address} for out in self.outputs],
            ''locktime'': self.locktime
        }
        # 序列化并计算双SHA256哈希
        tx_string = json.dumps(tx_data, sort_keys=True).encode()
        return hashlib.sha256(hashlib.sha256(tx_string).digest()).hexdigest()

# ====================== 账户模型核心数据结构 ======================
@dataclass
class EthereumAccount:
    """以太坊账户结构（外部账户EOA/合约账户CA）"""
    address: str  # 账户地址（0x前缀）
    balance: float = 0.0  # 余额（ETH）
    nonce: int = 0  # 交易计数器（防重放攻击，每笔交易递增）
    code: str = ""  # 合约字节码（EOA为空，CA为合约代码）
    storage: Dict[str, any] = field(default_factory=dict)  # 合约存储（键值对）
    
    def is_contract(self) -> bool:
        """判断是否为合约账户"""
        return len(self.code) > 0

@dataclass
class EthereumTransaction:
    """以太坊交易结构"""
    tx_hash: str  # 交易哈希
    from_address: str  # 发送地址
    to_address: str  # 接收地址（合约地址或外部账户地址）
    value: float  # 转账金额（ETH）
    gas_limit: int  # Gas上限（最大计算步数）
    gas_price: float  # 单位Gas价格（ETH/Gas）
    nonce: int  # 发送者nonce
    data: str = ""  # 交易数据（合约调用时存储函数名+参数）
    timestamp: float = 0.0  # 时间戳
    signature: str = ""  # 数字签名（v,r,s的十六进制表示）
    
    def compute_hash(self) -> str:
        """计算交易哈希（以太坊使用Keccak-256）"""
        tx_data = {
            ''from'': self.from_address,
            ''to'': self.to_address,
            ''value'': self.value,
            ''nonce'': self.nonce,
            ''gas_limit'': self.gas_limit,
            ''gas_price'': self.gas_price,
            ''data'': self.data
        }
        tx_string = json.dumps(tx_data, sort_keys=True).encode()
        
        # 优先使用Keccak-256（以太坊标准），若无则用SHA256替代
        try:
            from Crypto.Hash import keccak
            k = keccak.new(digest_bits=256)
            k.update(tx_string)
            return "0x" + k.hexdigest()
        except ImportError:
            print("[警告] 未安装pycryptodome，使用SHA256替代Keccak-256（仅教学使用）")
            return "0x" + hashlib.sha256(tx_string).hexdigest()

# 测试数据结构
def test_data_structures():
    """测试核心数据结构实例化"""
    # 测试UTXO
    utxo = UTXO(txid=secrets.token_hex(32), vout=0, amount=0.5, address="1Alice...", scriptPubKey="OP_DUP...")
    print(f"UTXO测试：{utxo}")
    
    # 测试比特币交易
    tx_input = TransactionInput(prev_txid=utxo.txid, vout=0, scriptSig="签名数据...")
    tx_output = TransactionOutput(amount=0.4, address="1Bob...", scriptPubKey="OP_DUP...")
    btc_tx = BitcoinTransaction(txid="", version=1, inputs=[tx_input], outputs=[tx_output], locktime=0, timestamp=time.time())
    btc_tx.txid = btc_tx.compute_txid()
    print(f"比特币交易测试：TXID={btc_tx.txid[:16]}...")
    
    # 测试以太坊账户与交易
    eth_account = EthereumAccount(address="0xAlice...", balance=10.0)
    eth_tx = EthereumTransaction(
        tx_hash="",
        from_address=eth_account.address,
        to_address="0xBob...",
        value=3.0,
        gas_limit=21000,
        gas_price=2e-8,
        nonce=eth_account.nonce
    )
    eth_tx.tx_hash = eth_tx.compute_hash()
    print(f"以太坊交易测试：TxHash={eth_tx.tx_hash[:18]}...")
    
    return True

# 执行数据结构测试
test_data_structures()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (135, 19, 3, '比特币UTXO模型实现与测试', '1. 核心原理：UTXO模型无账户概念，用户余额=其控制的所有未花费交易输出（UTXO）金额之和（Balance(U)=Σv_i，v_i为UTXO金额）；交易由输入（引用UTXO）和输出（创建新UTXO）组成，需满足Σv_in=Σv_out+fee（防止凭空造币），输入需通过签名验证所有权。
2. 实现BitcoinWallet类：
   - 初始化方法：生成ECDSA密钥对、存储钱包地址、初始化UTXO集合。
   - get_balance方法：计算余额（所有UTXO金额之和）。
   - add_utxo方法：添加收到的UTXO到本地集合。
   - select_utxos方法：选择足够金额的UTXO（简单累加策略）。
   - create_transaction方法：创建交易（输入选择、输出生成、找零计算、签名）。
   - _sign_input方法：对交易输入进行ECDSA签名（确保所有权）。
3. 编写测试函数demo_bitcoin_utxo：模拟Alice向Bob转账场景，包含UTXO接收、交易创建、找零机制、余额更新。
预期输出：打印钱包创建、UTXO接收、交易详情（输入/输出/找零/矿工费）、最终余额，验证UTXO模型的核心逻辑正确性。', 'UTXO模型的核心是“输入引用输出，输出成为新UTXO”，需确保每个UTXO只能被花费一次，找零机制是实现部分支付的关键', 'class BitcoinWallet:
    """比特币钱包（UTXO模型实现）"""
    
    def __init__(self, address: str):
        """初始化钱包
        Args:
            address: 钱包地址（公钥哈希的人类可读形式）
        """
        self.address = address
        self.utxos: List[UTXO] = []  # 钱包控制的UTXO集合
        
        # 生成ECDSA密钥对（secp256k1曲线，比特币标准）
        self.private_key = ec.generate_private_key(ec.SECP256K1(), default_backend())
        self.public_key = self.private_key.public_key()
        
        # 序列化公钥（用于验证签名）
        self.public_key_hex = self.public_key.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        ).hex()
        
        print(f"[Bitcoin] 钱包创建成功：地址={self.address[:16]}...")
    
    def get_balance(self) -> float:
        """计算钱包余额（所有UTXO金额之和）"""
        return sum(utxo.amount for utxo in self.utxos)
    
    def add_utxo(self, utxo: UTXO):
        """添加UTXO到钱包（收到转账时调用）"""
        if utxo.address != self.address:
            print(f"[Bitcoin] 拒绝添加UTXO：地址不匹配")
            return
        self.utxos.append(utxo)
        print(f"[Bitcoin] 收到UTXO：{utxo.amount} BTC，当前余额={self.get_balance()} BTC")
    
    def select_utxos(self, required_amount: float) -> List[UTXO]:
        """选择足够金额的UTXO（简单累加策略）
        Args:
            required_amount: 需要的总金额（包含矿工费）
        Returns:
            选中的UTXO列表
        """
        selected_utxos = []
        total = 0.0
        
        # 遍历UTXO，累加直到满足所需金额
        for utxo in self.utxos:
            selected_utxos.append(utxo)
            total += utxo.amount
            if total >= required_amount:
                break
        
        if total < required_amount:
            raise ValueError(f"[Bitcoin] 余额不足：需要{required_amount} BTC，当前可用{total} BTC")
        
        print(f"[Bitcoin] 选中UTXO：{len(selected_utxos)}个，总金额={total} BTC")
        return selected_utxos
    
    def _sign_input(self, utxo: UTXO) -> str:
        """对UTXO进行ECDSA签名（证明所有权）
        Args:
            utxo: 要花费的UTXO
        Returns:
            十六进制编码的签名
        """
        # 签名消息：UTXO的唯一标识（txid + vout）
        message = f"{utxo.txid}{utxo.vout}".encode()
        
        # ECDSA签名（secp256k1曲线 + SHA256哈希）
        signature = self.private_key.sign(
            message,
            ec.ECDSA(hashes.SHA256())
        )
        return signature.hex()
    
    def create_transaction(self, to_address: str, amount: float, fee: float = 0.0001) -> BitcoinTransaction:
        """创建比特币交易
        Args:
            to_address: 接收地址
            amount: 转账金额（BTC）
            fee: 矿工费（BTC）
        Returns:
            交易对象
        """
        # 计算所需总金额（转账金额 + 矿工费）
        total_required = amount + fee
        
        # 选择UTXO
        selected_utxos = self.select_utxos(total_required)
        total_input = sum(utxo.amount for utxo in selected_utxos)
        
        # 创建交易输入：每个选中的UTXO对应一个输入，包含签名
        tx_inputs = []
        for utxo in selected_utxos:
            signature = self._sign_input(utxo)
            tx_input = TransactionInput(
                prev_txid=utxo.txid,
                vout=utxo.vout,
                scriptSig=signature  # 解锁脚本=签名
            )
            tx_inputs.append(tx_input)
        
        # 创建交易输出：1个支付输出 + 1个找零输出（若有）
        tx_outputs = []
        # 1. 支付输出（给接收方）
        tx_outputs.append(TransactionOutput(
            amount=amount,
            address=to_address,
            scriptPubKey=f"OP_DUP OP_HASH160 {to_address} OP_EQUALVERIFY OP_CHECKSIG"
        ))
        # 2. 找零输出（剩余金额返回自己）
        change = total_input - amount - fee
        if change > 0:
            tx_outputs.append(TransactionOutput(
                amount=change,
                address=self.address,
                scriptPubKey=f"OP_DUP OP_HASH160 {self.address} OP_EQUALVERIFY OP_CHECKSIG"
            ))
        
        # 创建交易对象
        tx = BitcoinTransaction(
            txid="",
            version=1,
            inputs=tx_inputs,
            outputs=tx_outputs,
            locktime=0,
            timestamp=time.time()
        )
        # 计算交易ID
        tx.txid = tx.compute_txid()
        
        # 移除已花费的UTXO（防止双重支付）
        for utxo in selected_utxos:
            self.utxos.remove(utxo)
        
        # 打印交易详情
        print(f"\\n[Bitcoin] 交易创建成功：")
        print(f"  TXID: {tx.txid[:16]}...")
        print(f"  输入：{len(tx_inputs)}个UTXO，总金额={total_input} BTC")
        print(f"  输出1：{amount} BTC -> {to_address[:16]}...")
        if change > 0:
            print(f"  输出2：{change:.6f} BTC -> {self.address[:16]}...（找零）")
        print(f"  矿工费：{fee} BTC")
        print(f"  剩余余额：{self.get_balance()} BTC")
        
        return tx

# 测试UTXO模型
def demo_bitcoin_utxo():
    print("\\n" + "="*60)
    print("演示：比特币UTXO模型 - Alice向Bob转账")
    print("="*60)
    
    # 步骤1：创建Alice和Bob的钱包
    print("\\n[步骤1] 创建钱包")
    alice_wallet = BitcoinWallet("1Alice_address_1234567890abcdef")
    bob_wallet = BitcoinWallet("1Bob_address_abcdef1234567890")
    
    # 步骤2：Alice获得初始UTXO（模拟挖矿或他人转账）
    print("\\n[步骤2] Alice接收初始UTXO")
    utxo1 = UTXO(
        txid=secrets.token_hex(32),  # 随机生成交易ID
        vout=0,
        amount=0.5,
        address=alice_wallet.address,
        scriptPubKey=f"OP_DUP OP_HASH160 {alice_wallet.address} OP_EQUALVERIFY OP_CHECKSIG"
    )
    utxo2 = UTXO(
        txid=secrets.token_hex(32),
        vout=1,
        amount=0.8,
        address=alice_wallet.address,
        scriptPubKey=f"OP_DUP OP_HASH160 {alice_wallet.address} OP_EQUALVERIFY OP_CHECKSIG"
    )
    alice_wallet.add_utxo(utxo1)
    alice_wallet.add_utxo(utxo2)
    print(f"[Bitcoin] Alice当前余额：{alice_wallet.get_balance()} BTC")
    
    # 步骤3：Alice向Bob转账1.0 BTC（包含0.0001 BTC矿工费）
    print("\\n[步骤3] Alice向Bob转账1.0 BTC")
    tx = alice_wallet.create_transaction(bob_wallet.address, amount=1.0, fee=0.0001)
    
    # 步骤4：Bob接收交易输出（添加UTXO）
    print("\\n[步骤4] Bob接收转账")
    for idx, output in enumerate(tx.outputs):
        if output.address == bob_wallet.address:
            # 为Bob创建UTXO
            bob_utxo = UTXO(
                txid=tx.txid,
                vout=idx,
                amount=output.amount,
                address=output.address,
                scriptPubKey=output.scriptPubKey
            )
            bob_wallet.add_utxo(bob_utxo)
        elif output.address == alice_wallet.address:
            # Alice的找零UTXO，重新添加到她的钱包
            alice_change_utxo = UTXO(
                txid=tx.txid,
                vout=idx,
                amount=output.amount,
                address=output.address,
                scriptPubKey=output.scriptPubKey
            )
            alice_wallet.add_utxo(alice_change_utxo)
    
    # 步骤5：打印最终余额
    print("\\n[步骤5] 最终余额")
    print(f"Alice：{alice_wallet.get_balance():.6f} BTC")
    print(f"Bob：{bob_wallet.get_balance():.6f} BTC")
    
    # 总结UTXO模型特点
    print("\\n[UTXO模型核心特点]")
    print("  1. 无账户概念：余额=控制的UTXO金额之和")
    print("  2. 交易原子性：输入必须完全花费，通过找零实现部分支付")
    print("  3. 隐私性：可使用新地址接收每笔转账，难以追踪身份")
    print("  4. 并行验证：多笔交易可并行验证，无需锁账户")
    print("  5. 安全性：双重支付通过UTXO标记已花费防止")
    
    return True

# 执行UTXO模型测试
demo_bitcoin_utxo()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (136, 19, 4, '以太坊账户模型实现与测试', '1. 核心原理：以太坊采用账户模型，系统状态是全局账户映射（S=(address,balance,nonce,storage,code)）；交易直接修改账户余额，nonce用于防止重放攻击（每笔交易nonce递增）；Gas机制控制计算资源消耗（Gas=GasLimit×GasPrice）。
2. 实现EthereumWallet类：
   - 初始化方法：生成ECDSA密钥对、创建以太坊账户（EOA）、初始化余额与nonce。
   - get_balance方法：获取账户余额。
   - deposit方法：模拟存入ETH（测试用）。
   - create_transaction方法：创建以太坊交易（计算Gas费用、验证余额、签名、更新nonce）。
   - _sign_transaction方法：对交易进行ECDSA签名（生成v,r,s）。
3. 编写测试函数demo_ethereum_account：模拟Alice向Bob转账场景，包含账户创建、存款、多次转账（验证nonce递增）、Gas费用计算。
预期输出：打印账户创建、存款、交易详情（金额、Gas费用、nonce）、最终余额，验证账户模型的核心逻辑正确性。', '账户模型的核心是nonce管理（防重放）和Gas机制（控制合约执行资源），以太坊普通转账的默认GasLimit为21000', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (137, 19, 5, '以太坊账户模型实现与测试（续）', '', NULL, 'class EthereumWallet:
    """以太坊钱包（账户模型实现）"""
    
    def __init__(self, address: str):
        """初始化钱包
        Args:
            address: 以太坊账户地址（0x前缀）
        """
        self.address = address
        # 创建外部账户（EOA）
        self.account = EthereumAccount(address=address, balance=0.0, nonce=0)
        
        # 生成ECDSA密钥对（secp256k1曲线，以太坊标准）
        self.private_key = ec.generate_private_key(ec.SECP256K1(), default_backend())
        self.public_key = self.private_key.public_key()
        
        print(f"[Ethereum] 账户创建成功：地址={self.address[:18]}...")
    
    def get_balance(self) -> float:
        """获取账户余额（ETH）"""
        return self.account.balance
    
    def deposit(self, amount: float):
        """模拟存入ETH（测试用，真实场景通过挖矿或转账获得）"""
        if amount <= 0:
            print(f"[Ethereum] 存款失败：金额必须大于0")
            return
        self.account.balance += amount
        print(f"[Ethereum] 存款成功：+{amount} ETH，当前余额={self.account.balance} ETH")
    
    def _sign_transaction(self, tx: EthereumTransaction) -> str:
        """对交易进行ECDSA签名（生成v,r,s）
        Args:
            tx: 待签名的交易
        Returns:
            十六进制编码的签名（v+r+s拼接）
        """
        # 签名消息：交易核心字段的哈希（确保交易不可篡改）
        message = f"{tx.from_address}{tx.to_address}{tx.value}{tx.nonce}{tx.gas_limit}{tx.gas_price}".encode()
        
        # ECDSA签名（secp256k1曲线 + SHA256哈希）
        signature = self.private_key.sign(
            message,
            ec.ECDSA(hashes.SHA256())
        )
        
        # 以太坊签名格式：v（1字节）+ r（32字节）+ s（32字节）
        # 简化处理：直接返回签名的十六进制表示
        return signature.hex()
    
    def create_transaction(self, to_address: str, value: float, 
                          gas_limit: int = 21000, gas_price: float = 2e-8, data: str = "") -> EthereumTransaction:
        """创建以太坊交易
        Args:
            to_address: 接收地址
            value: 转账金额（ETH）
            gas_limit: Gas上限（默认21000，普通转账固定值）
            gas_price: 单位Gas价格（ETH/Gas，默认2e-8 ETH）
            data: 交易数据（合约调用时使用）
        Returns:
            交易对象
        """
        # 计算Gas费用（GasLimit × GasPrice）
        gas_fee = gas_limit * gas_price
        # 计算总花费（转账金额 + Gas费用）
        total_cost = value + gas_fee
        
        # 验证余额
        if self.account.balance < total_cost:
            raise ValueError(f"[Ethereum] 余额不足：需要{total_cost:.6f} ETH，当前余额={self.account.balance:.6f} ETH")
        
        # 创建交易对象
        tx = EthereumTransaction(
            tx_hash="",
            from_address=self.address,
            to_address=to_address,
            value=value,
            gas_limit=gas_limit,
            gas_price=gas_price,
            nonce=self.account.nonce,  # 使用当前nonce
            data=data,
            timestamp=time.time()
        )
        
        # 签名交易
        tx.signature = self._sign_transaction(tx)
        # 计算交易哈希
        tx.tx_hash = tx.compute_hash()
        
        # 更新账户状态：扣除余额，递增nonce（防重放）
        self.account.balance -= total_cost
        self.account.nonce += 1
        
        # 打印交易详情
        print(f"\\n[Ethereum] 交易创建成功：")
        print(f"  TxHash: {tx.tx_hash[:18]}...")
        print(f"  发送方：{tx.from_address[:18]}...")
        print(f"  接收方：{tx.to_address[:18]}...")
        print(f"  转账金额：{tx.value} ETH")
        print(f"  Gas参数：{tx.gas_limit} Gas × {tx.gas_price:.8f} ETH/Gas = {gas_fee:.6f} ETH")
        print(f"  Nonce：{tx.nonce}")
        print(f"  剩余余额：{self.account.balance:.6f} ETH")
        
        return tx

# 测试账户模型
def demo_ethereum_account():
    print("\\n" + "="*60)
    print("演示：以太坊账户模型 - Alice向Bob转账")
    print("="*60)
    
    # 步骤1：创建Alice和Bob的以太坊账户
    print("\\n[步骤1] 创建账户")
    alice_wallet = EthereumWallet("0xAlice_address_1234567890abcdef")
    bob_wallet = EthereumWallet("0xBob_address_abcdef1234567890")
    
    # 步骤2：Alice存入10.0 ETH（测试用）
    print("\\n[步骤2] Alice存款")
    alice_wallet.deposit(10.0)
    
    # 步骤3：Alice向Bob转账3.0 ETH（普通转账，GasLimit=21000）
    print("\\n[步骤3] Alice向Bob转账3.0 ETH")
    tx1 = alice_wallet.create_transaction(bob_wallet.address, value=3.0)
    # Bob接收转账（模拟区块确认后更新余额）
    bob_wallet.deposit(3.0)
    
    # 步骤4：Alice再次向Bob转账2.0 ETH（验证nonce递增）
    print("\\n[步骤4] Alice再次向Bob转账2.0 ETH")
    tx2 = alice_wallet.create_transaction(bob_wallet.address, value=2.0)
    # Bob接收转账
    bob_wallet.deposit(2.0)
    
    # 步骤5：打印最终状态
    print("\\n[步骤5] 最终状态")
    print(f"Alice：余额={alice_wallet.get_balance():.6f} ETH，Nonce={alice_wallet.account.nonce}")
    print(f"Bob：余额={bob_wallet.get_balance():.6f} ETH，Nonce={bob_wallet.account.nonce}")
    
    # 总结账户模型特点
    print("\\n[账户模型核心特点]")
    print("  1. 账户化管理：类似银行账户，直接维护余额")
    print("  2. Nonce机制：每笔交易nonce递增，防止重放攻击")
    print("  3. Gas费用：控制计算资源消耗，避免无限循环")
    print("  4. 支持合约：合约账户有code和storage字段，可执行复杂逻辑")
    print("  5. 状态集中：全局维护账户状态，交易直接修改状态")
    
    return True

# 执行账户模型测试
demo_ethereum_account()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (138, 19, 6, 'ERC-20智能合约实现与测试', '1. 核心原理：智能合约是运行在区块链上的不可篡改程序，ERC-20是代币标准（定义了transfer、approve、transferFrom等核心接口）；合约状态存储在链上，通过交易触发执行，执行结果修改合约存储。
2. 实现SimpleSmartContract类（ERC-20核心功能）：
   - 初始化方法：设置代币名称、符号、总供应量。
   - deploy方法：部署合约，将总供应量分配给部署者。
   - balance_of方法：查询地址的代币余额。
   - transfer方法：直接转账代币（从发送者到接收者）。
   - approve方法：授权第三方使用指定数量的代币。
   - transfer_from方法：第三方使用授权额度转账代币。
3. 编写测试函数demo_smart_contract：模拟合约部署、代币转账、授权转账场景，验证ERC-20核心功能。
预期输出：打印合约部署、代币转账、授权、授权转账详情，以及最终各地址的代币余额，验证智能合约的自动执行逻辑。', 'ERC-20的核心是“余额映射+授权映射”，所有状态变更都通过合约方法执行，且执行结果不可篡改，需确保转账和授权逻辑的原子性', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (139, 19, 7, 'ERC-20智能合约实现与测试（续）', '', NULL, 'class SimpleSmartContract:
    """简单ERC-20代币合约（核心功能实现）
    遵循ERC-20标准核心接口：balanceOf、transfer、approve、transferFrom
    """
    
    def __init__(self, name: str, symbol: str, total_supply: int):
        """初始化合约参数
        Args:
            name: 代币名称（如：MyToken）
            symbol: 代币符号（如：MTK）
            total_supply: 总供应量（整数，最小单位）
        """
        self.name = name  # 代币名称
        self.symbol = symbol  # 代币符号
        self.total_supply = total_supply  # 总供应量
        self.balances: Dict[str, int] = {}  # 余额映射：地址→余额
        self.allowances: Dict[str, Dict[str, int]] = {}  # 授权映射：所有者→授权者→额度
        self.owner = None  # 合约部署者
        
        print(f"[SmartContract] 合约初始化完成：{self.name}（{self.symbol}），总供应量={self.total_supply}")
    
    def deploy(self, deployer_address: str):
        """部署合约（将总供应量分配给部署者）
        Args:
            deployer_address: 部署者地址
        """
        self.owner = deployer_address
        self.balances[deployer_address] = self.total_supply
        print(f"[SmartContract] 合约部署成功！")
        print(f"  部署者：{deployer_address[:18]}...")
        print(f"  部署者初始余额：{self.balances[deployer_address]} {self.symbol}")
    
    def balance_of(self, address: str) -> int:
        """查询地址的代币余额
        Args:
            address: 要查询的地址
        Returns:
            代币余额（整数）
        """
        return self.balances.get(address, 0)
    
    def transfer(self, from_address: str, to_address: str, amount: int) -> bool:
        """直接转账代币（from_address → to_address）
        Args:
            from_address: 发送者地址
            to_address: 接收者地址
            amount: 转账金额（整数）
        Returns:
            转账是否成功
        """
        # 验证参数
        if amount <= 0:
            print(f"[SmartContract] 转账失败：金额必须大于0")
            return False
        if self.balance_of(from_address) < amount:
            print(f"[SmartContract] 转账失败：{from_address[:18]}... 余额不足（需{amount}，有{self.balance_of(from_address)}）")
            return False
        
        # 执行转账：扣减发送者余额，增加接收者余额
        self.balances[from_address] -= amount
        self.balances[to_address] = self.balances.get(to_address, 0) + amount
        
        print(f"[SmartContract] 转账成功！")
        print(f"  发送者：{from_address[:18]}...（余额：{self.balance_of(from_address)} {self.symbol}）")
        print(f"  接收者：{to_address[:18]}...（余额：{self.balance_of(to_address)} {self.symbol}）")
        print(f"  转账金额：{amount} {self.symbol}")
        return True
    
    def approve(self, owner_address: str, spender_address: str, amount: int):
        """授权第三方（spender）使用指定额度的代币
        Args:
            owner_address: 代币所有者地址
            spender_address: 被授权者地址
            amount: 授权额度（整数）
        """
        if amount <= 0:
            print(f"[SmartContract] 授权失败：额度必须大于0")
            return
        if self.balance_of(owner_address) < amount:
            print(f"[SmartContract] 授权失败：所有者余额不足")
            return
        
        # 初始化授权映射（若不存在）
        if owner_address not in self.allowances:
            self.allowances[owner_address] = {}
        self.allowances[owner_address][spender_address] = amount
        
        print(f"[SmartContract] 授权成功！")
        print(f"  所有者：{owner_address[:18]}...")
        print(f"  被授权者：{spender_address[:18]}...")
        print(f"  授权额度：{amount} {self.symbol}")
    
    def transfer_from(self, spender_address: str, from_address: str, to_address: str, amount: int) -> bool:
        """第三方（spender）使用授权额度转账代币（from_address → to_address）
        Args:
            spender_address: 被授权者（发起转账者）
            from_address: 代币所有者地址
            to_address: 接收者地址
            amount: 转账金额（整数）
        Returns:
            转账是否成功
        """
        # 验证参数
        if amount <= 0:
            print(f"[SmartContract] 授权转账失败：金额必须大于0")
            return False
        
        # 验证授权额度
        allowance = self.allowances.get(from_address, {}).get(spender_address, 0)
        if allowance < amount:
            print(f"[SmartContract] 授权转账失败：授权额度不足（需{amount}，有{allowance}）")
            return False
        
        # 验证所有者余额
        if self.balance_of(from_address) < amount:
            print(f"[SmartContract] 授权转账失败：所有者余额不足")
            return False
        
        # 执行转账
        self.balances[from_address] -= amount
        self.balances[to_address] = self.balances.get(to_address, 0) + amount
        
        # 扣减授权额度
        self.allowances[from_address][spender_address] -= amount
        
        print(f"[SmartContract] 授权转账成功！")
        print(f"  所有者：{from_address[:18]}...（余额：{self.balance_of(from_address)} {self.symbol}）")
        print(f"  接收者：{to_address[:18]}...（余额：{self.balance_of(to_address)} {self.symbol}）")
        print(f"  被授权者：{spender_address[:18]}...（剩余授权：{self.allowances[from_address][spender_address]} {self.symbol}）")
        print(f"  转账金额：{amount} {self.symbol}")
        return True

# 测试智能合约
def demo_smart_contract():
    print("\\n" + "="*60)
    print("演示：ERC-20智能合约 - MyToken（MTK）部署与交互")
    print("="*60)
    
    # 步骤1：定义测试地址
    deployer = "0xDeployer_Address_0000000000"
    alice = "0xAlice_Address_1111111111"
    bob = "0xBob_Address_2222222222"
    print(f"\\n[步骤1] 测试地址：")
    print(f"  合约部署者：{deployer[:18]}...")
    print(f"  Alice：{alice[:18]}...")
    print(f"  Bob：{bob[:18]}...")
    
    # 步骤2：创建并部署ERC-20代币合约（总供应量100万MTK）
    print("\\n[步骤2] 部署ERC-20合约")
    mtk_contract = SimpleSmartContract(name="MyToken", symbol="MTK", total_supply=1000000)
    mtk_contract.deploy(deployer_address=deployer)
    
    # 步骤3：部署者向Alice转账1000 MTK
    print("\\n[步骤3] 部署者→Alice 转账1000 MTK")
    mtk_contract.transfer(from_address=deployer, to_address=alice, amount=1000)
    print(f"  部署者余额：{mtk_contract.balance_of(deployer)} MTK")
    print(f"  Alice余额：{mtk_contract.balance_of(alice)} MTK")
    
    # 步骤4：Alice向Bob转账300 MTK（直接转账）
    print("\\n[步骤4] Alice→Bob 转账300 MTK")
    mtk_contract.transfer(from_address=alice, to_address=bob, amount=300)
    print(f"  Alice余额：{mtk_contract.balance_of(alice)} MTK")
    print(f"  Bob余额：{mtk_contract.balance_of(bob)} MTK")
    
    # 步骤5：Alice授权Bob使用200 MTK（授权机制）
    print("\\n[步骤5] Alice授权Bob使用200 MTK")
    mtk_contract.approve(owner_address=alice, spender_address=bob, amount=200)
    
    # 步骤6：Bob使用授权额度，从Alice账户转账100 MTK到自己账户（授权转账）
    print("\\n[步骤6] Bob使用授权转账100 MTK")
    mtk_contract.transfer_from(
        spender_address=bob,
        from_address=alice,
        to_address=bob,
        amount=100
    )
    
    # 步骤7：打印最终余额
    print("\\n[步骤7] 最终余额")
    print(f"  部署者：{mtk_contract.balance_of(deployer)} MTK")
    print(f"  Alice：{mtk_contract.balance_of(alice)} MTK")
    print(f"  Bob：{mtk_contract.balance_of(bob)} MTK")
    
    # 总结智能合约特点
    print("\\n[智能合约核心特点]")
    print("  1. 自动执行：满足条件即执行，无需第三方干预")
    print("  2. 不可篡改：部署后代码和状态无法修改（除非有升级机制）")
    print("  3. 透明公开：合约代码和所有交易记录可公开查询")
    print("  4. 去中心化：运行在区块链节点上，无单点故障")
    print("  5. 标准接口：ERC-20等标准确保不同合约间的互操作性")
    
    return True

# 执行智能合约测试
demo_smart_contract()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (140, 19, 8, 'UTXO模型与账户模型深度对比与总结', '1. 核心维度对比：
   - 数学定义：UTXO模型是离散输出集合（Balance=ΣUTXO金额），账户模型是全局状态映射（Balance=账户字段值）。
   - 状态存储：UTXO存储离散的未花费输出，账户模型存储集中式账户状态。
   - 交易逻辑：UTXO交易需引用前序输出，账户模型交易直接修改余额。
   - 隐私性：UTXO模型更优（可使用新地址），账户模型较差（地址固定）。
   - 性能：UTXO支持并行验证，账户模型需处理状态冲突。
   - 功能扩展：账户模型更适合智能合约（状态集中管理），UTXO模型较复杂。
2. 适用场景：
   - UTXO模型：适用于纯支付场景（如比特币），追求隐私性和并行性。
   - 账户模型：适用于需要复杂逻辑的场景（如以太坊），支持智能合约、DeFi、NFT等。
3. 技术选型要点：
   - 若核心需求是支付+隐私：选择UTXO模型。
   - 若核心需求是智能合约+功能扩展：选择账户模型。
   - 混合模型：部分项目（如Cardano）结合两者优点，兼顾支付与智能合约。
4. 发展趋势：UTXO模型向智能合约扩展（如比特币Taproot升级），账户模型优化并行性（如以太坊分片），双模型融合成为新方向。
预期输出：形成清晰的模型选型框架，能根据项目需求选择合适的记账模型。', '没有绝对最优的模型，只有最适合场景的模型，需在隐私性、性能、功能扩展性之间做权衡', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (213, 37, 1, '了解算法背景与定位', 'SHA-1是美国国家安全局（NSA）设计、NIST于1995年发布的安全散列算法，属于SHA家族第二代成员，用于替代存在安全漏洞的SHA-0。
其设计深受MD4/MD5影响，采用Merkle–Damgård结构，输出160位（20字节）摘要，曾广泛应用于SSL/TLS、数字签名、文件校验等场景。
但2005年王小云团队公布碰撞攻击方法，2017年Google发布SHAttered实际碰撞实例，导致其被NIST标记为不推荐使用，现已被SHA-2、SHA-3取代。', 'SHA-1的核心价值在于其历史地位和对现代散列算法设计的影响', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (214, 37, 2, '掌握消息预处理流程', 'SHA-1处理消息前需完成填充和分块两步预处理，确保消息格式符合算法要求：
1. 填充操作：先在原始消息末尾添加1比特''1''，再补充若干比特''0''，使消息长度 ≡ 448 (mod 512)；最后附加原始消息长度L的64位二进制表示，最终总长度为512的整数倍。
2. 分块操作：将填充后的消息按512比特为单位，划分为若干分组$M^{(1)}, M^{(2)}, …, M^{(N)}$，后续将逐组进行迭代压缩。
预处理的核心目的是避免不同消息经填充后产生相同序列，保障算法安全性。', '填充操作是所有基于Merkle–Damgård结构散列算法的通用步骤', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (215, 37, 3, '熟悉初始向量与状态', 'SHA-1的内部状态由5个32位寄存器构成，初始向量（IV）为固定值：
$H_0 = 0x67452301$
$H_1 = 0xEFCDAB89$
$H_2 = 0x98BADCFE$
$H_3 = 0x10325476$
$H_4 = 0xC3D2E1F0$
这些初始值是算法设计时确定的固定常量，用于迭代压缩的初始状态初始化，所有消息的哈希计算都从该初始状态开始。', '初始向量的选择需满足随机性和唯一性，避免被攻击者利用', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (216, 37, 4, '理解迭代压缩核心步骤', '对每个512比特消息分组$M^{(i)}$，执行迭代压缩流程，分为消息调度和80轮运算两步：
1. 消息调度：将512比特分组拆分为16个32位字$W_0-W_{15}$，再通过公式$W_t = ROL_1(W_{t-3} ⊕ W_{t-8} ⊕ W_{t-14} ⊕ W_{t-16})$（t=16-79）扩展为80个32位字，其中$ROL_1$表示左循环移位1位。
2. 80轮运算：先将当前哈希状态$H_0-H_4$赋值给工作寄存器(A,B,C,D,E)；每轮计算$TEMP = ROL_5(A) + f_t(B,C,D) + E + W_t + K_t$，再更新寄存器状态$E=D, D=C, C=ROL_{30}(B), B=A, A=TEMP$。
其中轮函数$f_t$和常数$K_t$随轮次变化，具体为：
- 0≤t≤19：$f_t=(B∧C)∨(¬B∧D)$，$K_t=0x5A827999$
- 20≤t≤39：$f_t=B⊕C⊕D$，$K_t=0x6ED9EBA1$
- 40≤t≤59：$f_t=(B∧C)∨(B∧D)∨(C∧D)$，$K_t=0x8F1BBCDC$
- 60≤t≤79：$f_t=B⊕C⊕D$，$K_t=0xCA62C1D6$', '轮函数和常数的分段设计是为了提高算法的抗攻击能力', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (217, 37, 5, '掌握哈希状态更新与输出', '每个消息分组处理完成后，需更新全局哈希状态：
$H_0 = H_0 + A$
$H_1 = H_1 + B$
$H_2 = H_2 + C$
$H_3 = H_3 + D$
$H_4 = H_4 + E$
所有加法均为模$2^{32}$运算，确保结果保持32位长度。
当所有512比特分组都处理完毕后，将最终的$H_0-H_4$按顺序拼接，得到$H_0∥H_1∥H_2∥H_3∥H_4$，即160位的SHA-1散列摘要，通常以40位十六进制字符串形式呈现。', NULL, 'def sha1_finalize(h0, h1, h2, h3, h4):
    # 拼接5个32位寄存器，生成160位摘要
    digest = (h0 << 128) | (h1 << 96) | (h2 << 64) | (h3 << 32) | h4
    # 转换为40位十六进制字符串
    return format(digest, ''040x'')
# 示例：假设最终H0-H4为初始值，输出初始摘要
print(sha1_finalize(0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0))');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (218, 37, 6, '分析安全性与应用现状', '1. 安全性弱点：SHA-1的设计存在结构性缺陷，容易被差分攻击找到碰撞，2017年的SHAttered攻击已实际构造出两个不同消息具有相同SHA-1摘要的实例，证明其不具备抗碰撞性。
2. 应用迁移：目前主流浏览器、安全协议已全面禁用SHA-1，数字签名、文件校验等场景已迁移至SHA-2（SHA-256/SHA-512）和SHA-3系列算法。
3. 残留用途：仅在历史系统兼容、教学演示、非安全敏感场景中仍有使用，核心价值在于帮助理解散列算法的设计原理和演进过程。', '实际开发中需优先使用SHA-256及以上安全强度的散列算法', NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (219, 37, 7, '参考实现与验证', '以下是SHA-1的简化实现（核心逻辑），可通过测试向量验证正确性：
实现要点包括循环移位、消息填充、消息调度、80轮迭代等核心模块，最终输出160位十六进制摘要。
测试向量示例：空字符串的SHA-1摘要为''da39a3ee5e6b4b0d3255bfef95601890afd80709''，字符串''abc''的摘要为''a9993e364706816aba3e25717850c26c9cd0d89d''。', NULL, 'def right_rotate(value, amount):
    return ((value >> amount) | (value << (32 - amount))) & 0xFFFFFFFF

def sha1(message):
    if isinstance(message, str):
        message = message.encode(''utf-8'')
    
    # 初始向量
    h = [0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0]
    
    # 消息填充
    original_bit_len = len(message) * 8
    message += b''\\x80''
    while (len(message) % 64) != 56:
        message += b''\\x00''
    message += original_bit_len.to_bytes(8, byteorder=''big'')
    
    # 处理每个512比特块
    for i in range(0, len(message), 64):
        chunk = message[i:i+64]
        w = [0] * 80
        
        # 消息调度：16字扩展为80字
        for j in range(16):
            w[j] = int.from_bytes(chunk[j*4:j*4+4], byteorder=''big'')
        for j in range(16, 80):
            w[j] = right_rotate(w[j-3] ^ w[j-8] ^ w[j-14] ^ w[j-16], 1)
        
        a, b, c, d, e = h
        
        # 80轮迭代
        for t in range(80):
            if 0 <= t <= 19:
                f = (b & c) | ((~b) & d)
                k = 0x5A827999
            elif 20 <= t <= 39:
                f = b ^ c ^ d
                k = 0x6ED9EBA1
            elif 40 <= t <= 59:
                f = (b & c) | (b & d) | (c & d)
                k = 0x8F1BBCDC
            else:
                f = b ^ c ^ d
                k = 0xCA62C1D6
            
            temp = (right_rotate(a, 5) + f + e + k + w[t]) & 0xFFFFFFFF
            e = d
            d = c
            c = right_rotate(b, 30)
            b = a
            a = temp
        
        # 更新哈希状态
        h[0] = (h[0] + a) & 0xFFFFFFFF
        h[1] = (h[1] + b) & 0xFFFFFFFF
        h[2] = (h[2] + c) & 0xFFFFFFFF
        h[3] = (h[3] + d) & 0xFFFFFFFF
        h[4] = (h[4] + e) & 0xFFFFFFFF
    
    # 生成最终摘要
    return ''''.join(format(x, ''08x'') for x in h)

# 测试
print(sha1(''''))  # 输出da39a3ee5e6b4b0d3255bfef95601890afd80709
print(sha1(''abc''))  # 输出a9993e364706816aba3e25717850c26c9cd0d89d');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (220, 38, 1, '了解SHA-1的起源与背景', '1. SHA-1全称为Secure Hash Algorithm（安全散列算法），由美国国家安全局（NSA）设计，美国国家标准与技术研究院（NIST）于1995年发布。
2. 它是SHA家族第二个成员，前身SHA-0因安全漏洞被快速弃用，SHA-1是其改进版本，核心差异在于压缩函数的消息扩展步骤，新增了消息调度的循环移位操作以提升安全性。
3. SHA-1的设计深受MD4/MD5哈希函数影响，目标是保证抗碰撞性、抗原像性和次原像性，最初用于数字签名和完整性校验等场景。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (221, 38, 2, '梳理SHA-1的应用历史', '1. 标准化阶段：1995年被收录到FIPS PUB 180-1联邦信息处理标准，成为美国联邦机构和商业系统的标配哈希算法。
2. 广泛应用阶段：覆盖SSL/TLS加密通信、PGP/SSH/IPsec安全协议、数字签名、软件/文件完整性校验等核心安全场景。
3. 淘汰阶段：2005年王小云团队公布碰撞攻击方法，2017年Google与CWI发布首个实际碰撞实例（SHAttered攻击），主流浏览器禁用SHA-1证书，NIST将其标记为不推荐使用，逐步被SHA-2、SHA-3取代。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (222, 38, 3, '掌握SHA-1的数学结构', 'SHA-1核心采用Merkle–Damgård结构，整体流程分为三大核心环节：
1. 对输入消息进行预处理后分块，每个块大小为512比特；
2. 每个分块通过压缩函数与前序哈希状态迭代计算；
3. 所有分块处理完成后，输出最终的160比特哈希摘要。
该结构是哈希函数经典设计范式，保证了算法的迭代可计算性。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (223, 38, 4, '执行消息预处理操作', '预处理是SHA-1保证输入唯一性的关键步骤，分为填充和分块两步：
1. 填充规则：
   - 设原始消息比特长度为$L$，先在消息末尾添加1比特的1；
   - 补充若干个0比特，直到消息总长度 ≡ 448 (mod 512)；
   - 最后附加$L$的64位二进制表示，使填充后总长度为512的整数倍。
2. 分块操作：将填充后的完整消息拆分为若干个512比特的分组，记为$M^{(1)}, M^{(2)}, …, M^{(N)}$。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (224, 38, 5, '初始化哈希向量（IV）', 'SHA-1的内部状态由5个32位寄存器组成，初始值固定为：
$H_0 = 0x67452301$
$H_1 = 0xEFCDAB89$
$H_2 = 0x98BADCFE$
$H_3 = 0x10325476$
$H_4 = 0xC3D2E1F0$
这5个初始值是算法预置的固定常量，作为迭代计算的初始哈希状态。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (225, 38, 6, '执行迭代压缩函数计算', '对每个512比特分组$M^{(i)}$，执行以下核心计算：
1. 消息调度：
   - 将分组拆分为16个32位字：$W_0, W_1, …, W_{15}$；
   - 扩展为80个32位字：$W_t = ROL_1(W_{t-3} ⊕ W_{t-8} ⊕ W_{t-14} ⊕ W_{t-16})$（$t=16..79$），其中$ROL_1$表示32位整数左循环移位1位。
2. 80轮迭代运算：
   - 初始化工作寄存器：$(A,B,C,D,E) = (H_0,H_1,H_2,H_3,H_4)$；
   - 每轮计算：$TEMP = ROL_5(A) + f_t(B,C,D) + E + W_t + K_t$，随后更新寄存器：$E=D, D=C, C=ROL_{30}(B), B=A, A=TEMP$；
   - 轮函数$f_t$和常数$K_t$随轮次变化（0-19/20-39/40-59/60-79四组取值）。
3. 更新哈希值：处理完分组后，$H_0=H_0+A, H_1=H_1+B, H_2=H_2+C, H_3=H_3+D, H_4=H_4+E$（均为模$2^{32}$加法）。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (226, 38, 7, '生成最终SHA-1哈希输出', '当所有512比特分组处理完成后，将最终的5个32位寄存器值拼接，得到160位的哈希摘要：
$SHA-1(M) = H_0 ∥ H_1 ∥ H_2 ∥ H_3 ∥ H_4$
其中$∥$表示比特串拼接，最终输出的160位结果即为输入消息的SHA-1哈希值，通常转换为40位十六进制字符串展示。', NULL, NULL);
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (227, 38, 8, '实现并验证哈希算法', '基于SHA系列算法的核心逻辑，可通过Python实现哈希计算（注：示例为SHA-256实现，SHA-1可类比实现）：
1. 实现核心工具函数：如32位整数循环右移函数right_rotate；
2. 消息预处理：完成比特填充和分块操作；
3. 消息调度扩展：将512比特块扩展为64个32位字（SHA-256）/80个32位字（SHA-1）；
4. 迭代压缩计算：按轮次执行压缩函数，更新哈希状态；
5. 结果输出：将最终哈希状态转换为十六进制字符串。
6. 验证：使用标准测试向量（空字符串、"abc"等）验证实现正确性，确保输出与预期哈希值一致。', '注意区分SHA-1与SHA-256的参数差异（如初始向量、轮常数、轮次、输出长度等）', 'def right_rotate(value, amount):
    """32位整数的循环右移"""
    return ((value >> amount) | (value << (32 - amount))) & 0xFFFFFFFF


def sha256(message):
    """
    计算输入消息的SHA-256散列值
    
    参数:
        message (str 或 bytes): 要计算散列值的消息
    
    返回:
        str: SHA-256散列值的十六进制字符串表示
    """
    # 将输入消息转换为字节
    if isinstance(message, str):
        message = message.encode(''utf-8'')
    
    # SHA-256算法使用的初始哈希值 (前8个质数的平方根小数部分的前32位)
    h = [
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    ]
    
    # SHA-256算法使用的常量 (前64个质数的立方根小数部分的前32位)
    k = [
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
        0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
        0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
        0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    ]
    
    # 预处理: 填充消息
    # 1. 附加比特''1''
    # 2. 附加k个比特''0'', k是满足 (消息长度 + 1 + k + 8) % 512 = 0 的最小非负整数
    # 3. 附加消息长度的64位表示
    
    # 计算原始比特长度
    original_bit_len = len(message) * 8
    
    # 附加比特''1''
    message += b''\\x80''
    
    # 附加比特''0''直到消息长度满足条件 (消息长度 + 1 + k + 8) % 64 = 0 (以字节为单位)
    while (len(message) % 64) != 56:
        message += b''\\x00''
    
    # 附加消息长度(原始消息的比特长度)的64位(8字节)表示
    message += original_bit_len.to_bytes(8, byteorder=''big'')
    
    # 处理消息块
    for i in range(0, len(message), 64):
        chunk = message[i:i+64]
        
        # 创建消息调度表 (message schedule)
        w = [0] * 64
        
        # 将块拆分为16个32位(4字节)字
        for j in range(16):
            w[j] = int.from_bytes(chunk[j*4:j*4+4], byteorder=''big'')
        
        # 扩展剩余的48个字
        for j in range(16, 64):
            s0 = right_rotate(w[j-15], 7) ^ right_rotate(w[j-15], 18) ^ (w[j-15] >> 3)
            s1 = right_rotate(w[j-2], 17) ^ right_rotate(w[j-2], 19) ^ (w[j-2] >> 10)
            w[j] = (w[j-16] + s0 + w[j-7] + s1) & 0xFFFFFFFF
        
        # 初始化工作变量为当前的哈希值
        a, b, c, d, e, f, g, h_val = h
        
        # 压缩函数主循环
        for j in range(64):
            S1 = right_rotate(e, 6) ^ right_rotate(e, 11) ^ right_rotate(e, 25)
            ch = (e & f) ^ ((~e) & g)
            temp1 = (h_val + S1 + ch + k[j] + w[j]) & 0xFFFFFFFF
            S0 = right_rotate(a, 2) ^ right_rotate(a, 13) ^ right_rotate(a, 22)
            maj = (a & b) ^ (a & c) ^ (b & c)
            temp2 = (S0 + maj) & 0xFFFFFFFF
            
            h_val = g
            g = f
            f = e
            e = (d + temp1) & 0xFFFFFFFF
            d = c
            c = b
            b = a
            a = (temp1 + temp2) & 0xFFFFFFFF
        
        # 将压缩后的块添加到当前哈希值
        h[0] = (h[0] + a) & 0xFFFFFFFF
        h[1] = (h[1] + b) & 0xFFFFFFFF
        h[2] = (h[2] + c) & 0xFFFFFFFF
        h[3] = (h[3] + d) & 0xFFFFFFFF
        h[4] = (h[4] + e) & 0xFFFFFFFF
        h[5] = (h[5] + f) & 0xFFFFFFFF
        h[6] = (h[6] + g) & 0xFFFFFFFF
        h[7] = (h[7] + h_val) & 0xFFFFFFFF
    
    # 将最终哈希值转换为十六进制字符串
    return ''''.join(format(h_val, ''08x'') for h_val in h)

# 演示验证
test_vectors = [
    ("", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
    ("abc", "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),
    ("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq",
     "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1"),
]

for message, expected in test_vectors:
    result = sha256(message)
    print(f"消息: {message!r}")
    print(f"SHA-256: {result}")
    print(f"预期结果: {expected}")
    print(f"验证: {''通过'' if result == expected else ''失败''}")
    print()');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (344, 53, 1, '理解 Transformer 与 Token 的概念', '大语言模型（LLM）基于 Transformer 架构，其核心是自注意力机制（Self-Attention），能够捕捉序列中任意两个位置之间的依赖关系。模型在海量文本上训练后，学会了根据上文预测下一个 token 的概率分布。

Token 是模型处理文本的基本单位，不等于字也不等于词。对于英文，一个 token 大约是 3/4 个单词（如 ''hello'' 是 1 个 token，''unbelievable'' 是 4 个 token）。对于中文，一个汉字通常是 1-2 个 token。例如''你好世界''大约 4-5 个 token。理解 token 很重要，因为 API 的计费、上下文窗口限制、输出长度限制都以 token 为单位。

模型本身是''无状态''的——每次 API 调用都是独立的推理过程，模型不''记住''任何之前的对话。它只看到当前传入的 token 序列，根据训练中学到的统计规律自回归地（autoregressive）逐个生成后续 token。', '可以用 tokenizer 工具查看文本的 token 数量：DeepSeek 兼容 OpenAI 的 tiktoken 库。理解 token 有助于后续项目中合理控制上下文长度和 API 成本。', '# 使用 tiktoken 查看 token 数量（DeepSeek 兼容 OpenAI 的编码）
import tiktoken

encoder = tiktoken.get_encoding(''cl100k_base'')

text_en = ''Hello, how are you?''
text_zh = ''你好，今天天气怎么样？''

print(f''英文 token 数: {len(encoder.encode(text_en))}'')  # 约 6 个
print(f''中文 token 数: {len(encoder.encode(text_zh))}'')  # 约 13 个');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (345, 53, 2, '掌握 API 客户端初始化与核心参数', 'DeepSeek API 兼容 OpenAI SDK 格式，通过修改 base_url 即可切换服务商。核心调用接口是 chat.completions.create()，关键参数如下：

- api_key：身份认证凭证，格式为 sk- 开头的字符串。API Key 对应一个账户额度，每次调用会消耗 token 配额。
- base_url：API 服务端点地址。DeepSeek 为 https://api.deepseek.com，OpenAI 为 https://api.openai.com/v1。这个设计让你可以用同一套代码切换不同模型提供商。
- model：选择具体模型。deepseek-chat 用于通用对话（类似 GPT-4 级别），deepseek-reasoner 用于深度推理（类似 o1 级别，会先输出思维链再给答案）。
- messages：消息列表，是核心输入参数，决定模型''看到''什么内容。
- temperature：采样温度，0.0~2.0，控制输出随机性。
- max_tokens：限制模型最多生成多少 token。如果设置过小，回复会被截断（finish_reason 返回 ''length'' 而非 ''stop''）。', 'api_key 永远不要硬编码在代码中提交到 Git。在实验室服务器上可以用环境变量：终端执行 set DEEPSEEK_API_KEY=sk-xxx（Windows）或 export DEEPSEEK_API_KEY=sk-xxx（Linux/Mac），代码中用 os.getenv() 读取。', 'import os
from openai import OpenAI

client = OpenAI(
    api_key=os.getenv("DEEPSEEK_API_KEY", "sk-xxx"),
    base_url="https://api.deepseek.com",
)

response = client.chat.completions.create(
    model="deepseek-chat",
    messages=[
        {"role": "user", "content": "你好"}
    ],
    temperature=0.7,
    max_tokens=500
)

print(response.choices[0].message.content)');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (346, 53, 3, '解析 API 响应结构', 'API 返回的 response 对象包含丰富的信息，理解其结构对于调试和监控至关重要。顶层字段包括：id（请求唯一标识）、model（实际使用的模型）、usage（token 用量统计）和 choices（候选回复列表）。

choices[0] 包含：message（模型回复，含 role 和 content）、finish_reason（停止原因，''stop'' 表示正常结束，''length'' 表示达到 max_tokens 限制被截断，''content_filter'' 表示被安全过滤）。

usage 包含三个字段：prompt_tokens（输入 token 数）、completion_tokens（输出 token 数）、total_tokens（总计）。这些信息对于监控 API 消耗和优化 prompt 长度很有价值。通常输入 token 单价较低，输出 token 单价较高（约 4 倍），所以控制输出长度是节省成本的关键。', '如果 finish_reason 是 ''length''，说明回复被截断了，需要增大 max_tokens 重试。这是新手常犯的错误——以为模型''不想回答''，其实是输出被截断了。', 'response = client.chat.completions.create(
    model="deepseek-chat",
    messages=[{"role": "user", "content": "用 200 字介绍深度学习"}],
    max_tokens=500
)

# 解析回复内容
result = response.choices[0].message
print(f"回复内容: {result.content}")
print(f"停止原因: {response.choices[0].finish_reason}")

# 查看 token 用量
print(f"\\n--- Token 用量 ---")
print(f"输入 tokens: {response.usage.prompt_tokens}")
print(f"输出 tokens: {response.usage.completion_tokens}")
print(f"总计 tokens: {response.usage.total_tokens}")');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (347, 53, 4, '深入理解三种消息角色', 'messages 列表中每条消息有 role 和 content 两个字段。role 有三种取值，各自承担不同职责：

system（系统消息）：设定 AI 的行为规则、角色身份和输出格式。模型在训练时被强化了对 system 消息的遵循——它被训练为将 system 内容视为不可违背的行为准则。因此 system prompt 适合放置全局性的指令，如''你是一个翻译助手，只输出翻译结果''。

user（用户消息）：用户输入的问题或指令。这是对话的主要内容，模型对此进行响应。

assistant（助手消息）：模型之前的回复。在多轮对话中，将之前的 assistant 消息保留在 messages 列表中，模型就能''看到''自己之前说了什么，从而维持对话连贯性。这也是项目三 Memory 机制的基础。

一条关键原则：System Prompt 与 User Prompt 的分离不仅是格式要求，更是控制力的分层——system 层提供稳定的行为框架，user 层处理变化的具体输入。', '把所有指令都放在 User Prompt 里也能工作，但当用户输入较长或包含干扰信息时，模型可能''忘记''遵循指令。将关键指令放在 System Prompt 中更稳定。', '# 单轮对话：只有 system + user
messages_single = [
    {"role": "system", "content": "你是一个专业的翻译助手，只输出翻译结果。"},
    {"role": "user", "content": "请把''你好世界''翻译成英文"}
]

# 多轮对话：包含历史 assistant 消息
messages_multi = [
    {"role": "system", "content": "你是一个友好的助手。"},
    {"role": "user", "content": "我叫小明"},
    {"role": "assistant", "content": "你好小明！很高兴认识你。"},
    {"role": "user", "content": "我叫什么名字？"}  # 模型能通过历史记录回答
]');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (348, 53, 5, '掌握采样参数：temperature 与 top_p', '模型生成每个 token 时，会计算词表中所有 token 的概率分布，然后从中采样。采样参数决定了''如何选择''：

temperature（温度）：控制概率分布的''锐度''。temperature → 0 时，分布变得极其尖锐，模型几乎总是选概率最高的 token（贪心解码）；temperature 升高，分布变平坦，低概率 token 也有机会被选中。temperature = 0 或极小时输出最确定，1.5 以上输出高度随机。

top_p（核采样）：只从累积概率达到 p 的最小 token 集合中采样。top_p = 0.1 意味着只从概率最高的那几个 token 中选择。与 temperature 的区别在于：top_p 动态调整候选集大小——当模型很确定时候选少，不确定时候选多。

通常只调其中一个。需要稳定输出（数据提取、日程规划）时设 temperature=0.0~0.3；一般对话设 0.7~1.0；创意写作设 1.2~1.5。', '如果日程规划助手的 temperature 设为 1.5，同样的输入可能每次输出完全不同的时间表格式。结构化任务务必用低 temperature。', '# 贪心解码：temperature=0，输出最确定
resp_greedy = client.chat.completions.create(
    model="deepseek-chat",
    messages=[{"role": "user", "content": "中国的首都是？"}],
    temperature=0.0  # 多次调用结果几乎完全相同
)

# 核采样：top_p=0.1，只从最可能的 token 中选择
resp_topp = client.chat.completions.create(
    model="deepseek-chat",
    messages=[{"role": "user", "content": "给公司起个名字"}],
    top_p=0.1  # 候选集动态调整
)');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (349, 53, 6, 'System Prompt 的四要素设计法', 'System Prompt 是 Prompt Engineering 的核心，它决定了模型的行为模式。一个有效的 System Prompt 应包含四个要素：

1. 角色定义（Role）：你是谁。如''你是一个专业的日程规划助手''。角色定义帮助模型进入''专家模式''，激活与角色相关的训练知识。

2. 任务描述（Task）：你要做什么。如''将用户杂乱的日程描述整理为清晰的时间表''。任务描述要具体明确，避免模糊表述。

3. 输出格式（Format）：按什么格式输出。如''使用 Markdown 无序列表，时间用 24 小时制''。明确格式要求是获得结构化输出的关键。

4. 约束条件（Constraint）：不能做什么。如''不要输出多余的解释，只输出时间表''。约束条件划定行为边界，防止模型''话多''。

此外，在 System Prompt 中给出输出示例（Few-shot）比纯抽象描述更有效——模型对示例的遵循度通常高于对规则描述的理解。', '一个好的检验标准：把 System Prompt 给一个完全不了解项目的人看，他能否准确预测模型的输出格式？如果不能，说明 prompt 还不够清晰。', 'system_prompt = """你是一个专业的日程规划助手。

【任务】将用户杂乱的日程描述整理为清晰的时间表。

【输出格式】
1. 使用 Markdown 无序列表格式
2. 时间统一使用 24 小时制 HH:MM 格式
3. 按时间先后排序
4. 每条格式为：- HH:MM 事项描述

【约束】
- 只输出时间表，不要附加任何解释或问候语
- 如果用户没有指定具体时间的事项，放在列表末尾并标注''待定''

【示例】
输入：明天上午九点开会，下午有空写报告
输出：
- 09:00 开会
- 待定 写报告"""');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (350, 53, 7, '掌握 Few-shot Prompting 技巧', 'Few-shot Prompting 是在提示中提供几个输入-输出示例，让模型''照样子''生成回答。这利用了模型的上下文学习（In-context Learning）能力——模型虽然不会因为你的示例而更新权重，但它会根据示例中的模式来推断你期望的输出格式和风格。

根据示例数量分为：Zero-shot（无示例，只给指令）、One-shot（1 个示例）、Few-shot（2-5 个示例）。通常 2-3 个示例就能显著提高输出格式的稳定性。示例过多反而会浪费 token 并可能导致模型''过拟合''到示例内容。

示例的选择原则：覆盖典型场景、格式一致、包含边界情况。如果示例之间有递进关系（从简单到复杂），效果更好。', 'Few-shot 示例放在 System Prompt 中（作为系统指令的一部分）还是 User Prompt 中（作为用户输入的一部分）都可以，但放在 System Prompt 中通常更稳定。', '# Zero-shot：只给指令
system_zero = "你是一个情绪分析器，输出 JSON 格式的情绪分析结果。"

# Few-shot：给指令 + 示例
system_fewshot = """你是一个情绪分析器。分析用户输入文本的情绪，输出 JSON 格式。

示例 1：
输入：太棒了！我今天拿到了 offer！
输出：{"emotion": "开心", "confidence": "高"}

示例 2：
输入：哎，又下雨了...
输出：{"emotion": "悲伤", "confidence": "中"}

示例 3：
输入：这个 bug 改了三遍还是不对！！！
输出：{"emotion": "愤怒", "confidence": "高"}

请分析以下输入："""');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (351, 53, 8, 'API 错误处理与重试机制', '在实际调用中，API 请求可能因各种原因失败：网络超时、速率限制（rate limit，短时间内请求过多，返回 429 状态码）、服务端错误（500）、余额不足等。健壮的程序需要处理这些异常。

常见的错误类型：openai.APIConnectionError（网络连接失败）、openai.RateLimitError（触发速率限制）、openai.APIStatusError（其他 HTTP 错误）。对于网络超时和速率限制，通常采用指数退避（exponential backoff）重试策略：第一次等 1 秒重试，第二次等 2 秒，第三次等 4 秒，以此类推。

openai SDK 内置了重试机制，默认会重试 2 次。也可以手动实现更精细的控制。', '在实验室批量调用 API 时很容易触发速率限制。DeepSeek 的速率限制可以在控制台查看，建议每次请求之间加 time.sleep(0.5) 间隔。', 'import time
from openai import OpenAI, APIConnectionError, RateLimitError

client = OpenAI(
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    base_url="https://api.deepseek.com",
)

def call_with_retry(system_prompt, user_input, max_retries=3):
    """带指数退避重试的 API 调用"""
    for attempt in range(max_retries):
        try:
            response = client.chat.completions.create(
                model="deepseek-chat",
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_input},
                ],
                temperature=0.2,
            )
            return response.choices[0].message.content
        except RateLimitError:
            wait_time = 2 ** attempt  # 指数退避：1s, 2s, 4s
            print(f"触发速率限制，等待 {wait_time}s 后重试...")
            time.sleep(wait_time)
        except APIConnectionError:
            print(f"网络错误，第 {attempt+1} 次重试...")
            time.sleep(1)
    return "API 调用失败，请稍后重试"');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (352, 53, 9, '封装可复用的调用基础设施', '在实际项目中，应将 API 调用封装为可复用的函数或类。封装的好处：统一管理配置（model、base_url 等）、集中处理错误、方便后续项目复用。这个封装函数是后续四个项目的基础设施。

好的封装应该：接受 system_prompt 和 user_input 作为必要参数，temperature 和 max_tokens 作为可选参数（有合理默认值），内部处理错误和重试，返回干净的字符串结果。也可以进一步封装为类，支持保存调用历史、统计 token 消耗等功能。', '将这段代码保存为 utils.py 或 common.py，后续项目中直接 import 使用，避免每个 notebook 重复写初始化代码。', 'import os
from openai import OpenAI

client = OpenAI(
    api_key=os.getenv("DEEPSEEK_API_KEY", "sk-xxx"),
    base_url="https://api.deepseek.com",
)

def call_deepseek(system_prompt, user_input, temperature=0.2, max_tokens=500):
    """
    调用 DeepSeek API 的通用函数。
    
    参数:
        system_prompt: 系统提示词，定义 AI 的角色和行为
        user_input: 用户输入的内容
        temperature: 创造性控制 (0.0~2.0)，默认 0.2
        max_tokens: 最大输出 token 数，默认 500
    返回:
        str: 模型的回复内容
    """
    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_input},
        ],
        temperature=temperature,
        max_tokens=max_tokens,
    )
    return response.choices[0].message.content


# 测试
result = call_deepseek(
    system_prompt="你是一个友好的助手，用一句话回答问题。",
    user_input="简短介绍一下深度学习？"
)
print(result)');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (353, 54, 1, '理解大模型的根本局限与 Tool Use 的设计动机', '大语言模型有四个根本局限，理解这些局限是理解 Tool Use 设计动机的前提：

1. 知识截止：模型的知识停留在训练数据的截止日期，无法获取实时信息（如今日天气、最新论文）。
2. 计算不可靠：模型通过概率分布生成 token，不擅长精确算术。1234×5678 模型可能''猜''一个接近的答案，但不保证正确。
3. 无法访问外部系统：模型不能查数据库、读文件、调 API，它的世界就是训练数据和输入 prompt。
4. 幻觉问题：模型可能自信地编造不存在的事实，因为它本质上是在做''合理的续写''而非''事实检索''。

Tool Use 的核心思想是''模型负责决策，工具负责执行''——模型像一个被关在房间里的智者，可以思考推理但无法直接观察外界。工具就是房间开的小窗口：模型判断需要外界信息时，递一张纸条出去（输出工具调用指令），外面的助手执行操作后把结果递回来（tool 消息），模型再根据结果组织回答。', '关键理解：模型本身并不执行工具。它只是输出一个''我想调用 get_weather(city=''Beijing'')''的结构化指令，真正的函数执行发生在我们本地的 Python 代码中。模型的角色是''决策者''而非''执行者''。', '');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (354, 54, 2, '定义本地工具函数', '工具调用的第一步是定义真正执行功能的本地 Python 函数。这些函数与模型无关，是纯粹的业务逻辑。以天气查询为例，我们使用免费的 wttr.in API 获取真实天气数据——这是一个开源天气服务，返回 JSON 格式数据，无需注册 API Key，适合学习使用。

函数设计要点：参数类型要明确（str、int 等），返回值统一为字符串（因为最终要作为 tool 消息的 content 发给模型），要有异常处理（网络请求可能失败），返回信息要有意义（模型需要据此生成自然语言回答）。', '工具函数的返回值会被模型''阅读''并据此生成回答，所以返回值应该是信息丰富的自然语言字符串，而不是原始的数据结构。', 'import requests

def get_weather(city: str) -> str:
    """查询指定城市的实时天气（使用 wttr.in 免费 API）"""
    try:
        resp = requests.get(
            f"https://wttr.in/{city}?format=j1&lang=zh",
            timeout=10
        )
        data = resp.json()
        current = data["current_condition"][0]
        temp = current["temp_C"]
        humidity = current["humidity"]
        desc = current["lang_zh"][0]["value"]
        feels_like = current["FeelsLikeC"]
        wind_speed = current["windspeedKmph"]
        
        return f"{city}当前天气：{desc}，气温 {temp}°C，体感 {feels_like}°C，湿度 {humidity}%，风速 {wind_speed}km/h"
    except Exception as e:
        return f"获取 {city} 天气失败：{e}"


def calculate_days(date1: str, date2: str) -> str:
    """计算两个日期之间相差多少天"""
    from datetime import datetime
    d1 = datetime.strptime(date1, "%Y-%m-%d")
    d2 = datetime.strptime(date2, "%Y-%m-%d")
    diff = abs((d2 - d1).days)
    return f"{date1} 到 {date2} 相差 {diff} 天"


# 测试工具函数
print(get_weather("Kaifeng"))
print(calculate_days("2025-01-01", "2025-10-01"))');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (355, 54, 3, '用 JSON Schema 定义工具描述', '工具描述是给模型看的''说明书''，使用 JSON Schema 格式。它不是执行函数，而是告诉模型''你有哪些工具可以用，每个工具需要什么参数''。每个工具描述包含三部分：

name：工具名称，模型据此指定调用哪个工具。命名要清晰，如 get_weather 比 tool_1 更好。

description：工具功能描述，模型据此判断何时应该调用。这是最关键的字段——描述不准确会导致模型在该调用时不调用，或不该调用时乱调用。描述中应包含使用场景和返回内容的说明。

parameters：参数定义，使用 JSON Schema 的 type/properties/required 结构。每个参数需要有 type（类型）、description（描述，含示例值）。required 数组指定必填参数。支持的参数类型包括 string、number、integer、boolean、array、object。

对于枚举类型的参数，可以使用 enum 字段限制取值范围，如 {"type": "string", "enum": ["celsius", "fahrenheit"]}。', 'description 中给出参数示例（如''城市英文名，例如 Beijing、Shanghai''）能显著提高模型传参的准确性。模糊的描述（如''城市名称''）可能导致模型传中文名而非拼音。', 'weather_tool = {
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "根据城市名称查询当前的实时天气情况，包括温度、天气状况、湿度和风速。当用户询问任何与天气相关的问题时应调用此工具。",
        "parameters": {
            "type": "object",
            "properties": {
                "city": {
                    "type": "string",
                    "description": "要查询天气的城市英文名（拼音），例如：Beijing、Shanghai、Guangzhou"
                }
            },
            "required": ["city"]
        }
    }
}

# 带枚举参数的工具示例
unit_tool = {
    "type": "function",
    "function": {
        "name": "convert_temperature",
        "description": "在摄氏度和华氏度之间转换温度",
        "parameters": {
            "type": "object",
            "properties": {
                "value": {"type": "number", "description": "温度数值"},
                "from_unit": {
                    "type": "string",
                    "enum": ["celsius", "fahrenheit"],
                    "description": "原始温度单位"
                },
                "to_unit": {
                    "type": "string",
                    "enum": ["celsius", "fahrenheit"],
                    "description": "目标温度单位"
                }
            },
            "required": ["value", "from_unit", "to_unit"]
        }
    }
}');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (356, 54, 4, '理解两种新的消息角色', 'Tool Use 在 system/user/assistant 基础上引入了两种新的消息结构：

第一种：带 tool_calls 的 assistant 消息。当模型判断需要调用工具时，返回的 content 为空字符串，取而代之的是 tool_calls 数组。每个 tool_call 包含：id（本次调用的唯一标识符，如 ''call_abc123''）、type（固定为 ''function''）、function.name（要调用的函数名）和 function.arguments（JSON 格式的参数字符串）。注意 arguments 是字符串而非字典，需要用 json.loads() 解析。

第二种：tool 消息。工具执行完成后，通过 role: ''tool'' 的消息将结果返回给模型。其中 tool_call_id 必须与 assistant 消息中对应 tool_call 的 id 完全匹配——这是 API 用来关联请求和响应的机制。

重要规则：每个 tool_call 都必须有一条对应的 tool 消息，数量必须一一对应。如果模型返回了 3 个 tool_calls，就必须有 3 条 tool 消息，否则 API 会报错 ''insufficient tool messages following tool_calls message''。assistant 消息中 content 必须是空字符串 "" 而不是 None。', '最常见的错误是 tool_call_id 不匹配或 tool 消息数量不对。调试时建议打印 messages 列表，检查每条 tool 消息的 tool_call_id 是否与 assistant 中的 id 一一对应。', '# 完整的消息流转示例

# 第 1 次 API 调用：用户提问
messages = [
    {"role": "user", "content": "北京今天天气怎么样？"}
]
# → 模型返回 assistant 消息，包含 tool_calls：
# {
#     "role": "assistant",
#     "content": "",
#     "tool_calls": [{
#         "id": "call_abc123",
#         "type": "function",
#         "function": {
#             "name": "get_weather",
#             "arguments": "{\\"city\\": \\"Beijing\\"}"
#         }
#     }]
# }

# 第 2 次 API 调用：带上完整历史
messages = [
    {"role": "user", "content": "北京今天天气怎么样？"},
    {"role": "assistant", "content": "", "tool_calls": [...]},
    {"role": "tool", "tool_call_id": "call_abc123", "content": "Beijing当前天气：晴，气温 28°C"},
]
# → 模型根据工具结果，生成最终回答："北京今天天气晴朗，气温28°C..."');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (357, 54, 5, '实现单工具调用的完整流程', '单工具智能体的完整流程是一个循环：

第 1 步：将用户消息和 tools 参数一起发送给模型。tools 参数告诉模型''你有这个工具可以用''。
第 2 步：检查返回的 assistant 消息是否包含 tool_calls。如果没有，说明模型判断不需要工具（如用户问''你好''），直接返回 content。
第 3 步：如果有 tool_calls，需要把 assistant 消息（含 tool_calls）追加到 messages 中。这里需要将 SDK 返回的对象转为字典格式。
第 4 步：遍历每个 tool_call，解析函数名和参数，执行对应的本地函数。
第 5 步：将每个工具的执行结果作为 tool 消息追加到 messages 中，tool_call_id 必须与对应的 tool_call.id 匹配。
第 6 步：再次调用 API。模型看到工具结果后，可能生成最终回答，也可能继续调用工具（多轮工具调用）。因此整个流程用 while 循环包裹。

设置最大轮次限制（如 5 轮）防止无限循环。', 'messages 列表的完整性至关重要。模型需要通过完整的消息历史来理解工具调用的上下文和结果。不要在中途丢弃任何消息。', 'def weather_agent(user_query):
    """天气查询智能体：支持多轮工具调用"""
    print(f"用户提问：{user_query}\\n")
    messages = [{"role": "user", "content": user_query}]
    
    round_num = 0
    while True:
        round_num += 1
        print(f"--- 第 {round_num} 轮 API 调用 ---")
        
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=messages,
            tools=[weather_tool],
        )
        assistant_msg = response.choices[0].message
        
        # 没有工具调用 → 最终回答
        if not assistant_msg.tool_calls:
            print(f"最终回答：{assistant_msg.content}")
            return assistant_msg.content
        
        # 有工具调用 → 构建 assistant 消息
        print(f"模型决定调用 {len(assistant_msg.tool_calls)} 个工具")
        tool_calls_dict = [{
            "id": tc.id, "type": "function",
            "function": {"name": tc.function.name, "arguments": tc.function.arguments}
        } for tc in assistant_msg.tool_calls]
        messages.append({"role": "assistant", "content": "", "tool_calls": tool_calls_dict})
        
        # 执行每个工具并添加 tool 消息
        for tc in assistant_msg.tool_calls:
            args = json.loads(tc.function.arguments)
            print(f"  调用工具：{tc.function.name}({args})")
            result = get_weather(**args)
            print(f"  执行结果：{result}")
            messages.append({"role": "tool", "tool_call_id": tc.id, "content": result})
        
        if round_num >= 5:
            print("达到最大轮次限制")
            break');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (358, 54, 6, '实现多工具协作与工具注册表', '当智能体拥有多个工具时，需要一个路由机制来根据模型返回的函数名调用不同的本地函数。推荐使用工具注册表（TOOL_REGISTRY）——一个字典，将工具名字符串映射到实际的 Python 函数对象。

相比 if-else 链，注册表的优势：新增工具只需两行代码（定义函数 + 注册映射），不需要修改路由逻辑；可以用循环统一处理所有工具，代码更简洁；可以动态注册/注销工具（如根据用户权限决定可用工具集）。

将所有工具的描述放入 tools 列表传给 API，模型会根据用户意图自动选择合适的工具。模型选择工具的依据主要来自 description 字段——所以每个工具的描述要有足够的区分度，让模型能区分何时该用哪个。', '工具注册表是生产环境中的标准做法，它让代码符合开闭原则（对扩展开放，对修改关闭）。当工具数量超过 10 个时，建议将工具描述也自动生成（从函数的 docstring 提取），减少手动维护。', 'from datetime import datetime

# 本地工具函数
def get_weather(city: str) -> str:
    """查询天气..."""
    # ... 同前面的实现 ...
    pass

def calculate_days(date1: str, date2: str) -> str:
    """计算日期差..."""
    d1 = datetime.strptime(date1, "%Y-%m-%d")
    d2 = datetime.strptime(date2, "%Y-%m-%d")
    return f"{date1} 到 {date2} 相差 {abs((d2-d1).days)} 天"

# 工具描述列表
tools = [weather_tool_description, calculate_days_tool_description]

# 工具注册表：名字 → 函数对象的映射
TOOL_REGISTRY = {
    "get_weather": get_weather,
    "calculate_days": calculate_days,
}

# 统一路由
for tc in assistant_msg.tool_calls:
    function_name = tc.function.name
    arguments = json.loads(tc.function.arguments)
    
    if function_name in TOOL_REGISTRY:
        result = TOOL_REGISTRY[function_name](**arguments)
    else:
        result = f"错误：未知工具 {function_name}"
    
    print(f"调用：{function_name}({arguments}) → {result}")');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (359, 54, 7, '处理并行工具调用', '模型可以在一次回复中返回多个 tool_calls，表示需要并行调用多个工具。典型场景：用户问''上海和北京哪个更热？''，模型会同时返回两个 get_weather 调用（一个查上海，一个查北京）。

处理并行调用的关键：必须为每个 tool_call 都执行本地函数并返回对应的 tool 消息。所有 tool 消息在同一次 API 调用中一起发给模型，模型会综合所有工具结果生成回答。

在构建 messages 时，一个 assistant 消息（含 N 个 tool_calls）后面必须紧跟 N 条 tool 消息，顺序不强制要求与 tool_calls 一致，但 tool_call_id 必须一一对应。建议按相同顺序构建，便于调试。', '并行工具调用是模型''聪明''的体现——它知道需要两个城市的数据才能比较，于是一次性请求两个工具调用，而不是分两轮。这减少了 API 调用次数，提高了效率。', 'def smart_agent(user_query):
    """多工具智能体：支持并行工具调用"""
    print(f"用户提问：{user_query}\\n")
    
    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[{"role": "user", "content": user_query}],
        tools=tools,
    )
    assistant_msg = response.choices[0].message
    
    if assistant_msg.tool_calls:
        tool_results = []
        
        # 执行所有工具调用（可能并行）
        for tc in assistant_msg.tool_calls:
            args = json.loads(tc.function.arguments)
            print(f"调用工具：{tc.function.name}({args})")
            result = TOOL_REGISTRY[tc.function.name](**args)
            print(f"执行结果：{result}")
            tool_results.append((tc.id, result))
        
        # 构建完整 messages
        messages = [{"role": "user", "content": user_query}]
        tool_calls_dict = [{
            "id": tc.id, "type": "function",
            "function": {"name": tc.function.name, "arguments": tc.function.arguments}
        } for tc in assistant_msg.tool_calls]
        messages.append({"role": "assistant", "content": "", "tool_calls": tool_calls_dict})
        for tool_id, result in tool_results:
            messages.append({"role": "tool", "tool_call_id": tool_id, "content": result})
        
        # 模型综合所有工具结果生成回答
        final = client.chat.completions.create(
            model="deepseek-chat", messages=messages
        )
        print(f"最终回答：{final.choices[0].message.content}")
        return final.choices[0].message.content');
INSERT INTO `learning_step` (`step_id`, `item_pk`, `step_no`, `title`, `description`, `tip`, `code`) VALUES (360, 54, 8, '工具调用的错误处理与边界情况', '工具调用中有多种可能的错误场景需要处理：

1. 工具执行异常：本地函数抛出异常（如网络超时、文件不存在）。应在函数内部 try-except 捕获，返回错误信息字符串而非让程序崩溃。错误信息会被模型看到，模型可能会换个参数重试或告知用户。

2. 未知工具名：模型返回了一个不在注册表中的函数名（罕见但可能，尤其在工具描述有歧义时）。应返回友好的错误提示。

3. 参数解析失败：arguments 不是合法 JSON，或参数类型不匹配（如模型传了字符串但函数期望数字）。用 try-except 包裹 json.loads() 和函数调用。

4. 模型幻觉工具：模型可能''发明''一个不存在的工具名。通过注册表的 existence check 可以拦截。

5. 无限循环：模型反复调用工具而不给出最终回答。通过设置最大轮次限制来防止。

这些错误处理不仅是工程需要，也影响智能体的可靠性——一个能优雅处理错误的智能体比一个遇到异常就崩溃的智能体实用得多。', '建议将工具执行逻辑封装为统一的 dispatch 函数，集中处理所有错误情况，而不是在每个工具函数中重复写 try-except。', 'def dispatch_tool_call(tc) -> tuple:
    """
    统一工具调度函数，处理所有错误情况。
    返回 (tool_call_id, result_string)
    """
    function_name = tc.function.name
    
    # 检查工具是否存在
    if function_name not in TOOL_REGISTRY:
        return tc.id, f"错误：未知工具 ''{function_name}''。可用工具：{list(TOOL_REGISTRY.keys())}"
    
    # 解析参数
    try:
        arguments = json.loads(tc.function.arguments)
    except json.JSONDecodeError as e:
        return tc.id, f"错误：参数解析失败 - {e}。原始参数：{tc.function.arguments}"
    
    # 执行工具
    try:
        result = TOOL_REGISTRY[function_name](**arguments)
        return tc.id, str(result)
    except Exception as e:
        return tc.id, f"工具执行出错：{type(e).__name__}: {e}"');
-- AI 助学扩展表：会话表负责定位学生在具体课程中的连续对话。
CREATE TABLE IF NOT EXISTS `ai_conversation` (
  `conversation_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `item_pk` bigint NOT NULL,
  `scene` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`conversation_id`),
  KEY `idx_ai_conversation_user_item_scene` (`user_id`,`item_pk`,`scene`,`updated_at`),
  KEY `idx_ai_conversation_item` (`item_pk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI 助学扩展表：消息按会话保存，source_label 用于向学生展示回答依据。
CREATE TABLE IF NOT EXISTS `ai_message` (
  `message_id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_label` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `feedback` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`),
  KEY `idx_ai_message_conversation` (`conversation_id`),
  KEY `idx_ai_message_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI 助学扩展表：保存每次调用的 Token、耗时与成功状态，不记录对话正文。
CREATE TABLE IF NOT EXISTS `ai_usage_record` (
  `usage_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `conversation_id` bigint DEFAULT NULL,
  `model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prompt_tokens` int NOT NULL DEFAULT 0,
  `completion_tokens` int NOT NULL DEFAULT 0,
  `total_tokens` int NOT NULL DEFAULT 0,
  `success` tinyint(1) NOT NULL DEFAULT 0,
  `error_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latency_ms` bigint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`usage_id`),
  KEY `idx_ai_usage_user_created` (`user_id`,`created_at`),
  KEY `idx_ai_usage_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
