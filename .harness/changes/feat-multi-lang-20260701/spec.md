# 需求规格说明书 — E-S3-MULTI 多语言引擎扩展

> **Epic ID**: E-S3-MULTI
> **Epic 名称**: 多语言引擎基础（Go/Python tree-sitter 适配）
> **变更目录**: `.harness/changes/feat-multi-lang-20260701/`
> **创建日期**: 2026-07-01
> **对应 Sprint**: Sprint 3（M1 第三冲刺）
> **总工时**: 100h
> **优先级**: P2
> **关联文档**:
> - 冲刺合同：`.harness/changes/sprint-3/contract.md` § 5.1
> - 引擎架构：`engine/src/main/java/com/codesec/engine/`
> - AstParser 接口：`engine/src/main/java/com/codesec/engine/parser/AstParser.java`
> - 现有 Java 实现：`engine/src/main/java/com/codesec/engine/parser/languages/JavaLanguage.java`

---

## 1. 背景

当前 SAST 引擎仅支持 Java（通过 JavaParser 实现 AstParser 接口）。项目需要覆盖 Go 和 Python 两种主流语言的基础安全检测能力。

**多语言策略**：
- 当前引擎通过 `AstParser` 接口解耦语言实现（见 `engine/README.md`）
- Java 使用 JavaParser（纯 Java），Go/Python 使用 tree-sitter
- tree-sitter 支持 100+ 语法，C 核心实现性能极高
- 引擎整体架构（Engine.java → RuleRegistry → Detector → AstParser）已支持多语言路由

---

## 2. 需求描述

### 2.1 范围与边界

**范围内**：
- tree-sitter 运行时集成（JNI 方式）
- Go AST 适配器（实现 AstParser 接口）
- Python AST 适配器（实现 AstParser 接口）
- Go 基础检测规则（硬编码密码）
- Python 基础检测规则（SQL 注入）
- YAML 规则文件 + 测试

**范围外**：
- ❌ 多语言 PHP/JS/TS 适配 — M2
- ❌ tree-sitter 独立进程模式（当前用 JNI）— M2
- ❌ 完整的 Go/Python 检测规则库 — Sprint 4
- ❌ 跨语言数据流分析 — M3

### 2.2 交付物清单

| # | 交付物 | 工时 |
|---|--------|------|
| T1 | tree-sitter 运行时集成（JNI） | 20h |
| T2 | Go AST 适配器 | 20h |
| T3 | Python AST 适配器 | 20h |
| T4 | Go/Python 检测规则（各 2 条） | 20h |
| T5 | 多语言集成测试 | 12h |
| T6 | 文档 | 8h |

---

## 3. 技术方案

### 3.1 tree-sitter 集成路径

```
方案 A (JNI):     Java ↔ JNI ↔ libtree-sitter.so/.dylib ↔ grammar.so
方案 B (进程):    Java ↔ Process ↔ tree-sitter CLI (stdin/stdout JSON)
方案 C (gRPC):    Java ↔ gRPC ↔ tree-sitter sidecar
```

**选择方案 A（JNI）**：
- 性能最佳（无序列化/进程开销）
- 部署需包含 `.so/.dylib` 文件
- 使用 `org.bonede:tree-sitter`（JDK 8+ 兼容，纯 Java JNI 封装）
- macOS 开发用 brew 安装 tree-sitter，Docker 部署包含 .so

### 3.2 AstParser 扩展

```java
// engine/src/main/java/com/codesec/engine/parser/languages/
GoLanguage.java        — TreeSitterGoParser implements AstParser
PythonLanguage.java    — TreeSitterPythonParser implements AstParser
TreeSitterAdapter.java — tree-sitter JNI 封装公共基类
```

### 3.3 规则

| 规则 ID | 名称 | 语言 | 严重度 | 类型 |
|---------|------|------|--------|------|
| `go/hardcoded-password-001` | Hardcoded Password | Go | high | regex |
| `go/command-injection-001` | Command Injection | Go | high | AST |
| `python/sql-injection-001` | SQL Injection | Python | high | AST |
| `python/unsafe-eval-001` | Unsafe eval() Usage | Python | medium | AST |

### 3.4 Engine 路由

`Engine.java` 中的 `registerDetectors()` 增加：

```java
// Go detectors
detectorsByRuleId.put("go/hardcoded-password-001", new RegexDetector());
detectorsByRuleId.put("go/command-injection-001", new GoCommandInjectionDetector());

// Python detectors
detectorsByRuleId.put("python/sql-injection-001", new PythonSqlInjectionDetector());
detectorsByRuleId.put("python/unsafe-eval-001", new PythonUnsafeEvalDetector());
```

---

## 4. 验收标准

- [ ] `AstParser` 在检测到 `.go` 文件时路由到 GoLanguage
- [ ] `AstParser` 在检测到 `.py` 文件时路由到 PythonLanguage
- [ ] GoLanguage 可解析含语法错误的 Go 源码（容错）
- [ ] PythonLanguage 可解析含语法错误的 Python 源码（容错）
- [ ] `go/hardcoded-password-001` 检测到 Go 源码中硬编码密码
- [ ] `python/sql-injection-001` 检测到 Python 源码中字符串拼接 SQL
- [ ] Java 原有 4 条规则和 123 个测试零回归
- [ ] `mvn -pl engine test` 全部通过
