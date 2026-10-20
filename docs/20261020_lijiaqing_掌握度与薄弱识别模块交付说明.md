# 知识点掌握度计算与薄弱知识点识别 模块交付说明

- 分支：`lijiaqing`（基线：上一组 `caiyaqi` 分支，提交 26a2d8a）
- 交付人：李佳晴（第二组）
- 日期：2026-10-20
- 模块范围：**知识点掌握度计算 + 薄弱知识点识别**（学习分析与推荐 → 学习能力画像）
- 明确不做：个性化学习计划模块（与 caiyaqi 的 learning_plan 表撞名不同构，后续组另行处理）

---

## 一、模块解决什么问题

1. 把"学习记录 + 考核成绩"换算成每个知识点的**掌握度分值**（0~100）；
2. 按**预警阈值**自动识别**薄弱知识点**（高风险），并给出风险等级与统计；
3. 学生端查看自己的掌握度；管理员端查看全员风险总览、薄弱知识点名单，并可**手动触发重算**。

## 二、核心算法（与页面、接口一一对应）

### 1. 学习得分（学习资源 → 知识点，按权重加权）

| 学习记录状态 | 单资源得分 |
|---|---|
| 已完成（complete_time 非空） | 100 |
| 已开始（有记录但未完成） | 40 |
| 未开始（无记录） | 0 |

学习分 = Σ(资源权重 × 资源得分) / Σ(资源权重)

### 2. 考核得分（能力考核 → 知识点，按权重加权）

- 仅 `GRADED` 且 `score` 非空的提交计分：`score / 考核总分 × 100`；
- 未提交 / 已提交未批改（SUBMITTED 等）→ **0 分，但权重照算**（区别于"无考核关联"）。

### 3. 综合掌握度

| 关联情况 | 综合掌握度 |
|---|---|
| 学习 + 考核都有关联 | 0.4 × 学习分 + 0.6 × 考核分 |
| 仅学习关联 | 学习分 |
| 仅考核关联 | 考核分 |
| 无任何关联 | 0 |

### 4. 风险分级（薄弱识别）

| 条件 | 等级 |
|---|---|
| 无任何关联 | UNKNOWN（未知） |
| 掌握度 < 预警阈值 | HIGH（高风险，薄弱） |
| 阈值 ≤ 掌握度 < 阈值 + 15 | MEDIUM（中风险） |
| 掌握度 ≥ 阈值 + 15 | LOW（低风险） |

> 阈值存在 knowledge_point.warning_threshold，默认 60，智能体知识点可单独调（如 AI-001/AI-005 为 65）。

## 三、新增内容清单

### 后端（`src/main/java/com/hwz/`，共 18 个文件）

- `personalized/controller/`：KnowledgeMasteryStudentController、KnowledgeMasteryAdminController、KnowledgePointAdminController
- `personalized/service/`：KnowledgeMasteryService（核心计算）、KnowledgeMasteryOverviewService（管理员总览）、KnowledgeMasteryQueryService（详情查询）、KnowledgePointAdminService（知识点管理）
- `personalized/dto/`：KnowledgePointAdminDtos
- `common/entity/`：KnowledgePoint、StudentKnowledgeMastery、LearningItemKnowledgePoint、AssignmentKnowledgePoint、KnowledgePointPrerequisite
- `common/mapper/`：对应 5 个 MyBatis-Plus Mapper

### 前端（`web-study-1.0.1/package/src/`，共 5 个新文件 + 2 处修改）

- 新页面：`views/MyKnowledgeMastery.vue`（学生端"我的掌握度"）、`views/MasteryRiskOverview.vue`（管理员"掌握度风险预警"）、`views/KnowledgePointManagement.vue`（管理员"知识点管理"）
- 新接口：`api/masteryRisk.js`、`api/knowledgePointAdmin.js`
- 修改：`router/index.js`（学生路由 my-mastery + 2 条管理员路由）、`components/Layout/Index.vue`（侧边栏"我的掌握度" + 顶栏下拉分组："内容管理→知识点管理"、"数据监控→掌握度风险预警/学习计划风险"）

### 数据库（`src/main/resources/db/`，共 2 个 SQL）

- `migrations/20261020_personalized_mastery.sql`：新增 5 张表 + 10 个种子知识点（覆盖 6 大学习分类）+ 15 条资源关联 + 3 条考核关联 + 3 条前置关系，幂等可重复执行
- `demo/seed_mastery_test.sql`：8 条测试用例数据（只动测试学生 4/5，幂等 DELETE+INSERT）

## 四、主要接口

| 接口 | 角色 | 说明 |
|---|---|---|
| GET `/api/personalized/student/mastery/me` | 学生 | 我的知识点掌握度 |
| GET `/api/personalized/admin/mastery/students` | 管理员 | 全员掌握度风险总览 |
| GET `/api/personalized/admin/mastery/students/{id}` | 管理员 | 指定学生掌握度详情 |
| POST `/api/personalized/admin/mastery/students/{id}/recalculate` | 管理员 | 手动重算该生全部知识点掌握度 |
| GET/POST/PUT `/api/personalized/admin/knowledge-points...` | 管理员 | 知识点管理（分页/统计/详情/增改/启停/绑定资源与考核） |

## 五、8 条测试用例验收结果（已实测通过）

测试学生：S1 = demo2024_001 李明(user_id=4)，S2 = demo2024_002 王芳(user_id=5)

| 用例 | 场景 | 学习分 | 考核分 | 综合 | 期望风险 | 实测结果 | 结论 |
|---|---|---|---|---|---|---|---|
| T1 | S1×KP-AI-001 双关联满分 | 100 | 100 | 100 | LOW | 100 / LOW | PASS |
| T2 | S2×KP-AI-001 混合加权（57完成+56开始，考核50） | 70 | 50 | 58 | HIGH | 58 / HIGH | PASS |
| T3 | S1×KP-AI-002 全零（未学习+未考核） | 0 | 0 | 0 | HIGH | 0 / HIGH | PASS |
| T4 | S1×KP-AI-003 只学满分 | 100 | — | 100 | LOW | 100 / LOW | PASS |
| T5 | S2×KP-AI-003 只学开始 | 40 | — | 40 | HIGH | 40 / HIGH | PASS |
| T6 | S1×KP-AI-004 只考 GRADED 60（=阈值边界） | — | 60 | 60 | MEDIUM | 60 / MEDIUM | PASS |
| T7 | S2×KP-AI-004 只考未批改（SUBMITTED 无分→0） | — | 0 | 0 | HIGH | 0 / HIGH | PASS |
| T8 | S1×KP-CR-001 权重 2:1（42完成权2 + 49开始权1） | 80 | — | 80 | LOW | 80 / LOW | PASS |

验证方式：后端启动后调用 recalculate 接口重算 S1/S2，读取详情接口的 masteryScore / riskLevel 逐条比对。

## 六、如何部署运行

1. 导库（幂等，可重复执行）：
   ```bash
   mysql -uroot -p123456 --default-character-set=utf8mb4 labcore < src/main/resources/db/migrations/20261020_personalized_mastery.sql
   mysql -uroot -p123456 --default-character-set=utf8mb4 labcore < src/main/resources/db/demo/seed_mastery_test.sql
   ```
2. 后端：`mvn spring-boot:run`（JDK8 + Maven3.8，端口 8080）。
3. 前端：`cd web-study-1.0.1/package && npm install && npm run dev`（Vite 端口 5175，CORS 已允许）。
4. 管理员登录后：顶栏头像下拉 → 数据监控 → **掌握度风险预警** / 内容管理 → **知识点管理**；学生登录后：侧边栏 → **我的掌握度**。
5. 新产生学习/考核数据后，管理员在"掌握度风险预警"页对学生执行"重算"即可刷新掌握度。

## 七、与上一组（caiyaqi）的边界与说明

- 本分支新增 5 张画像表；**未建计划表**（caiyaqi 的 learning_plan 表与画像版学习计划表撞名不同构，为避免破坏其已交付功能，本组不建计划相关表，计划模块由后续组另行设计）。
- `MasteryRiskOverview.vue` 中原"生成计划"按钮保留入口，但点击提示"个性化学习计划模块将在后续版本提供"。
- 未修改 `main` 分支，未改动 caiyaqi 已交付的任何既有功能文件（仅在本组新增页面/接口/表范围内改动）。

## 八、触发方式说明

当前为**管理员手动重算**（简单可靠）。演进路线（后续组可做）：
手动重算 → 页面懒加载自动重算 → 学习/考核事件驱动增量重算 → 定时任务全量重算。
