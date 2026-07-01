# 任务拆解 — E-S3-MULTI 多语言引擎扩展

> 基于 `spec.md`（E-S3-MULTI）拆解的可执行任务清单。
> **总工时**: 100h

---

## 任务列表

### 任务 1：tree-sitter 运行时集成（JNI）

- **优先级**: P2
- **估计工时**: 20h
- **涉及模块**: `engine/`
- **涉及文件**:
  - `engine/pom.xml` — 添加 org.bonede:tree-sitter 依赖
  - `engine/src/main/java/com/codesec/engine/parser/languages/TreeSitterAdapter.java` — JNI 封装基类（新增）
  - `engine/src/main/java/com/codesec/engine/parser/languages/TreeSitterLibraryLoader.java` — 本地库加载（新增）
  - `engine/README.md` — 更新构建说明（含原生库安装）
- **验收标准**:
  - [ ] macOS `brew install tree-sitter` 后 Java 可加载 libtree-sitter.dylib
  - [ ] `TreeSitterAdapter` 可解析 Go/Python 源码为 CST
  - [ ] `mvn -pl engine compile` 通过
  - [ ] CI 兼容：原生库不可用时静默降级（不阻塞其他模块测试）
- **依赖任务**: 无

---

### 任务 2：Go AST 适配器

- **优先级**: P2
- **估计工时**: 20h
- **涉及模块**: `engine/`
- **涉及文件**:
  - `engine/src/main/java/com/codesec/engine/parser/languages/GoLanguage.java` — AstParser 实现（新增）
  - `engine/src/main/resources/rules/go/` — Go 规则目录（新增）
  - `engine/src/main/resources/rules/go/hardcoded-password-001.yaml`（新增）
  - `engine/src/main/resources/rules/go/command-injection-001.yaml`（新增）
  - `engine/src/main/java/com/codesec/engine/detector/impl/GoCommandInjectionDetector.java`（新增）
  - `engine/src/main/java/com/codesec/engine/Engine.java` — 注册 Go detector
- **验收标准**:
  - [ ] GoLanguage 正确识别 `.go` 文件
  - [ ] 可解析 Go 接口/结构体/函数定义
  - [ ] go/hardcoded-password-001 检测到硬编码密码
  - [ ] go/command-injection-001 检测到 exec.Command 含变量
  - [ ] 容错：含语法错误的 .go 文件不抛异常，返回空结果
- **依赖任务**: T1

---

### 任务 3：Python AST 适配器

- **优先级**: P2
- **估计工时**: 20h
- **涉及模块**: `engine/`
- **涉及文件**:
  - `engine/src/main/java/com/codesec/engine/parser/languages/PythonLanguage.java` — AstParser 实现（新增）
  - `engine/src/main/resources/rules/python/` — Python 规则目录（新增）
  - `engine/src/main/resources/rules/python/sql-injection-001.yaml`（新增）
  - `engine/src/main/resources/rules/python/unsafe-eval-001.yaml`（新增）
  - `engine/src/main/java/com/codesec/engine/detector/impl/PythonSqlInjectionDetector.java`（新增）
  - `engine/src/main/java/com/codesec/engine/detector/impl/PythonUnsafeEvalDetector.java`（新增）
  - `engine/src/main/java/com/codesec/engine/Engine.java` — 注册 Python detector
- **验收标准**:
  - [ ] PythonLanguage 正确识别 `.py` 文件
  - [ ] 可解析 Python 函数/类/decorator 定义
  - [ ] python/sql-injection-001 检测到字符串拼接 SQL
  - [ ] python/unsafe-eval-001 检测到 eval(user_input)
  - [ ] 容错：含语法错误的 .py 文件不抛异常
- **依赖任务**: T1

---

### 任务 4：Go/Python 示例 + E2E 测试

- **优先级**: P2
- **估计工时**: 20h
- **涉及模块**: `engine/`
- **涉及文件**:
  - `engine/examples/sample-code-go/` — Go 示例代码（含正例/负例）
  - `engine/examples/sample-code-python/` — Python 示例代码
  - `engine/src/test/java/com/codesec/engine/detector/impl/GoDetectorTest.java` — Go 检测测试（新增）
  - `engine/src/test/java/com/codesec/engine/detector/impl/PythonDetectorTest.java` — Python 检测测试（新增）
  - `engine/examples/output/expected-findings-go.json` — 预期输出
  - `engine/examples/output/expected-findings-python.json` — 预期输出
- **验收标准**:
  - [ ] Go 示例检测匹配预期输出
  - [ ] Python 示例检测匹配预期输出
  - [ ] E2E 测试覆盖 4 条新增规则
  - [ ] `mvn -pl engine test` 全部通过（含 Java 原有 123 测试）
- **依赖任务**: T2, T3

---

### 任务 5：多语言集成文档

- **优先级**: P2
- **估计工时**: 8h
- **涉及文件**:
  - `engine/README.md` — 更新多语言支持章节
  - `engine/MULTI-LANG.md` — 多语言扩展指南（新增）
- **验收标准**:
  - [ ] README 包含 Go/Python 支持说明和构建步骤
  - [ ] MULTI-LANG.md 包含添加新语言的完整步骤
- **依赖任务**: T1, T2, T3

---

## 排期

| 任务 | 优先级 | 工时 | 依赖 | 计划开始 |
|------|--------|------|------|----------|
| T1: tree-sitter JNI 集成 | P2 | 20h | - | D6 |
| T2: Go AST 适配器 | P2 | 20h | T1 | D7 |
| T3: Python AST 适配器 | P2 | 20h | T1 | D7 |
| T4: 示例 + E2E 测试 | P2 | 20h | T2,T3 | D8 |
| T5: 文档 | P2 | 8h | T1,T2,T3 | D9 |
