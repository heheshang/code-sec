# 任务拆解 — E-S3-RULE 规则白名单/项目级豁免 UI

> 基于 `spec.md`（E-S3-RULE）拆解的可执行任务清单。
> **总工时**: 60h

---

## 任务列表

### 任务 1：后端规则元数据 API + 数据模型

- **优先级**: P1
- **估计工时**: 20h
- **涉及模块**: `backend/api/`
- **涉及文件**:
  - `backend/api/src/main/resources/db/migration/V4__rule_metadata.sql` — 2 张表 DDL（新增）
  - `backend/api/src/main/java/com/codesec/api/domain/entity/RuleMetadataEntity.java`（新增）
  - `backend/api/src/main/java/com/codesec/api/domain/entity/ProjectRuleExemptionEntity.java`（新增）
  - `backend/api/src/main/java/com/codesec/api/domain/repository/RuleMetadataRepository.java`（新增）
  - `backend/api/src/main/java/com/codesec/api/domain/repository/ProjectRuleExemptionRepository.java`（新增）
  - `backend/api/src/main/java/com/codesec/api/module/rule/RuleController.java`（新增）
  - `backend/api/src/main/java/com/codesec/api/module/rule/RuleService.java`（新增）
  - `backend/api/src/main/java/com/codesec/api/module/rule/dto/` — DTO 包（新增）
  - `backend/api/src/main/java/com/codesec/api/module/rule/RuleSyncService.java` — YAML 同步逻辑（新增）
- **验收标准**:
  - [ ] Flyway V4 DDL 创建成功，2 张表含索引和外键
  - [ ] `GET /api/v1/rules` 返回分页列表，支持 severity/language/engine 筛选
  - [ ] `GET /api/v1/rules/{id}` 返回规则详情
  - [ ] `PUT /api/v1/rules/{id}` 可启用/关闭规则
  - [ ] `POST /api/v1/rules/sync` 从引擎 YAML 导入，幂等
  - [ ] `GET /api/v1/projects/{id}/exemptions` 返回豁免列表
  - [ ] `POST /api/v1/projects/{id}/exemptions` 添加豁免，含原因和过期时间
  - [ ] `DELETE /api/v1/projects/{id}/exemptions/{ruleId}` 移除豁免
  - [ ] `mvn -pl api compile` 通过
- **依赖任务**: 无

---

### 任务 2：规则管理前端页面

- **优先级**: P1
- **估计工时**: 24h
- **涉及模块**: `frontend/`
- **涉及文件**:
  - `frontend/src/api/rule.ts` — 规则 API 客户端（新增）
  - `frontend/src/views/RulesView.vue` — 规则管理页面（新增）
  - `frontend/src/components/rule/RuleSyncButton.vue` — 同步按钮组件（新增）
  - `frontend/src/components/rule/RuleExemptionDialog.vue` — 豁免弹窗（新增）
  - `frontend/src/components/rule/RuleTable.vue` — 规则表格组件（新增）
  - `frontend/src/stores/rule.ts` — Pinia store（新增）
  - `frontend/src/router/index.ts` — 添加 /rules 路由
- **验收标准**:
  - [ ] RulesView 表格展示 rule_id/name/severity/language/engine/enabled
  - [ ] 表格可按 severity/language/engine 筛选，支持搜索
  - [ ] 启用/关闭规则开关，即时生效
  - [ ] 同步按钮触发 YAML 导入，显示同步进度
  - [ ] 豁免弹窗可搜索规则、填写原因、设置过期时间
  - [ ] 路由 /rules 可访问，TopBar 有导航入口
  - [ ] `npm run build` 通过
- **依赖任务**: T1

---

### 任务 3：扫描引擎集成白名单过滤

- **优先级**: P1
- **估计工时**: 10h
- **涉及模块**: `backend/engine-adapter/` + `backend/api/`
- **涉及文件**:
  - `backend/engine-adapter/src/main/java/com/codesec/engineadapter/EngineAdapterImpl.java` — 添加 projectId 参数过滤
  - `backend/api/src/main/java/com/codesec/api/module/scan/ScanService.java` — 扫描时传入项目豁免列表
  - `backend/api/src/main/java/com/codesec/api/module/rule/RuleService.java` — 添加 `getExemptedRuleIds(projectId)`
- **验收标准**:
  - [ ] 扫描时传递 projectId，引擎自动过滤豁免规则
  - [ ] 豁免规则的 finding 不会出现在扫描结果中
  - [ ] 豁免记录在 scanning log 中标记为 "skipped (exempted)"
  - [ ] 无豁免时扫描行为不变
- **依赖任务**: T1

---

### 任务 4：测试 + 文档

- **优先级**: P1
- **估计工时**: 6h
- **涉及模块**: `backend/api/` + `frontend/`
- **涉及文件**:
  - `backend/api/src/test/java/com/codesec/api/module/rule/RuleControllerTest.java`（新增）
  - `backend/api/src/test/java/com/codesec/api/module/rule/RuleServiceTest.java`（新增）
  - `backend/api/src/test/java/com/codesec/api/module/rule/RuleSyncServiceTest.java`（新增）
- **验收标准**:
  - [ ] RuleControllerTest 覆盖 CRUD + 豁免
  - [ ] RuleSyncServiceTest 覆盖 YAML 解析 + 幂等同步
  - [ ] `mvn -pl api test` 全部通过
- **依赖任务**: T1, T2, T3

---

## 排期

| 任务 | 优先级 | 工时 | 依赖 | 计划开始 |
|------|--------|------|------|----------|
| T1: 后端规则 API | P1 | 20h | - | D3 |
| T2: 前端规则页面 | P1 | 24h | T1 | D4 |
| T3: 引擎过滤集成 | P1 | 10h | T1 | D5 |
| T4: 测试 | P1 | 6h | T1,T2,T3 | D6 |
