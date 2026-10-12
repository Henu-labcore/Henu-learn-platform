-- 收藏/学习计划增量升级。执行前备份并核对库名；不含 DROP、TRUNCATE 或原表 ALTER。
-- 原 sys_user / learning_item / learning_record 只作逻辑引用，不加限制原功能的外键。
-- 已有旧版计划表的服务器需先核对结构，不能盲目执行。
CREATE TABLE IF NOT EXISTS learning_plan (
  plan_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
  plan_name VARCHAR(100) NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', active_marker TINYINT NULL DEFAULT 1,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_learning_plan_user_active (user_id, active_marker),
  KEY idx_learning_plan_user (user_id), KEY idx_learning_plan_status_end (status,end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 提醒偏好属于个人；不改变原学习记录或系统通知配置。
CREATE TABLE IF NOT EXISTS learning_plan_reminder_setting (
  user_id BIGINT NOT NULL PRIMARY KEY,enabled TINYINT NOT NULL DEFAULT 1,due_days TINYINT NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS learning_plan_notification (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,plan_id BIGINT NOT NULL,item_pk BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,event_date DATE NOT NULL,title VARCHAR(400) NOT NULL,message VARCHAR(1000) NOT NULL,is_read TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_learning_plan_notification(user_id,plan_id,item_pk,type,event_date),KEY idx_learning_plan_notification_user(user_id,is_read,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 学习心得单独持久化；item_pk=0 用于计划总结，不是原学习内容编号。
CREATE TABLE IF NOT EXISTS learning_plan_feedback (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,plan_id BIGINT NOT NULL,item_pk BIGINT NOT NULL,
  reflection TEXT NOT NULL,difficulty TEXT NOT NULL,next_step TEXT NOT NULL,rating TINYINT NOT NULL,version INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_learning_plan_feedback(user_id,plan_id,item_pk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 收藏分类只属于个人，不改变 learning_item.category。
CREATE TABLE IF NOT EXISTS favorite_category (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,name VARCHAR(30) NOT NULL,
  UNIQUE KEY uk_favorite_category(user_id,name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS favorite (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,item_pk BIGINT NOT NULL,
  title_snapshot VARCHAR(255) NOT NULL,category_id BIGINT NULL,note VARCHAR(1000) NOT NULL DEFAULT '',version INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_favorite_user_item(user_id,item_pk),KEY idx_favorite_category(user_id,category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS favorite_tag (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,name VARCHAR(30) NOT NULL,
  UNIQUE KEY uk_favorite_tag(user_id,name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS favorite_tag_rel (
  favorite_id BIGINT NOT NULL,tag_id BIGINT NOT NULL,PRIMARY KEY(favorite_id,tag_id),KEY idx_favorite_tag(tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS learning_plan_item (
  plan_item_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  plan_id BIGINT NOT NULL, item_pk BIGINT NOT NULL,
  target_date DATE NOT NULL, sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_learning_plan_item (plan_id,item_pk), KEY idx_learning_plan_item_target (target_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
