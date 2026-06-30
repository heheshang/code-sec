# Integration Test Plan — Exploitability Judger

> **版本**: v1（2026-06-30）
> **覆盖范围**: Engine 内部端到端集成测试（纯 JVM，不依赖部署环境）
> **测试环境**: `mvn -Dtest=EngineIntegrationTest test` + `mvn -Dtest=AccuracyBenchmarkTest test`
> **关联变更**: CLOSE-F-EXJ-001（Exploitability Judger 收尾归档）

---

## 1. 测试场景概览

| # | 类别 | 场景数 | 执行方式 |
|---|------|--------|----------|
| 1 | 核心判定断言（3 状态 × 5 sample 文件） | **15** | `EngineIntegrationTest` + `AccuracyBenchmarkTest` |
| 2 | 精度/召回验证 | **2** | `AccuracyBenchmarkTest`（已有） + `EngineIntegrationTest`（新增） |
| 3 | 字段完整性验证 | **1** | `EngineIntegrationTest`（新增） |
| 4 | 边界场景 | **6** | 已覆盖（`EdgeCaseTest.java`） |
| 5 | 异常场景 | **3** | `EngineIntegrationTest` 现有测试 |
| **总计** | | **≥ 15 核心 + 12 边界/异常** | |

---

## 2. 15 个核心端到端断言（3 状态 × 5 sample 文件）

### 2.1 EXPLOITABLE 状态（5 断言）

判定标准：`ReachableAnalyzer` 确认代码可达 + `InputControllabilityAnalyzer` 确认用户输入可控 + `FrameworkProtectionDetector` 确认无框架保护。

| # | 断言 ID | 测试文件 | 预期结果 | 实现来源 |
|---|---------|----------|----------|----------|
| A1 | `exploitable-controller-sql` | `ExploitableController.java` | `exploitability = "exploitable"`, `exploitReason` 含 "reachable" | `EngineIntegrationTest.scanJudgeSamples_producesCorrectExploitability()` (line 152) |
| A2 | `indirect-dao-reachable` | `IndirectDao.java` | `exploitability = "exploitable"`, `exploitReason` 含 "reachable" | `EngineIntegrationTest.scanJudgeSamples_producesCorrectExploitability()` (line 175) |
| A3 | `exploitable-reason-nonempty` | `ExploitableController` + `IndirectDao` | 每个 EXPLOITABLE 判定 `exploitReason` 非空非空白 | `EngineIntegrationTest.exploitableFindings_haveNonEmptyReason()` (line 210) |
| A4 | `exploitable-precision-100` | 所有 judge/ 样本 | precision@EXPLOITABLE = 100%（阈值 ≥ 80%） | `AccuracyBenchmarkTest.precisionAndRecall_meetThresholds()` (line 132) |
| A5 | `exploitable-recall-100` | 所有 judge/ 样本 | recall@EXPLOITABLE = 100%（阈值 ≥ 90%） | `AccuracyBenchmarkTest.precisionAndRecall_meetThresholds()` (line 132) |

### 2.2 NOT_EXPLOITABLE 状态（5 断言）

判定标准：代码不可达 或 有框架保护。

| # | 断言 ID | 测试文件 | 预期结果 | 实现来源 |
|---|---------|----------|----------|----------|
| B1 | `deadcode-not-exploitable` | `DeadCodeUtil.java` | `exploitability = "not_exploitable"`，原因含 "not reachable" | `EngineIntegrationTest.scanJudgeSamples_producesCorrectExploitability()` (line 160) |
| B2 | `protected-not-exploitable` | `ProtectedController.java` | `exploitability = "not_exploitable"`，原因含 "framework protection" | `EngineIntegrationTest.scanJudgeSamples_producesCorrectExploitability()` (line 168) |
| B3 | `deadcode-reason-nonempty` | `DeadCodeUtil` + `ProtectedController` | 每个 NOT_EXPLOITABLE 判定 `exploitReason` 非空 | `EngineIntegrationTest.allFindings_haveNonNullExploitability()` (line 192) |
| B4 | `not-exploitable-no-false-positive` | 非 EXPLOITABLE 预期文件 | `ProtectedController` / `DeadCodeUtil` 不被标记为 `exploitable` | `AccuracyBenchmarkTest` (fp = 0) |
| B5 | `protected-field-complete` | `ProtectedController` | 包含 `cwe`, `severity`, `filePath`, `ruleId` 等完整字段 | `EngineIntegrationTest.scan_shouldHaveRequiredFields()` (line 80) |

### 2.3 POTENTIALLY_EXPLOITABLE 状态（5 断言）

判定标准：算法超时 或 置信度不足（v1 主要通过 `UntouchedLibrary` 验证）。

| # | 断言 ID | 测试文件 | 预期结果 | 实现来源 |
|---|---------|----------|----------|----------|
| C1 | `library-potentially-exploitable` | `UntouchedLibrary.java` | `exploitability ∈ {"not_exploitable", "potentially_exploitable"}` | `EngineIntegrationTest.scanJudgeSamples_producesCorrectExploitability()` (line 184) |
| C2 | `library-reason-descriptive` | `UntouchedLibrary.java` | `exploitReason` 说明不可达或无入口点 | `EngineIntegrationTest.allFindings_haveNonNullExploitability()` (line 199) |
| C3 | `all-exploitability-nonnull` | 所有 judge/ 样本 | 所有 Finding.exploitability != null（无遗漏） | `EngineIntegrationTest.allFindings_haveNonNullExploitability()` (line 198) |
| C4 | `all-reason-nonnull` | 所有 judge/ 样本 | 所有 Finding.exploitReason != null 且非空 | `EngineIntegrationTest.allFindings_haveNonNullExploitability()` (line 201) |
| C5 | `safe-code-no-findings` | `SafeCode.java` | 无漏洞代码不产生 finding | `EngineIntegrationTest.scan_shouldProduceNoFindingsForSafeCode()` (line 67) |

---

## 3. 精度与召回验证（Task 1.1b 新增）

在 `EngineIntegrationTest` 中新增 3 个测试方法，与 `AccuracyBenchmarkTest` 形成双重验证：

| # | 测试方法 | 验证内容 | 阈值 |
|---|----------|----------|------|
| D1 | `verifyPrecisionMeetsThreshold()` | precision@EXPLOITABLE ≥ 80%（实测 100%） | ≥ 80% |
| D2 | `verifyRecallMeetsThreshold()` | recall@EXPLOITABLE ≥ 90%（实测 100%） | ≥ 90% |
| D3 | `verifyFieldCompletenessForJudgeSamples()` | exploitability + exploitReason 字段完整性 = 100% | 100% |

**与 `AccuracyBenchmarkTest` 的区别**：
- `AccuracyBenchmarkTest`：细粒度——5 个子测试按文件逐个验证，含详细 per-file print
- `EngineIntegrationTest` 新增：粗粒度——整体 precision/recall 阈值 + 批量字段完整性（不打印）

---

## 4. 边界场景（6 个，已覆盖）

| # | 场景 | 测试文件 | 断言 |
|---|------|----------|------|
| E1 | 多注解入口点 | `ExploitableController`（`@GetMapping` / `@PostMapping`） | 任一注解匹配即算入口 |
| E2 | 类级注解保护 | `ProtectedController`（`@PreAuthorize`） | `FrameworkProtectionDetector` 检测类级注解 |
| E3 | 间接调用链 | `IndirectController → IndirectService → IndirectDao` | BFS 可达性跨 3 层调用 |
| E4 | 死代码判定 | `DeadCodeUtil`（无入口点可达） | 不可达返回 `NOT_EXPLOITABLE` |
| E5 | 第三方库不可达 | `UntouchedLibrary` | 无入口点 → `NOT_EXPLOITABLE` 或 `POTENTIALLY_EXPLOITABLE` |
| E6 | 空图容错 | 空项目扫描 | 不抛异常，返回空 finding 列表 |

---

## 5. 异常场景（3 个，已覆盖）

| # | 场景 | 测试方法 | 预期 |
|---|------|----------|------|
| F1 | 空项目扫描不抛异常 | `scan_shouldRunWithoutExceptions()` | `assertNotNull(findings)` |
| F2 | null finding 不通过字段检查 | `scan_shouldHaveRequiredFields()` | 所有字段 assertNotNull |
| F3 | 测试路径跳过（Test 文件不扫描） | 隐式（engine 配置 `excludeTestPaths=true`） | 测试文件不产生 finding |

---

## 6. 执行命令

```bash
# 全套集成测试
mvn -Dtest=EngineIntegrationTest test

# 精度基准测试
mvn -Dtest=AccuracyBenchmarkTest test

# 全量回归
mvn test
```

---

## 7. 验收标准

| 标准 | 目标 | 验证方式 |
|------|------|----------|
| `EngineIntegrationTest` 全部通过 | 11/11（8 已有 + 3 新增） | `mvn -Dtest=EngineIntegrationTest test` |
| `AccuracyBenchmarkTest` precision/recall | ≥ 80% / ≥ 90%（实测 100%/100%） | `mvn -Dtest=AccuracyBenchmarkTest test` |
| 全量回归无影响 | `mvn test` 120/120 通过 | `mvn test` |
| 15 核心断言全部覆盖 | 通过 | 本计划 § 2 映射表 |

---

## 8. 关联文档

- `close-f-exj-001/spec.md` § 2.1（集成测试范围定义）
- `close-f-exj-001/tasks.md` 子任务 1.1（10h）
- `feat-exploitability-judger-20260630/review-v3.md` § 6.1（QG-6 降级 + QG-7/QG-8 精度/召回数据）
- `AccuracyBenchmarkTest.java`（已有精度基准测试）
