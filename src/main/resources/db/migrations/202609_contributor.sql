-- 新增贡献者档案表。
-- 用途：维护项目贡献者名单，供「项目贡献者」页面展示、供管理员在维护页管理。
-- 注意：贡献者权限复用 sys_user.role，其定义为 varchar(20)，
--       新增取值 CONTRIBUTOR 无需修改表结构，本脚本不涉及 sys_user。
-- 幂等：可重复执行。
-- Usage:
--   mysql -uroot -p labcore < migrations/202609_contributor.sql

SET @has_contributor_table = (
  SELECT COUNT(*)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'contributor'
);

SET @ddl = IF(
  @has_contributor_table = 0,
  'CREATE TABLE `contributor` (
     `contributor_id` bigint       NOT NULL AUTO_INCREMENT COMMENT ''贡献者ID'',
     `user_id`        bigint       DEFAULT NULL COMMENT ''关联平台账号，NULL 表示无账号的外部贡献者'',
     `display_name`   varchar(100) DEFAULT NULL COMMENT ''展示名，仅外部贡献者使用；有账号时取 sys_user.display_name'',
     `role_title`     varchar(100) DEFAULT NULL COMMENT ''职责标题，如：内容维护、前端开发'',
     `module_scope`   varchar(255) DEFAULT NULL COMMENT ''负责模块，如：学习卡片、模板管理'',
     `description`    text COMMENT ''贡献描述'',
     `github_url`     varchar(255) DEFAULT NULL COMMENT ''GitHub 主页'',
     `join_date`      date         DEFAULT NULL COMMENT ''加入项目时间'',
     `sort_order`     int          NOT NULL DEFAULT 0 COMMENT ''展示排序，越小越靠前'',
     `visible`        tinyint      NOT NULL DEFAULT 1 COMMENT ''是否在前台展示'',
     `created_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''记录创建时间'',
     `updated_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''记录更新时间'',
     PRIMARY KEY (`contributor_id`),
     KEY `idx_contributor_user` (`user_id`),
     KEY `idx_contributor_sort` (`sort_order`),
     CONSTRAINT `fk_contributor_user`
       FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`)
       ON DELETE SET NULL ON UPDATE CASCADE
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''项目贡献者档案''',
  'SELECT ''contributor table already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
