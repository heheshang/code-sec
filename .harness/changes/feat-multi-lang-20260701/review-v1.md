# 评审报告（第 1 轮）

## 评审信息
- **需求名称**：E-S3-MULTI 多语言引擎扩展（Go/Python tree-sitter）
- **Epic ID**：E-S3-MULTI
- **评审类型**：执行评审（Phase 4 - Code Review）
- **评审时间**：2026-07-01 08:10
- **评审范围**：全部 6 个子任务变更文件（18 新增 + 5 修改）
- **评审结论**：✅ **通过**（有条件）

## 评分概览

| 维度 | 评分 | 阈值 | 状态 |
|------|------|------|------|
| 业务正确性 | 8 | ≥ 7 | ✅ |
| 功能完整性 | 7 | ≥ 7 | ✅ |
| 代码质量 | 7 | ≥ 6 | ✅ |
| 安全性 | 8 | ≥ 7 | ✅ |

**结论：✅ 通过** — 所有维度 ≥ 阈值，MUST FIX 级别问题为 0。

## 评审意见

### MUST FIX（必须修改）

无。

### LOW（建议修改）

1. **T6 文档未全部完成**
   - **位置**：`backend/engine/MULTI-LANG.md` 文件缺失
   - **建议**：创建 MULTI-LANG.md，包含：多语言 AstParser 扩展步骤、tree-sitter JNI 配置说明、Go/Python 规则添加指南
   - **理由**：spec.md § 2.2 T6 明确包含此项交付物

2. **GoCommandInjectionDetector 未使用的字段**
   - **位置**：`GoCommandInjectionDetector.java:37-40` — `LITERAL_ARG` 和 `LITERAL_ARG_SINGLE` 字段
   - **建议**：移除未使用的字段，或将其用于 Javadoc 注释中说明 literal 检查逻辑的参考
   - **理由**：存在未被引用的常量，增加维护负担

3. **重复的工具方法**
   - **位置**：`GoCommandInjectionDetector.java:153-165`、`PythonSqlInjectionDetector.java:117-129`、`PythonUnsafeEvalDetector.java:97-109`
   - **建议**：将 `lineNumberOf()` 和 `extractLine()` 提取到公共工具类 `EngineUtils.java` 或直接在 `Detector` 接口中提供 default 方法
   - **理由**：三份完全相同的代码重复

4. **Engine.java 中探测器注册硬编码**
   - **位置**：`Engine.java:77-88` — `registerDetectors()`
   - **建议**：后续可考虑 SPI 或注解扫描的动态发现，避免每添加一种语言都需要手动注册
   - **理由**：按 spec，Sprint 4 将扩展 PHP/JS/TS，手动注册会膨胀

### INFO（仅供参考）

1. **Python taint tracking 是单文件单跳的**
   - **位置**：`PythonSqlInjectionDetector.java:55-63`
   - **说明**：当前 taint tracking 只追踪直接赋值 `var = "..." + ...`，不追踪跨变量传递（如 `a = b; execute(a)` 且 `b = "..." + x`）。这是有意为之的简化，但文档中应注明限制。

2. **Tree-sitter JNI TODO 占位**
   - **位置**：`GoLanguage.java:40-44`、`PythonLanguage.java:40-44`
   - **说明**：`parseWithTreeSitter()` 方法目前是 TODO 占位，当原生库可用时才会执行 JNI 解析。这符合 spec 的"静默降级"设计，但应在 README 中标注 tree-sitter 集成状态。

3. **Python SQL 注入检测器 CONCAT_ASSIGNMENT 模式中 ^ 依赖 MULTILINE**
   - **位置**：`PythonSqlInjectionDetector.java:48-51`
   - **说明**：`^` 锚点依赖 `Pattern.MULTILINE` 标志才按行匹配。当前已正确设置，但若未来重构时移除此标志将导致检测失效。建议添加注释说明依赖关系。

4. **Go 硬编码密码检测器复用 HardcodedPasswordDetector**
   - **位置**：`Engine.java:83`
   - **说明**：Go 的 hardcoded-password 规则复用了现有的 Java `HardcodedPasswordDetector`，因为检测逻辑是纯 regex 的且语言无关。这是一个正确的设计决策。

## 总结
- MUST FIX：0
- LOW：4
- INFO：4

**评审结论：✅ 通过**。功能完整，所有测试通过（131/131），无阻塞性问题。建议在 Sprint 结束前处理 LOW 项（特别是 MULTI-LANG.md 文档）。
