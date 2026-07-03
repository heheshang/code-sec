# CodeX AI 底座集成设计文档

> **版本**: v1.0  
> **状态**: Draft  
> **关联**: PRD § 3.5 AI 核心审计能力, SAD § 4.3 CodeX 架构  
> **前置依赖**: backend/engine (CPG + ExploitabilityJudger)

---

## 1. 设计目标

将大模型 API（代码专用模型 + 通用语义模型）作为 AI 底座，提供代码理解、漏洞分析、POC 生成、补丁生成四大能力。

**核心原则**：
- CodeX 负责代码任务（理解/生成/校验），通用 LLM 负责语义任务（归因/报告）
- 统一抽象层接口，仅支持 API 模式（远程大模型 API 调用）
- 三种降级等级，保障生产稳定性

---

## 2. 模块架构

```
backend/codex-integration/                    # 新增 Maven 模块
├── pom.xml
└── src/main/java/com/codesec/codex/
    ├── CodexAdapter.java                     # 统一入口接口
    ├── config/
    │   ├── CodexProperties.java              # 配置属性
    │   └── CodexAutoConfiguration.java       # Spring Boot 自动装配
    ├── model/
    │   ├── CodexRequest.java                 # 请求参数
    │   ├── CodexResponse.java                # 统一响应
    │   ├── CodexCapability.java              # 能力枚举
    │   └── CodexVerdict.java                 # 审计结论
    ├── client/                               # 大模型 API 调用层
    │   ├── CodexClient.java                  # 统一抽象接口
    │   ├── CodexApiClient.java               # 远程 API 调用
    │   └── CodexClientFactory.java           # 客户端工厂
    ├── capability/                           # 四大能力模组
    │   ├── VulnAnalysisCapability.java       # 漏洞分析
    │   ├── FalsePositiveFilterCapability.java # 误报过滤
    │   ├── PocGenerationCapability.java      # POC 生成
    │   └── PatchGenerationCapability.java    # 补丁生成
    ├── prompt/                               # Prompt 管理
    │   ├── PromptTemplate.java               # 模板模型
    │   ├── PromptLoader.java                 # YAML 模板加载
    │   └── PromptRepository.java             # 模板仓库
└── pipeline/
    ├── AnalysisPipeline.java             # 漏洞分析编排
    ├── CodexAdapterImpl.java             # 统一入口实现
    └── FallbackStrategy.java             # 降级策略
```

---

## 3. 核心接口设计

### 3.1 CodexAdapter — 统一入口

```java
package com.codesec.codex;

/**
 * CodeX AI 底座统一适配器。
 * 所有代码安全 AI 能力均通过此接口调用。
 */
public interface CodexAdapter {

    /**
     * 分析单个漏洞片段的可利用性和类型。
     */
    CodexResponse<VulnAnalysisResult> analyzeVuln(CodexRequest request);

    /**
     * 批量过滤 SAST 原始告警，区分真报/误报。
     */
    CodexResponse<List<FalsePositiveVerdict>> batchFilter(CodexRequest request);

    /**
     * 生成标准化 POC 脚本。
     */
    CodexResponse<PocResult> generatePoc(CodexRequest request);

    /**
     * 生成可编译的修复补丁。
     */
    CodexResponse<PatchResult> generatePatch(CodexRequest request);

    /**
     * 健康检查。
     */
    CodexHealth health();
}
```

### 3.2 CodexClient — 调用抽象

```java
package com.codesec.codex.client;

/**
 * 大模型 API 调用抽象层。
 * 仅支持 API 模式（远程大模型 API 调用），如 OpenAI/Claude 兼容 API。
 */
public interface CodexClient {

    /**
     * 发送 prompt 到 CodeX 模型，返回原始响应文本。
     */
    String execute(CodexContext context, String systemPrompt, String userPrompt);

    /** 检查模型是否可用。 */
    boolean isAvailable();

    /** 健康状态。 */
    CodexHealth health();

    /** 客户端类型。 */
    ClientType type();
}

/** 客户端类型 */
enum ClientType {
    API     // 远程 API 调用（OpenAI / Claude 兼容接口）
}
```

### 3.3 请求/响应模型

```java
/** CodeX 请求体（统一结构） */
public class CodexRequest {
    private String scanId;              // 扫描任务 ID
    private String vulnId;              // 漏洞 ID（可选）
    private String language;            // 编程语言
    private String codeSnippet;         // 代码片段
    private String filePath;            // 文件路径
    private int lineStart;              // 起始行
    private int lineEnd;                // 结束行
    private CallChain callChain;        // 函数调用链（CPG 输出）
    private String dataSource;          // 数据流来源
    private boolean reachable;          // 是否可达
    private Duration timeout;           // 推理超时时间
    private Map<String, String> extra;  // 扩展字段
}

/** 统一响应体 */
public class CodexResponse<T> {
    private boolean success;
    private T data;
    private String errorMessage;
    private Duration duration;
    private String modelVersion;
    private FallbackLevel fallbackLevel;  // 实际使用的降级等级
}

/** 降级等级 */
enum FallbackLevel {
    NONE,               // 未降级，大模型 API 正常
    API_FALLBACK,       // 代码模型 API 异常（超时/429/鉴权失败），降级为纯 LLM API
    FULL_FALLBACK       // 大模型 API 均异常，降级为原生 SAST
}
```

---

## 4. Prompt 模板管理

### 4.1 目录结构

```
backend/codex-integration/src/main/resources/prompts/
├── vuln-analysis.yaml              # 漏洞分析
├── false-positive-filter.yaml      # 误报过滤
├── poc-generation.yaml             # POC 生成
├── patch-generation.yaml           # 补丁生成
└── templates/
    ├── java/                       # Java 语言特定模板
    ├── go/                         # Go 语言特定模板
    └── python/                     # Python 语言特定模板
```

### 4.2 模板示例

```yaml
# prompts/vuln-analysis.yaml
capability: vuln_analysis
version: 1.0
model: codex
system_prompt: |
  你是一个代码安全审计专家。请分析以下代码片段是否存在安全漏洞。

  输出必须为以下 JSON 格式：
  {
    "vulnerable": true/false,
    "vuln_type": "sql_injection|xss|command_injection|path_traversal|ssrf|xxe|insecure_deserialization|other",
    "cwe": "CWE-89",
    "confidence": 0.0-1.0,
    "reason": "判断依据（引用具体代码行）",
    "source": "污染源（变量名 + 位置）",
    "sink": "触发点（函数调用 + 位置）",
    "exploit_condition": "利用条件描述"
  }

  注意事项：
  1. 仅基于给定的代码片段和 CPG 上下文判断
  2. 区分"理论存在漏洞"和"实际可利用"
  3. 确认框架是否自带防护（Spring Security、ESAPI 等）
  4. 污染源需精确定位到变量和参数

user_prompt_template: |
  语言: {language}
  文件: {file_path}
  行号: {line_start}-{line_end}

  代码片段:
  ```{language}
  {code_snippet}
  ```

  代码属性图上下文:
  - 函数调用链: {call_chain}
  - 数据流来源: {data_source}
  - HTTP 入口可达: {reachable}
  - 框架保护: {framework_protection}

timeout_seconds: 30
max_retries: 2
```

---

## 5. 调用流程

### 5.1 漏洞分析流水线

```
Engine 扫描完成 → CPG 构建完成
       │
       ▼
┌──────────────────────────────────────────────┐
│ AnalysisPipeline                              │
│                                               │
│  1. CodexAdapter.batchFilter()               │
│     → CodeX 批量甄别误报                       │
│     → 返回: {vuln_id, verdict, confidence}     │
│     → 误报标为 false_positive，打入白名单候选   │
│                                               │
│  2. CodexAdapter.analyzeVuln() (仅真报)      │
│     → CodeX 逐条漏洞深度分析                   │
│     → 返回: {vuln_type, cwe, exploit, reason} │
│     → 写入 Finding.exploitReason + AI 审计字段 │
│                                               │
│  3. (可选) CodexAdapter.generatePoc()         │
│     → CodeX 生成标准化 POC                    │
│     → 沙箱验证 → 有效则保留                   │
│                                               │
│  4. (可选) CodexAdapter.generatePatch()       │
│     → CodeX 生成修复补丁                      │
│     → 编译校验 → 逻辑校验 → 存入工单           │
└──────────────────────────────────────────────┘
       │
       ▼
写入 MySQL + ES → 推送人工审计工作台
```

### 5.2 降级策略

```java
public class FallbackStrategy {

    /** 按当前大模型 API 可用情况选择实际执行路径 */
    public AnalysisPath resolvePath(ApiHealth codeModelHealth, ApiHealth llmHealth) {
        if (codeModelHealth.isOk() && llmHealth.isOk()) {
            return AnalysisPath.DUAL_API;             // 等级 0: 双模型 API 协同
        }
        if (!codeModelHealth.isOk() && llmHealth.isOk()) {
            return AnalysisPath.LLM_ONLY;             // 等级 1: 纯 LLM API（代码 API 超时/429）
        }
        if (codeModelHealth.isOk() && !llmHealth.isOk()) {
            return AnalysisPath.CODE_PLUS_SAST;       // 等级 2: 代码模型 API + 传统 SAST
        }
        return AnalysisPath.SAST_ONLY;                // 等级 3: 纯 SAST 兜底
    }
}

enum AnalysisPath {
    DUAL_API,         // 代码模型 API + LLM API: 最高精度
    LLM_ONLY,         // 仅 LLM API: 保留核心审计，弱化 POC/补丁
    CODE_PLUS_SAST,   // 代码模型 API + SAST: 保留代码分析，缺少语义归因
    SAST_ONLY         // 仅 SAST: 基础扫描，不阻断流水线
}
```

---

## 6. 与现有代码的集成

| 现有组件 | 变更方式 |
|----------|----------|
| `Engine.scan(Path)` | 扫描完成后调用 `CodexAdapter.batchFilter()` 过滤结果 |
| `ExploitabilityJudger` | AI 结论与算法结论融合（置信度加权） |
| `Finding` record | 新增字段：`aiVerdict`（3 值方案）、`aiConfidence`、`aiExplanation` |
| `EngineAdapterImpl` | 扫描结果返回前经过 AI 处理流水线 |
| `backend/api` 漏洞相关 API | 返回 AI 审计结论字段 |

```java
// Finding 新增字段（需同步修改 Union Schema）
// aiVerdict 三值方案：exploitable / false_positive / suspicious
// 与前端交互设计对齐（suspicious 为中间态，需人工二审）
public record Finding(
    // ... 原有字段 ...
    String aiVerdict,           // AI 审计结论：exploitable / false_positive / suspicious
    Double aiConfidence,        // AI 置信度 0.0-1.0
    String aiExplanation,       // AI 判断依据
    String aiGeneratedPatch     // AI 生成的修复代码（可选）
) { }
```

---

## 7. 性能指标

| 操作 | 预期延迟 | 并发量 | 备注 |
|------|----------|--------|------|
| 单漏洞分析 | 8-20s | 20 并发 | 大模型 API，依赖网络延迟 + 模型响应 |
| 误报批量过滤（100 条） | 20-40s | 1 批 | 可合并为单次 prompt |
| POC 生成 | 15-30s | 10 并发 | 含代码生成 + 编译校验 |
| 补丁生成 | 10-25s | 10 并发 | 含代码生成 + 编译校验 |

---

## 8. 配置属性

```yaml
# application.yml 新增配置
codex:
  enabled: true
  # API 模式配置（大模型 API 网关）
  api:
    code-model:
      endpoint: https://api.openai.com/v1/chat/completions  # 或兼容 API
      apiKey: ${CODE_MODEL_API_KEY}
      model: gpt-4o                     # 代码专用模型
      maxTokens: 4096
      temperature: 0.1
      timeoutSeconds: 60
    llm-model:
      endpoint: https://api.openai.com/v1/chat/completions  # 或兼容 API
      apiKey: ${LLM_MODEL_API_KEY}
      model: gpt-4o                     # 通用语义模型（可与代码模型相同）
      maxTokens: 8192
      temperature: 0.3
      timeoutSeconds: 90
  # API 调用策略
  api-strategy:
    retry:
      maxAttempts: 3
      backoff: "exponential"           # 指数退避（应对 429）
      initialDelayMs: 1000
    circuitBreaker:
      failureThreshold: 5               # 连续 5 次失败断开
      resetTimeoutMs: 60000             # 60s 后尝试恢复
  # 能力开关
  capabilities:
    vuln-analysis: true
    false-positive-filter: true
    poc-generation: false      # V1.1 开启
    patch-generation: false    # V1.1 开启
  # 并发控制
  maxConcurrency: 20
  perRequestTimeoutSeconds: 30
```
