# 需求规格说明书 — E-S3-RULE 规则白名单/项目级豁免 UI

> **Epic ID**: E-S3-RULE
> **Epic 名称**: 规则白名单/项目级豁免 UI
> **变更目录**: `.harness/changes/feat-rule-whitelist-20260701/`
> **创建日期**: 2026-07-01
> **对应 Sprint**: Sprint 3（M1 第三冲刺）
> **总工时**: 60h
> **优先级**: P1
> **关联文档**:
> - 冲刺合同：`.harness/changes/sprint-3/contract.md` § 5.1
> - 引擎规则体系：`engine/src/main/resources/rules/java/`（YAML 规则文件）
> - 工程架构：`engine/src/main/java/com/codesec/engine/rule/`
> - 现有后端模式：`backend/api/src/main/java/com/codesec/api/module/`

---

## 1. 背景

Sprint 2 交付的引擎支持 4 条内置 Java 规则（SQL 注入/硬编码密码/XSS/弱加密），但缺乏规则管理能力：

1. **全量扫描无豁免**：即使项目已知某些规则为误报，仍会反复扫描和告警
2. **无项目级规则配置**：不同项目对同一规则的容忍度不同（如老项目遗留代码 vs 新项目）
3. **规则开关靠改 YAML**：管理员需修改引擎规则文件并重启服务

需要一套规则白名单系统，允许安全管理员在 UI 上按项目管理规则豁免。

---

## 2. 需求描述

### 2.1 范围与边界

**范围内**：
- 后端规则 CRUD API（规则元数据管理）
- 项目-规则关联表（豁免/白名单）
- 规则管理页面（前端）
- 扫描引擎集成（白名单过滤）
- 规则导入（从引擎 YAML 同步）

**范围外**：
- ❌ 自定义规则编写 UI — M2
- ❌ 规则市场/模板库 — M3
- ❌ 规则测试沙箱 — M2
- ❌ 规则版本历史 — Sprint 4
- ❌ 批量规则操作（导入/导出 CSV）— Sprint 4

### 2.2 交付物清单

| # | 交付物 | 工时 |
|---|--------|------|
| T1 | 后端规则元数据 API（CRUD + 项目关联） | 20h |
| T2 | 规则白名单管理页面（前端） | 24h |
| T3 | 扫描引擎集成（白名单过滤） | 10h |
| T4 | 测试 + 文档 | 6h |

---

## 3. 技术方案

### 3.1 数据模型

```sql
CREATE TABLE rule_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id VARCHAR(128) NOT NULL UNIQUE,   -- e.g. "java/sql-injection-001"
    name VARCHAR(256) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    cwe VARCHAR(32),
    language VARCHAR(32) NOT NULL DEFAULT 'java',
    engine VARCHAR(32) NOT NULL DEFAULT 'self_sast',
    detection_type VARCHAR(16) NOT NULL DEFAULT 'ast',  -- ast | regex
    description TEXT,
    fix_suggestion TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    imported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule_enabled (enabled),
    INDEX idx_rule_severity (severity)
);

CREATE TABLE project_rule_exemption (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    reason TEXT,                              -- 豁免原因
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME,                      -- 可选：过期时间
    UNIQUE KEY uk_project_rule (project_id, rule_id),
    INDEX idx_exemption_project (project_id),
    FOREIGN KEY (rule_id) REFERENCES rule_metadata(id),
    FOREIGN KEY (project_id) REFERENCES repo(id)
);
```

### 3.2 后端 API

| Method | Path | 说明 |
|--------|------|------|
| `GET` | `/api/v1/rules` | 规则列表（分页+筛选） |
| `GET` | `/api/v1/rules/{id}` | 规则详情 |
| `PUT` | `/api/v1/rules/{id}` | 更新规则（启用/关闭） |
| `POST` | `/api/v1/rules/sync` | 从引擎 YAML 同步规则元数据 |
| `GET` | `/api/v1/projects/{id}/exemptions` | 项目豁免列表 |
| `POST` | `/api/v1/projects/{id}/exemptions` | 添加项目规则豁免 |
| `DELETE` | `/api/v1/projects/{id}/exemptions/{ruleId}` | 移除项目规则豁免 |

### 3.3 前端页面

- **规则管理页** (`RulesView.vue`):
  - 表格展示规则列表（rule_id/name/severity/language/engine/enabled）
  - 搜索/筛选（按 severity/language/engine）
  - 启用/关闭规则开关
  - 同步引擎规则按钮
- **项目豁免对话框**:
  - 嵌入项目设置页或独立弹窗
  - 搜索可豁免规则
  - 填写豁免原因 + 可选过期时间
- **扫描过滤**:
  - `EngineAdapterImpl.scanFiles()` 过滤掉被豁免的规则
  - 传递 projectId → 查询 `project_rule_exemption` → 从规则列表中排除

### 3.4 规则同步

- `POST /api/v1/rules/sync` 读取 `engine/src/main/resources/rules/java/*.yaml`
- 解析 YAML → `RuleMetadataEntity` → INSERT OR UPDATE
- 引擎规则文件是源数据，同步操作不会删除已有规则（避免误删）

---

## 4. 验收标准

- [ ] `GET /api/v1/rules` 返回分页规则列表，支持按 severity/language 筛选
- [ ] `POST /api/v1/rules/sync` 从引擎 YAML 导入规则，幂等（重复调用无副作用）
- [ ] `POST /api/v1/projects/{id}/exemptions` 添加豁免成功，`GET` 返回含新增纪录
- [ ] `DELETE /api/v1/projects/{id}/exemptions/{ruleId}` 移除豁免成功
- [ ] 前端规则管理页面表格展示完整，启用开关生效
- [ ] 项目豁免对话框可搜索规则并提交
- [ ] 扫描引擎集成后，豁免规则不出现在结果中
- [ ] `mvn -pl api test` 全部通过
