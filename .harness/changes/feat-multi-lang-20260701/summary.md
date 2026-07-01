# 变更摘要 — E-S3-MULTI 多语言引擎扩展

> 整个变更的 Single Source of Truth。

## 基本信息

- **需求名称**：E-S3-MULTI 多语言引擎扩展（Go/Python tree-sitter）
- **Epic ID**：E-S3-MULTI
- **变更类型**：feat
- **日期**：20260701
- **总工时**：100h
- **Sprint**：Sprint 3（M1 第三冲刺）
- **Owner**：Application Owner Agent（Sisyphus）

## 阶段执行状态

| 阶段 | 范围 | 状态 | 轮次 | 备注 |
|------|------|------|------|------|
| 需求分析 | Core | ✅ | - | spec.md v1 |
| 需求评审 | Core | ✅ | 1 | 一轮通过 |
| 编码实现 | Core | ✅ | - | 全部 6 子任务完成 |
| 编码评审 | Core | ⬜ | - | 待 Evaluator Agent |
| 单元测试编写 | Core | ✅ | - | 7 新测试（4 Go + 3 Python），原有 124 测试零回归 |
| 单元测试 CI | Core | ✅ | - | mvn test: 131/131 通过 |
| 集成测试 | Extended | ⬜ | - | 待编码评审通过后 |
| 部署验证 | Extended | ⬜ | - | |
| 灰度发布 | Extended | ⬜ | - | |
| 交付确认 | Extended | ⬜ | - | |

## 评审记录

| 评审类型 | 轮次 | 结论 | MUST FIX | LOW | INFO |
|----------|------|------|----------|-----|------|
| 需求评审 | 1 | ✅ 通过 | 0 | 0 | 0 |
| 编码评审 | 1 | ⬜ 待评审 | - | - | - |

## 变更文件清单

### T1: tree-sitter 集成
| 文件路径 | 变更类型 | 说明 |
|----------|----------|------|
| engine/src/main/java/.../languages/TreeSitterLibraryLoader.java | 新增 | JNI 本地库加载（静默降级） |
| engine/src/main/java/.../languages/TreeSitterAdapter.java | 新增 | tree-sitter 解析基类 |

### T2/T3: Go/Python AstParser
| 文件路径 | 变更类型 | 说明 |
|----------|----------|------|
| engine/src/main/java/.../languages/GoLanguage.java | 新增 | Go AstParser（支持 .go） |
| engine/src/main/java/.../languages/PythonLanguage.java | 新增 | Python AstParser（支持 .py） |

### T4: 规则 + Detector
| 文件路径 | 变更类型 | 说明 |
|----------|----------|------|
| engine/src/main/resources/rules/go/hardcoded-password-001.yml | 新增 | Go 硬编码密码规则 |
| engine/src/main/resources/rules/go/command-injection-001.yml | 新增 | Go 命令注入规则 |
| engine/src/main/resources/rules/python/sql-injection-001.yml | 新增 | Python SQL 注入规则 |
| engine/src/main/resources/rules/python/unsafe-eval-001.yml | 新增 | Python unsafe eval 规则 |
| engine/src/main/java/.../detector/impl/GoCommandInjectionDetector.java | 新增 | Go 命令注入检测器 |
| engine/src/main/java/.../detector/impl/PythonSqlInjectionDetector.java | 新增 | Python SQL 注入检测器（含 taint tracking） |
| engine/src/main/java/.../detector/impl/PythonUnsafeEvalDetector.java | 新增 | Python unsafe eval 检测器 |
| engine/src/main/java/.../Engine.java | 修改 | 注册 Go/Python parser + detector |
| engine/src/main/java/.../rule/RuleLoader.java | 修改 | 支持多语言 classpath 规则发现 |
| engine/src/main/java/.../rule/RuleRegistry.java | 修改 | 支持 varargs classpath 加载 |
| engine/src/main/java/.../cli/CliRunner.java | 修改 | CLI 同时加载 java/go/python 规则 |

### T5: 示例 + 测试
| 文件路径 | 变更类型 | 说明 |
|----------|----------|------|
| engine/examples/sample-code-go/hardcoded_password.go | 新增 | Go 硬编码密码正例 |
| engine/examples/sample-code-go/command_injection.go | 新增 | Go 命令注入正例/负例 |
| engine/examples/sample-code-go/safe_code.go | 新增 | Go 安全代码负例 |
| engine/examples/sample-code-python/sql_injection.py | 新增 | Python SQL 注入正例/负例 |
| engine/examples/sample-code-python/unsafe_eval.py | 新增 | Python unsafe eval 正例/负例 |
| engine/examples/sample-code-python/safe_code.py | 新增 | Python 安全代码负例 |
| engine/examples/output/expected-findings-go.json | 新增 | Go 期望输出 |
| engine/examples/output/expected-findings-python.json | 新增 | Python 期望输出 |
| engine/src/test/java/.../detector/impl/GoDetectorTest.java | 新增 | 4 Go 检测单元测试 |
| engine/src/test/java/.../detector/impl/PythonDetectorTest.java | 新增 | 4 Python 检测单元测试 |

### T6: 文档
| 文件路径 | 变更类型 | 说明 |
|----------|----------|------|
| engine/MULTI-LANG.md | 新增 | 多语言扩展指南 |

### 测试配置文件更新
| 文件路径 | 变更类型 | 说明 |
|----------|----------|------|
| EngineIntegrationTest.java | 修改 | 加载 java/go/python 规则 |
| AccuracyBenchmarkTest.java | 修改 | 加载 java/go/python 规则 |
| PerformanceBenchmarkTest.java | 修改 | 加载 java/go/python 规则 |

## CI 信息

- **构建编号**：N/A（本地）
- **测试用例数**：131 / **通过**：131 / **失败**：0
- **构建结果**：BUILD SUCCESS
- **编译**：mvn compile ✅

## 例外情况

1. **tree-sitter JNI 使用静默降级**：当前无原生库，Go/Python 使用纯文本解析。执行 `brew install tree-sitter` 后自动启用 JNI。
2. **Python SQL 注入检测器实现了简单 taint tracking**：先扫描所有 concat/f-string 赋值标记变量为 tainted，再检查 execute() 的变量引用。非完整数据流分析。
3. **Go 命令注入 regex 要求 `exec.` 前缀**：`runCommand(` 等非标准调用形式不匹配。

## 升级点（需 Owner 决策）

| # | 升级点 | 状态 |
|---|--------|------|
| 1 | tree-sitter JNI 激活需要 `brew install tree-sitter` + 配置 grammar 路径 | 💡 可选优化 |
| 2 | Go/Python 规则库当前各 2 条，Sprint 4 可扩展完整规则集 | 💡 Sprint 4 规划 |
| 3 | Python taint tracking 仅单文件范围，跨文件需完整数据流分析 | 💡 M3 规划 |
