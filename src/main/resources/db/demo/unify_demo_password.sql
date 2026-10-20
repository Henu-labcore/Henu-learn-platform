-- =========================================================
-- 演示用户密码统一脚本
--
-- 用途：
--   本平台所有演示账号（普通学生）密码统一为 admin123，
--   方便老师 / 同学 / 评审现场演示时直接登录，无需逐个询问。
--
-- 为什么需要：
--   sys_user 表中普通用户（role = 'USER'）原本密码各异，
--   演示切换账号（李明、王芳、张伟等）时容易忘记密码。
--   本脚本一次把所有普通用户密码改为统一口令。
--
-- 注意：
--   1. 只改普通用户（role = 'USER'），管理员（admin）不受影响；
--   2. 使用的哈希是 admin 密码的 bcrypt 哈希（明文 admin123），
--      复用同一个哈希即可，无需为每个用户单独加密；
--   3. 脚本幂等，可重复执行；
--   4. 如后续新增演示账号，重新执行本脚本即可。
--
-- 执行方式（在项目根目录）：
--   mysql -uroot -p -D labcore < src/main/resources/db/demo/unify_demo_password.sql
-- =========================================================

USE labcore;

UPDATE sys_user
SET password = '$2b$10$0.2JGuMoQNUsa1zYGIPN.Ok0LbywQixjeN9AgTT7fSYJYXXw2WyFC'
WHERE role = 'USER';

-- 校验：应等于当前普通用户总数（演示库中为 46）
SELECT COUNT(*) AS unified_count
FROM sys_user
WHERE role = 'USER'
  AND password = '$2b$10$0.2JGuMoQNUsa1zYGIPN.Ok0LbywQixjeN9AgTT7fSYJYXXw2WyFC';
