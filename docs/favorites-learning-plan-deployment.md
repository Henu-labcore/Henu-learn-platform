# 收藏与学习计划：运行、人工验收和部署

## 使用哪份源码

本次功能在 `caiyaqi` 分支维护，以 GitHub 的 `main` 为基础。功能开发和试用目录为 `Henu-learn-platform-main`；2026-10-11 将选定增量整理到独立 Git 克隆目录 `Henu-learn-platform-git`，用于提交和协作。
未覆盖旧 `labcore-frontend`、`labcore-deploy` 或桌面项目。执行 Git 命令应先进入独立克隆目录，不要在包含其他项目的父目录执行整目录 `git add .`。

原版备份位于 `D:\文档数据\New project\outputs\labcore-module-backup-20261009`，含源码和数据库敏感数据，禁止上传 GitHub。
源码 ZIP 中前端 `src/views` 等条目恢复时应映射到 `web-study-1.0.1/package/src`，不要盲目解压覆盖根目录。

## 本机运行

日常数据库：`labcore_github`，原数据库已保留，仅增加模块表。数据库密码不是平台登录密码，平台仍使用原账号。

首次克隆后需在本机创建被 Git 忽略的 `src/main/resources/application.properties`，以仓库中的 `application.properties.example` 为模板。通过环境变量设置实际的 `LABCORE_DB_URL`、`LABCORE_DB_USERNAME`、`LABCORE_DB_PASSWORD`；不要把本机密码提交到仓库。普通克隆不会包含他人的私有数据库配置，默认数据库名也不一定是 `labcore_github`。

在项目目录打开 PowerShell：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-learning-modules.ps1
```

脚本交互输入数据库密码（不回显、不保存）。启动前要求本地端口 8080、5175 空闲；不会关闭占用端口的其他项目。
如系统找不到 Java/Node，可指定 `-JavaPath 'C:\Program Files\Java\jdk-20\bin\java.exe' -NodePath 'D:\cyq\node.exe'`。
浏览器访问 `http://127.0.0.1:5175`。原 5173 页面不是本次验收入口。
本地临时登录签名密钥只保存在进程环境，重启脚本后需重新登录。正式服务器必须沿用安全、持久的原密钥配置。
脚本打印的两个 PID 用于关闭本次服务；先确认对应进程命令行再用 `Stop-Process -Id <PID>`，不要按程序名关闭所有 Java/Node。

重新构建（先设置实际的 `JAVA_HOME`，Maven 与 npm 在 PATH 中）。Windows 上先核对并停止本项目后台 PID，再打包；运行中的 Java 可能锁定 JAR，导致打包无法重命名。不要关闭其他项目的进程。

```powershell
mvn package
cd .\web-study-1.0.1\package
npm ci
npm run build
```

普通 `mvn package` 运行单元测试，跳过需要独立测试库的集成测试。
本次完整验收另行显式启用了克隆库集成测试，初版 20 项通过，人工反馈修订后共 22 项通过；不要对日常库执行写入测试。
浏览器开发验收脚本为 `scripts/verify-learning-modules.cjs`，依赖可用的 Playwright 和 Edge，并强制要求 `labcore_module_check_*` 测试库。

## 人工验收顺序

每一步通过后再继续；出现错误请记录账号、页面、操作、提示及截图，不要为了测试清空原表。

| 顺序 | 操作 | 预期 |
| --- | --- | --- |
| 1 | 用原平台账号登录，进入知识、实验、学习数据、考核、贡献者页面 | 原导航和已有数据正常；开始学习、练习和详情入口仍在 |
| 2 | 在知识卡片收藏，刷新后进入“我的收藏” | 星标和列表保留；取消收藏只移除收藏，不删除知识 |
| 3 | 新建个人分类，编辑收藏分类、多个标签和备注，再组合检索 | 只返回符合条件项；删分类后收藏转为未分类，不消失 |
| 4 | 另一浏览器登录同一账号并刷新；换另一账号检查 | 同账号数据一致，不同账号收藏/分类/计划/反馈/提醒互不串号 |
| 5 | 若有旧浏览器收藏，确认归属后点导入 | 有效编号幂等导入；不清除旧 localStorage；不可用内容跳过 |
| 6 | 创建/调整学习计划，检查任务日期、进度、历史 | 每账号最多一个进行中计划；任务来自原已发布内容；不改原学习记录 |
| 6a | 创建时只选以前已学完的条目，再加入一项未完成条目 | 全部已完成时提示且不创建；部分已完成时需确认，创建后留在进行中并显示待完成任务风险 |
| 7 | 正常完成原学习任务，再刷新计划页面 | 进度更新；已完成任务可写心得/困难/下一步/评分，全部完成可写总结 |
| 8 | 第二设备查看反馈；用两个设备同时编辑同一计划或收藏 | 刷新后读取保存结果；旧版本保存提示冲突，不静默覆盖 |
| 9 | 设置任务当天/近期截止，打开“学习提醒”，保存提醒偏好和已读 | 提醒扫描后显示；偏好及已读保留；同一事件不重复生成 |
| 10 | 完成或取消有提醒的任务/计划，调整截止日期 | 失效催办不继续显示；完成提醒独立于截止提醒开关 |
| 11 | 用管理员原账号检查原后台和“学习计划风险” | 原权限保留；普通账号不能访问管理接口 |
| 12 | 完成一次真实学习、练习及考核提交/文件下载回归 | 原流程照常运行；这一项需要你人工实际操作确认 |

跨设备指同一后端数据库下的数据同步，不是自动联网到你本机：另一电脑/手机需能访问部署后的平台地址。
提醒为站内提醒，默认提前 1 天，可配置 1～7 天；后端启动约 30 秒后首次扫描，再按约 60 秒间隔扫描。
每轮最多 100 份计划，规模较大时轮转会延长生成延迟；前端也约每分钟刷新。页面关闭不发送系统推送。

## 修改范围和数据库

原业务 Java 源码、原构建依赖不变。原前端只有路由、布局、知识卡片、知识页 4 处增量接入。
新增文件使用原计划命名与 `studyStore.js`，代码注释注明个人分类、只读原学习记录、独立提醒调度及版本冲突保护。

唯一增量 SQL：`src/main/resources/db/migrations/20261009_learning_modules.sql`。
新增表：`favorite`、`favorite_category`、`favorite_tag`、`favorite_tag_rel`、`learning_plan`、`learning_plan_item`、`learning_plan_feedback`、`learning_plan_notification`、`learning_plan_reminder_setting`。
不修改原 `learning_item.category`，不覆盖原 `learning_record.complete_remark`，不新增限制原内容删除的外键。
本机原 13 张表在执行增量脚本前后导出哈希相同。

## 服务器发布（尚未执行）

1. 明确服务器实际源码版本、Java/MySQL、服务管理方式、前端站点目录、数据库名及持久化配置；先做原包、配置、数据库和上传文件备份。
2. 用 `SHOW TABLES` 核对 9 个模块表是否已存在。若存在，逐表 `SHOW CREATE TABLE` 和增量脚本对照，特别是 `learning_plan.version`、唯一键和通知/反馈字段。`IF NOT EXISTS` 不会升级已有旧表；遇到不同结构必须先设计独立迁移，不能直接覆盖或删表。
3. 若这 9 张表完全没有，确认目标库后，在 MySQL Workbench 打开增量 SQL，仅对正确库执行。不要重新导入整份交接 SQL，不要执行 DROP/TRUNCATE/原表 ALTER。
4. 上传本次 `target/LabCore-0.0.1-SNAPSHOT.jar` 和 `web-study-1.0.1/package/dist`。按原服务器方式配置密码、签名密钥、数据库 URL、CORS 和上传目录，不把本机端口或密码写进源码。
5. 按原服务管理方式重启后端，检查日志和 `/actuator/health`，发布前端；保留原反向代理 `/api` 与 `/lite`。新表必须先准备，否则新增模块接口会报错。
6. 在真实平台地址按上表验收，再让另一设备使用同账号确认同步。检查数据库与上传路径仍指向原服务器持久数据。

回滚：恢复备份的原后端包、前端包与配置，重启并复查。新增模块表暂时保留，不删除；通常无需恢复整个数据库，避免覆盖回滚期间的正常业务数据。
数据库备份、运行日志和测试输出可能含个人数据；不要提交 GitHub。只提交源码、增量 SQL、文档及必要的忽略规则，提交前逐文件检查差异。
