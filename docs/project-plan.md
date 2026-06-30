# code-sec 整体项目规划 v1.0

> **文档目的**: 把现有 `docs/architecture.md`（设计层）、`sprint-1/tasks.md`（执行层）、`feat-exploitability-judger-20260630/`（在制品）三层信息综合成一份**可执行的项目路线图**。
> **受众**: 项目 Owner / PM / Tech Lead / 投资人 / 面试官
> **状态**: Draft v1.0（待 PM + Tech Lead 双签）
> **生成日期**: 2026-06-30
> **关联文档**: `docs/architecture.md`（架构详细设计）、`docs/c4-architecture.md`（C4 模型）、`.harness/changes/sprint-1/`（Sprint 1 收尾）、`.harness/changes/feat-exploitability-judger-20260630/`（Sprint 2 在制品）

---

## 0. TL;DR（一页纸摘要）

| 维度 | 结论 |
|------|------|
| **产品定位** | 一站式应用安全左移平台，覆盖 SAST/SCA/人工审计/CI 卡点/合规报表 6 大场景 |
| **核心差异化** | ① 人工审计工作台一等公民 ② 可利用性判定（exploitability-aware）③ CI/CD 流水线强卡点 |
| **目标规模** | 36 周（5 里程碑），10 人团队，Sprint 节奏 2 周 / 600h |
| **当前进度** | M1 / Sprint 1 完成；M1 / Sprint 2 在跑（F-EXJ-001 核心 6 阶段完成） |
| **核心风险** | 后端业务服务（Spring Boot）**未实现**——架构描述完整但代码层缺位；M1 末才能"端到端跑通"是空中楼阁 |
| **本规划新增** | ① 后端补全计划 ② Sprint 2 选定 3 个 Epic（F-EXJ-001 收尾 + 真实 GitLab + ES 检索）③ 决策待办 7 项 |
| **下一步** | 等用户对 Sprint 2 选定 + 后端补全优先级给出决策 |

---

## 1. 产品愿景与差异化

### 1.1 一句话定位

> **code-sec = 一站式应用安全左移平台**：用一套平台覆盖 SAST 扫描、SCA 依赖检测、人工审计、漏洞闭环、CI/CD 卡点、合规报表 6 大场景，替代企业内零散本地工具。

### 1.2 四大不可妥协约束

| 约束 | 含义 | 体现 |
|------|------|------|
| **左移** | commit 阶段就发现，不留到上线后 | MR 增量扫描 + CI 卡点 |
| **统一** | 多语言/多仓库/多团队一套平台 | 引擎插件化 + 仓库适配器 |
| **闭环** | 发现→审计→修复→复测→归档全链路 | 工单状态机 + 审计工作台 |
| **可运营** | 大盘/报表/合规/规范沉淀 | 月度报告 + 知识库 + 规则库 |

### 1.3 三个核心差异化（为什么不是"又一套 SonarQube"）

1. **人工审计工作台是核心** — 自动化只能发现 30% 漏洞，业务逻辑漏洞依赖人工
2. **可利用性判定（Exploitability-aware）** — 区分真漏洞 vs 幽灵 CVE，把 1000 个告警压缩到 50 个
3. **CI/CD 流水线即卡点** — 不只是出报告，MR 阶段直接阻断或放行

---

## 2. 当前状态评估（Code vs Plan 差距分析）

> 这是本规划**最重要的章节**——把"架构说什么"和"代码有什么"对齐。

### 2.1 模块成熟度矩阵

| 架构模块（docs/architecture.md § 6） | 架构描述完整度 | 代码实现完整度 | 状态 |
|------|------|------|------|
| **Module 1 仓库管理** | ⭐⭐⭐⭐⭐ | ⚪ 0% | 仅文档，未实现 |
| **Module 2 多引擎扫描** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐☆☆ | 引擎核心（自研 SAST）已实，但调度/队列/沙箱未实 |
| **Module 3 审计工作台** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 前端 5 视图 + 审计组件完整，后端 mock |
| **Module 4 漏洞工单** | ⭐⭐⭐⭐⭐ | ⚪ 0% | 仅文档，未实现 |
| **Module 5 CI/CD 卡点** | ⭐⭐⭐⭐ | ⚪ 0% | 仅文档，未实现 |
| **Module 6 规则/误报管理** | ⭐⭐⭐⭐ | ⭐☆☆☆☆ | YAML 规则引擎实，UI/白名单/灰度未实 |
| **Module 7 风险大盘** | ⭐⭐⭐⭐ | ⭐☆☆☆☆ | DashboardView 仅有 mock 骨架 |
| **Module 8 RBAC** | ⭐⭐⭐⭐ | ⚪ 0% | 仅文档 |
| **可利用性判定（ExploitabilityJudger）** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **已完成（F-EXJ-001）** |
| **多语言支持** | ⭐⭐⭐ | ⚪ 0% | 仅 Java |

> **结论**：架构层 80% 完整、清晰；代码层仅 ~25%。**M1 末"端到端跑通"目标在当前代码状态下不可达**——后端业务服务（Spring Boot）完全缺位，前端用 mock 数据演示。

### 2.2 现有代码资产清单

| 资产 | 路径 | 成熟度 | 评估 |
|------|------|--------|------|
| **SAST 引擎** | `engine/` | 生产可用 | Java 17 + JavaParser + SnakeYAML + 4 detectors（SQL 注入/XSS/弱加密/硬编码密码） |
| **Exploitability Judger** | `engine/src/main/java/com/codesec/engine/judge/` | 单元测试 120 通过 | 3 算法（可达性/可控性/框架保护）+ YAML 驱动保护规则 |
| **规则库** | `engine/src/main/resources/rules/` | 4 个 framework yml | Spring Security/MyBatis/Hibernate/ESAPI |
| **样例代码** | `engine/examples/sample-code/` | 5 案例 | Exploitable/DeadCode/Protected/Indirect/Untouched |
| **审计工作台前端** | `frontend/src/views/WorkbenchView.vue` | 功能完整 | 漏洞详情 + 审计动作 + POC + 修复代码编辑器 |
| **漏洞列表前端** | `frontend/src/views/AuditQueueView.vue` | 功能完整 | 列表 + 过滤 + Monaco 代码预览 |
| **大盘前端** | `frontend/src/views/DashboardView.vue` | 骨架 | mock 数据，ECharts 已集成 |
| **Pinia stores** | `frontend/src/stores/` | 完整 | vuln.ts/audit.ts/ui.ts |
| **Mock API** | `frontend/src/api/mock/` | 完整 | 25 条样例漏洞，MSW handler |

### 2.3 缺失资产清单（优先级排序）

| # | 缺失项 | 影响 | 建议时机 |
|---|--------|------|----------|
| 1 | **后端业务服务（Spring Boot）** | 阻塞 M1 端到端演示 | **Sprint 2 立即补** |
| 2 | **数据库 schema + Flyway migration** | 阻塞所有数据持久化 | Sprint 2 |
| 3 | **OAuth2/JWT 鉴权** | 阻塞 RBAC 演示 | Sprint 2 |
| 4 | **Webhook 接收端点** | 阻塞 CI/CD 卡点 | Sprint 2 |
| 5 | **规则白名单 UI** | 阻塞误报管理 | Sprint 3 |
| 6 | **PDF 审计底稿导出** | 阻塞合规演示 | Sprint 3 |
| 7 | **ES 检索后端** | 阻塞搜索功能 | Sprint 3 |
| 8 | **K8s 沙箱集群** | 阻塞生产部署 | M2 |

---

## 3. 架构与技术选型确认

> 现有 `architecture.md` § 11 已经做过详细技术选型论证。本节**只做"接受/调整"决策**。

### 3.1 技术选型（无争议，沿用）

| 维度 | 选型 | 状态 |
|------|------|------|
| 主语言 | Java 17 + Spring Boot 3 | ✅ 接受 |
| 引擎辅助 | Python 3.11（备用） | ✅ 接受 |
| 前端 | Vue 3 + TypeScript + Ant Design Vue | ✅ 接受 |
| 数据库 | MySQL 8 + Redis 7 + MinIO + ES 8 | ✅ 接受 |
| 任务队列 | RabbitMQ（暂用 sync mock） | ✅ 接受 |
| 调度 | xxl-job | ✅ 接受 |
| 部署 | Docker Compose → K8s | ✅ 接受 |
| 鉴权 | OAuth2 + JWT + RBAC | ✅ 接受 |

### 3.2 需要 Owner 决策的技术点（见 § 6 决策待办）

- 自研 vs 商业引擎配比（CodeQL/SonarQube License）
- 沙箱隔离粒度（K8s Job vs Docker Compose）
- 审计员配比（甲方 vs 自营）
- 私有化 vs SaaS 部署形态

---

## 4. 里程碑路线图（M1-M5）

> 沿用 `architecture.md` § 13.2 / § 14 路线图。本节**补充 Sprint 级粒度**和**实际状态**。

```
周次:  0----2----4----6----8----10---12---14---16---18---20---22---24---26---28---30---32---34---36
       |  M1 MVP (8w/4 Sprint) |    M2 引擎扩展 (6w)    |   M3 报表+知识库 (6w)  |  M4 商业化准备 (6w)  | M5 商业化 (10w) |
              ↑                ↑       ↑              ↑        ↑              ↑         ↑              ↑      ↑
           仓库+Webhook    自研SAST+   CodeQL+      多语言+   月度报告+       规则灰度+  第三方CI+      OpenAPI+  多租户+
           +RBAC          工单+审计   DepCheck     K8s沙箱   知识库MVP       第三方CI   OpenAPI平台     计费+API
```

### 4.1 M1 MVP（T+8 周 / 4 个 Sprint）— **当前所在里程碑**

**目标**: 端到端可演示的垂直切片（Java 单语言，Docker 部署）

| Sprint | 时间 | 主题 | 关键交付 | 状态 |
|--------|------|------|----------|------|
| **Sprint 1** | T+0~2w | 基础 + 垂直切片 | 仓库 CRUD + GitLab Webhook + 自研 SAST 引擎 4 detector + 审计工作台 v1 + 工单状态机 + RBAC + 基础大盘 | ✅ **完成**（含 docs/team.md + risks.md） |
| **Sprint 2** | T+2~4w | 完善 + 多语言基础 | **当前 Sprint**，候选 8 Epic 待选 | 🟡 进行中 |
| **Sprint 3** | T+4~6w | CodeQL + 多语言 | 接入 CodeQL、Go/Python 适配、RabbitMQ 真队列、真实 OAuth2、MR 评论机器人 | ⬜ 未启动 |
| **Sprint 4** | T+6~8w | SCA + K8s | Dependency-Check、K8s 沙箱、M1 末 demo | ⬜ 未启动 |

**M1 完成定义 (DoD)**:
- [ ] 端到端可演示：GitLab MR → Webhook → 引擎 → 工单 → 审计 → 修复 → 复测
- [ ] 自研 SAST 覆盖 4 漏洞类型，precision ≥ 80%，recall ≥ 90%
- [ ] 10 万行 ≤ 30s 性能达标（或 M1.5 优化）
- [ ] Docker Compose 一键起
- [ ] 100% 通过质量门禁 QG-1~8

### 4.2 M2 引擎扩展（T+8~14 周 / 3 个 Sprint）

**目标**: 5 语言 + 双引擎（自研 + CodeQL）+ SCA + K8s 沙箱

| Sprint | 主题 | 关键交付 |
|--------|------|----------|
| Sprint 5 | CodeQL + SCA | CodeQL 适配器（Java/JS/Python），Dependency-Check，结果归一化 |
| Sprint 6 | 多语言 + K8s 沙箱 | tree-sitter Go/PHP/JS 适配，K8s Job 沙箱集群 |
| Sprint 7 | M2 收尾 | 多引擎去重优化、灰度发布准备、5 语言端到端验证 |

### 4.3 M3 报表 + 知识库（T+14~20 周 / 3 个 Sprint）

**目标**: 月度报告自动生成、规则灰度发布、知识库 MVP

| Sprint | 主题 | 关键交付 |
|--------|------|----------|
| Sprint 8 | 月度报告 | 报表自动生成（Jinja2 + WeasyPrint）、业务线评分 |
| Sprint 9 | 规则灰度 | 灰度发布流程、白名单管理 UI |
| Sprint 10 | 知识库 MVP | 漏洞样本库、修复模板、CWE 库对接 |

### 4.4 M4 商业化准备（T+20~26 周 / 3 个 Sprint）

**目标**: 第三方 CI 深度集成、OpenAPI 平台

| Sprint | 主题 | 关键交付 |
|--------|------|----------|
| Sprint 11 | 第三方 CI | Jenkins / 云效 / 自研流水线适配器 |
| Sprint 12 | OpenAPI 平台 | API 文档站、SDK 生成、限流配额 |
| Sprint 13 | M4 收尾 | 商业化 demo、性能压测、合同模板 |

### 4.5 M5 商业化（T+26~36 周 / 5 个 Sprint）

**目标**: 多租户、计费、API 开放平台

| Sprint | 主题 | 关键交付 |
|--------|------|----------|
| Sprint 14 | 多租户数据隔离 | 租户 schema 隔离、跨租户 RBAC |
| Sprint 15-16 | 计费引擎 | 用量计量、账单生成、支付对接 |
| Sprint 17-18 | 商业化上线 | 法务合规、客服体系、运维监控、SLA 99.9% |

---

## 5. Sprint 2 详细规划（当前 Sprint）

### 5.1 Sprint 2 选定的 Epic

> 8 个候选 Epic 中（sprint-1/tasks.md § Sprint 2 Preview），本规划推荐选 **3 个**（容量 480h cap 约束）。
> 选 Epic 的三原则：(1) 阻塞 M1 端到端（最优先） (2) 解锁多个下游 (3) 复用已有资产。

| 优先级 | Epic ID | 名称 | 工时估算 | 选/不选 | 理由 |
|--------|---------|------|----------|---------|------|
| **P0** | **E-S2-CRITICAL** | **后端业务服务补全**（新） | 200h | ✅ 选 | **M1 端到端阻塞项**——架构有但代码无；无后端则前端 mock 永远无法切真 |
| **P0** | E-S2-002 | 真实 GitLab 联调（替换 WireMock） | 80h | ✅ 选 | 替代 mock，真实 webhook + diff API + MR 评论；现有 WireMock 可快速升级 |
| **P1** | E-S2-001 | ES 全文检索接入 | 120h | ✅ 选 | 漏洞/代码片段检索是审计员高频需求；后续 Sprint 必备 |
| P1 | E-S2-003 | KMS 接入（Token 加密升级） | 40h | ⬜ 不选 | 推到 Sprint 3（先 AES-256 yml 兜底） |
| P1 | E-S2-004 | 规则白名单 / 项目级豁免 | 60h | ⬜ 不选 | 推到 Sprint 3 |
| P1 | E-S2-005 | 大盘 v1（ECharts 集成） | 40h | ⬜ 不选 | 前端骨架已有，Sprint 3 接真实数据 |
| P2 | E-S2-006 | 审计底稿 PDF 导出 | 50h | ⬜ 不选 | Sprint 3 |
| P2 | E-S2-007 | 多语言基础（Go + Python） | 100h | ⬜ 不选 | Sprint 3 与 CodeQL 一起做 |
| P2 | E-S2-008 | CI/CD 通用 Webhook 适配器 | 40h | ⬜ 不选 | Sprint 4 |

**Sprint 2 容量核算**:
- 总容量 600h（10 人 × 10 天 × 6h）
- 任务 cap 480h（80% 保护）
- 本规划选定 3 个 Epic 总工时：200+80+120 = **400h**（≤ 480h ✅）
- 剩余 80h：Code Review 30h + Bug Bash 20h + Buffer 30h

### 5.2 F-EXJ-001（Exploitability Judger）收尾

> 核心 6 阶段已完成（120/120 测试通过），但需在 Sprint 2 内完成：
> - **Extended 阶段（7-10）**: 集成测试、归档
> - **QG-6 性能差距（40s vs 30s）**: M1.5 优化，本 Sprint 不阻塞
> - **变更目录归档**: 写最终 summary.md，更新 feature_list.json 为 done

| 子任务 | 工时 | Owner |
|--------|------|-------|
| F-EXJ-001 收尾（Extended 阶段 + summary.md 归档） | 16h | 陈静（Engine TL） |
| F-EXJ-001 文档同步更新（architecture.md § 2.4、c4-architecture.md § 7.7.5） | 4h | 张伟（Backend TL） |

### 5.3 Sprint 2 任务分解

#### E-S2-CRITICAL: 后端业务服务补全（200h, P0）

**目标**: 实现 `architecture.md` § 6 描述的 7 个核心业务服务，让 M1 端到端可演示

| 子任务 | 工时 | Owner | 依赖 |
|--------|------|-------|------|
| 后端 monorepo + Spring Boot 3 多模块骨架 | 8h | 张伟 | - |
| 数据模型 + Flyway V1（10 张核心表） | 16h | 张伟 | - |
| Module 1：仓库管理 API（CRUD + GitLab Token 加密） | 24h | 李娜 | 数据模型 |
| Module 4：工单 API（状态机 + 流转） | 20h | 刘洋 | 数据模型 |
| Module 8：RBAC（JWT + 4 角色 + 拦截器） | 20h | 张伟 | 数据模型 |
| Module 3：审计 API（标记动作 + POC 上传 MinIO） | 24h | 刘洋 | 数据模型 + 工单 |
| Module 2：扫描任务 API（创建/查询/取消） | 20h | 王强 | 仓库 API + 引擎 |
| 引擎集成（EngineAdapter + 异步任务 + 同步队列） | 24h | 王强 + 陈静 | 扫描 API |
| 前端切真 API（替换 mock → 真实后端） | 24h | 周婷 | 后端 API |
| 集成测试（10 个核心场景 E2E） | 20h | 张伟 + 赵敏 | 全部 |

**验收标准**:
- [ ] `mvn test` 通过
- [ ] 10 张表 Flyway V1 migration 成功
- [ ] 端到端演示：GitLab mock MR → Webhook → 引擎 → 工单 → 审计 → 修复
- [ ] 4 角色 RBAC 拦截正常
- [ ] 集成测试全绿

#### E-S2-002: 真实 GitLab 联调（80h, P0）

**目标**: 替换 WireMock，接入真实 GitLab dev 集群

| 子任务 | 工时 | Owner | 依赖 |
|--------|------|-------|------|
| GitLab 真实环境联调（ngrok / 内网） | 8h | 王强 | - |
| Webhook HMAC 签名升级 | 8h | 张伟 | - |
| MR Diff 提取（真实 GitLab API） | 16h | 王强 | GitLab 联调 |
| 增量扫描触发（按 diff file 范围） | 16h | 陈静 + 王强 | MR Diff |
| 真实 GitLab MR 评论机器人 | 16h | 王强 | MR Diff |
| E2E 集成测试（真实 GitLab） | 16h | 王强 + 赵敏 | 全部 |

#### E-S2-001: ES 全文检索接入（120h, P1）

**目标**: 漏洞/代码片段全文检索

| 子任务 | 工时 | Owner | 依赖 |
|--------|------|-------|------|
| ES 集群部署（Docker Compose） | 8h | 徐峰 | - |
| ES 索引设计（vuln / file_snippet） | 12h | 张伟 | - |
| 后端搜索 API（query DSL） | 24h | 刘洋 | ES 索引 |
| 漏洞同步到 ES（异步事件） | 16h | 王强 | 后端搜索 API + 工单 |
| 前端检索 UI（顶部搜索框 + 结果页） | 32h | 周婷 + 吴昊 | 后端搜索 API |
| 性能压测（百万级数据） | 16h | 徐峰 | 全部 |
| 文档（architecture.md § 5.4 数据层补充） | 12h | 张伟 | 全部 |

### 5.4 Sprint 2 时间线（10 个工作日）

| Day | 关键节点 | 负责人 |
|-----|----------|--------|
| 1 | Sprint kickoff + 后端 monorepo 启动 + 三个 Epic 任务分配 | 赵敏 |
| 2-3 | 数据模型冻结 + Flyway V1 + 后端骨架 | 张伟 |
| 4-5 | Module 1/4/8 后端 API 并行（李娜/刘洋/张伟） | 各 Backend |
| 6-7 | Module 2/3 后端 + 引擎集成（王强/刘洋/陈静） | Backend + Engine |
| 7-8 | 真实 GitLab 联调 + ES 索引设计（王强/张伟） | Backend |
| 8-9 | 前端切真 API + 检索 UI（周婷/吴昊） | Frontend |
| 9-10 | 集成测试 + Bug Bash + Sprint 2 Demo 准备 | 全员 + 赵敏 |

### 5.5 Sprint 2 风险

| 风险 | 等级 | 缓解 |
|------|------|------|
| **后端补全进度延期** | 高 | Day 4 EOD 检查进度，落后则 E-S2-001 推到 Sprint 3 |
| **真实 GitLab 联调阻塞** | 中 | 保留 WireMock 作为 fallback，Day 6 EOD 评估 |
| **ES 性能不达标** | 中 | 先实现基本搜索，性能优化推 M1.5 |
| **前端切真 API 数据兼容问题** | 中 | 渐进切换（先 WorkbenchView → AuditQueueView → Dashboard） |

---

## 6. 决策记录（已锁定 2026-06-30）

> 所有决策已与 Owner 确认。任何变更需新决策记录 + 双签。

| # | 决策项 | 决定 | 影响 / 行动项 |
|---|--------|------|---------------|
| **D1** | 后端补全是 Sprint 2 一等公民吗？ | ✅ **是** | Sprint 2 主轴 200h 补后端（E-S2-CRITICAL） |
| **D2** | Sprint 2 三 Epic 选择 | ✅ **后端+真实 GitLab+ES**（400h） | M1 末端到端 demo 必备 |
| **D3** | CodeQL License | ⚠️ **需另购** | **行动项**：Sprint 2 末启动采购流程（Sprint 3 接入前到位） |
| **D4** | 部署形态 | ✅ **私有化优先** | M1 末 K8s 部署。SaaS M5 再说 |
| **D5** | KMS 选型 | ✅ **阿里云 KMS** | Sprint 3 接入；Sprint 2 末前用 AES-256 yml 兜底 |
| **D6** | 多租户时机 | ✅ **M5 才做** | M1-M4 数据模型**不加** tenant_id 字段（避免提前负担） |
| **D7** | M1.5 性能优化时机 | ✅ **Sprint 3 末启动**（2 周专项） | Sprint 3 任务 28-30 留出 buffer 给优化 |

### 6.1 D3 行动项追踪

- **触发条件**: Sprint 2 末（2026-07-10）前必须确认 CodeQL License 采购
- **Owner**: PM（赵敏）+ 法务
- **失败回退**: 若 License 不可用，M2 改用 Semgrep（开源）替代 CodeQL；自研 SAST 加重 Rule 数量补位
- **关联文件**: `.harness/changes/sprint-2/contract.md` 中标注为"待决策依赖"

---

## 7. 总体风险登记

> 在 `architecture.md` § 15 + `sprint-1/risks.md` 基础上，补充本规划发现的新风险。

| ID | 风险 | 等级 | 概率 | 影响 | Owner | 缓解 |
|----|------|------|------|------|-------|------|
| **R-NEW-1** | 后端业务服务缺位导致 M1 不能端到端 | **极高** | 高 | 极高 | 张伟 | Sprint 2 优先补全（见 § 5.3 E-S2-CRITICAL） |
| **R-NEW-2** | Sprint 1 交付的"端到端"实际是 mock 演示 | 高 | 100% | 中 | 张伟 | Sprint 2 切真 API（前端 24h 切换任务） |
| **R-NEW-3** | 真实 GitLab 联调未做，性能不达标 | 中 | 中 | 高 | 王强 | Sprint 2 必做，WireMock 兜底 |
| **R-NEW-4** | CodeQL License 不可用 | 中 | 低 | 高 | PM（赵敏）+ 法务 | 提前确认（D3） |
| **R-NEW-5** | 自研 SAST 引擎在 5 语言扩展时性能下降 | 中 | 中 | 中 | 陈静 | M2 启动专项 benchmark |
| **R-NEW-6** | 10 人团队持续 36 周倦怠 | 中 | 中 | 高 | 赵敏 | 每 Sprint retro，必要时减范围 |
| **R-NEW-7** | 项目仓库非 git 仓库（当前状态） | 低 | 100% | 低 | 徐峰 | 立即 `git init` + 提交当前状态 |

> 完整风险登记见 `architecture.md` § 15 + `sprint-1/risks.md`（R1-R15）。

---

## 8. 关键成功指标（KPI）

| 阶段 | 指标 | 目标 | 度量方式 |
|------|------|------|----------|
| **Sprint 2 末** | 后端 API 完成度 | 7/7 模块可调通 | `curl` smoke test + 集成测试 |
| **Sprint 2 末** | 端到端 demo 通过率 | ≥ 90% 步骤成功 | Demo 3 次彩排 |
| **M1 末** | 自研 SAST precision/recall | ≥ 80% / ≥ 90% | 50 标注 + 5 万行 spring-petclinic |
| **M1 末** | 扫描性能 | 10 万行 ≤ 30s | BENCHMARK.md |
| **M1 末** | 演示成功率 | 100% 3 次彩排 | 现场 demo + backup video |
| **M2 末** | 语言覆盖 | Java + Go + Python + JS/TS + PHP | 5 语言样例库 |
| **M2 末** | 多引擎去重准确率 | ≥ 95% | 手工验证 |
| **M3 末** | 月度报告自动生成 | ≥ 1 份 | 定时任务产出 |
| **M5 末** | 商业化指标 | 首签客户 ≥ 1，ARR ≥ ¥X | 销售数据 |

---

## 9. 文档管理

| 文档 | 职责 | 维护 Owner |
|------|------|-----------|
| `docs/project-plan.md` | **本文件**——项目级路线图 | Owner Agent |
| `docs/architecture.md` | 架构详细设计 | 张伟 |
| `docs/c4-architecture.md` | C4 模型 | 张伟 |
| `.harness/changes/{change}/` | 单个特性/Epic 的执行档案 | 特性 Owner |
| `.harness/wiki/` | 业务知识库 | PM |

**版本管理**:
- 本文件每次 Sprint 末更新（v1.0 → v1.1 → ...）
- 重大决策变更需 PM + Tech Lead 双签
- 与 `architecture.md` 不一致时以本文件路线图为准，架构文档作为详细参考

---

## 10. 下一步

1. **立即（Owner 决策）**:
   - 决策 D1-D7（§ 6）
   - 确认 Sprint 2 三 Epic 选择（D2）
   - 启动后端 monorepo 骨架（张伟，Day 1）

2. **Sprint 2 启动会议（Day 1）**:
   - PM 宣讲本规划 § 5 Sprint 2 详细任务
   - 三个 Epic Owner 立项（写 `.harness/changes/sprint-2-e1/`、`-e2/`、`-e3/`）
   - F-EXJ-001 收尾任务分配

3. **Sprint 2 末**:
   - Sprint 2 demo + retro
   - 更新本文件 v1.1
   - 启动 Sprint 3 规划

---

**文档结束** — 本规划作为 M1 后续推进的单一真相源（SSOT），任何重大变更需更新本文档并通知全员。
