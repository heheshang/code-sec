# Spec: GPU/vLLM → 大模型 API 迁移

> 将 code-sec 平台从自管 GPU/vLLM/CLI 模式统一切换为"大模型 API"模式
> 对应设计文档变更集：codex-integration-design / sla-resource-planning / ai-evaluation-framework / c4-architecture

**基线文档（已更新）**
- `docs/designs/codex-integration-design.md` — API-only 集成设计
- `docs/designs/sla-resource-planning.md` — AI API 成本+部署规划
- `docs/designs/ai-evaluation-framework.md` — API 版本锚定评估框架
- `docs/c4-architecture.md` — AI API 网关架构

---

## 优先级总览

| 等级 | 时间 | 性质 |
|------|------|------|
| **P0** | Week 1 | 核心代码变更 — 后端 CLI 删除 + API 客户端 + 配置 |
| **P1** | Week 1–2 | 部署 + CI/CD 适配 |
| **P2** | Week 2–3 | 监控 + 告警迁移 |
| **P3** | Week 3–4 | 评估框架更新 + 集成测试 |
| **P4** | 后续 | 运维文档 + 混沌工程 + UI 适配 |

---

## P0 — 核心代码变更（Week 1）

### P0.1 删除 CLI 模式代码

**目标**：移除 CodexCliClient、CLI 配置类、ClientType.CLI 枚举值

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `backend/codex-integration/src/main/java/.../CodexCliClient.java` | 删除整个文件 |
| `backend/codex-integration/src/main/java/.../ClientType.java` | 删除 `CLI` 枚举值 |
| `backend/codex-integration/src/main/java/.../CodexConfig.java` | 删除 `cliMode` 字段 |
| `backend/codex-integration/src/main/java/.../CodexClientFactory.java` | 删除 `CLI` → `CodexCliClient` 分支 |
| `backend/codex-integration/src/main/java/.../CodexHealth.java` | 删除 |

**验收标准**
- [ ] 编译通过，零残留 CLI 引用
- [ ] 全局搜索 `CodexCliClient` / `ClientType.CLI` / `cliMode` 结果为 0
- [ ] 单元测试 CLI 相关用例删除或适配

### P0.2 实现 API 客户端（含限流/熔断/重试）

**目标**：新的 `AiApiClient` 替代 `CodexCliClient`，内置生产级 API 调用保障

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `backend/codex-integration/src/main/java/.../AiApiClient.java` | **新建** — 统一 AI API HTTP 客户端 |
| `backend/codex-integration/src/main/java/.../RateLimiter.java` | **新建** — 令牌桶限流 |
| `backend/codex-integration/src/main/java/.../CircuitBreaker.java` | **新建** — 熔断器（半开/全开/关闭） |
| `backend/codex-integration/src/main/java/.../RetryHandler.java` | **新建** — 指数退避重试 |

**`AiApiClient` 接口契约**
```java
public interface AiApiClient {
    CompletableFuture<AiResponse> call(AiRequest request);
    AiHealth health();                  // 健康检查
    ApiMetrics metrics();               // 延迟/成功/限流统计
}
```

**验收标准**
- [ ] `AiApiClient` 支持 API Key 认证（Header 注入）
- [ ] 限流器：超过阈值返回 `429 TooManyRequestsException`
- [ ] 熔断器：连续 5 次失败 → 半开 → 全开 → 冷却后恢复
- [ ] 重试：5xx / 429 自动重试，最多 3 次，指数退避
- [ ] 单元测试覆盖：正常请求、限流、熔断、重试耗尽

### P0.3 更新降级策略框架

**目标**：从 `CodexHealth` / `LlmHealth` 调整为 `ApiHealth`，降级等级从 4 级（NONE/CLI/API_FALLBACK/FULL_FALLBACK）简化为 3 级（NONE/API_FALLBACK/FULL_FALLBACK）

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `backend/codex-integration/src/main/java/.../ApiHealth.java` | **新建** — API 健康检查 |
| `backend/codex-integration/src/main/java/.../DegradationManager.java` | 适配 3 级降级、API 健康判断 |
| `backend/codex-integration/src/main/java/.../DegradationLevel.java` | 删除 `CLI` 等级 |

**验收标准**
- [ ] 降级策略读取 `ApiHealth` 而非 `CodexHealth` / `LlmHealth`
- [ ] 3 级降级：`NONE`（正常） → `API_FALLBACK`（API 异常降级） → `FULL_FALLBACK`（完全降级）
- [ ] `API_FALLBACK` 触发条件：连续 3 次 API 调用超时/5xx
- [ ] `API_FALLBACK` 恢复条件：API 健康检查通过

### P0.4 更新配置模型

**目标**：配置从 CLI+API 双模式改为 API-only

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `backend/codex-integration/src/main/resources/application.yml` | 更新配置结构 |
| `backend/codex-integration/src/main/java/.../CodexConfig.java` | 重构为 API-only 配置 |

**新配置结构示例**
```yaml
codex:
  api:
    key: ${AI_API_KEY}
    model: gpt-4o
    rate-limit: 20          # RPM
    circuit-breaker:
      threshold: 5
      cooldown-ms: 30000
    retry:
      max-attempts: 3
      backoff-ms: 1000
    timeout-ms: 30000
```

**验收标准**
- [ ] 配置加载后 `ApiClient` 正确初始化
- [ ] 缺失 `API Key` 启动时 panic 并给出明确错误信息
- [ ] 配置热更新（如果现有框架支持）

---

## P1 — 部署 + CI/CD 适配（Week 1–2）

### P1.1 创建 ai-gateway 服务

**目标**：K8s 部署 `ai-gateway` 替代原有 GPU 推理服务（1C 2G，无 GPU）

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `infra/k8s/ai-gateway/deployment.yaml` | **新建** |
| `infra/k8s/ai-gateway/service.yaml` | **新建** |
| `infra/k8s/kustomization.yaml` | 添加 ai-gateway 资源 |

**验收标准**
- [ ] `ai-gateway` Pod 正常运行 `1C 2G` 无 GPU
- [ ] 就绪探针通过（`GET /health` 返回 200）
- [ ] 与现存 `codex-service` / `llm-service` Service 不冲突
- [ ] 旧的推理服务 `deployment.yaml` 删除或有清理计划

### P1.2 更新网络策略

**目标**：允许 ai-gateway 出站 HTTPS（443），保持其他服务内网

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `infra/k8s/network-policy.yaml` | 添加允许 ai-gateway 出站 443 的 Egress 规则 |

**验收标准**
- [ ] `ai-gateway` Pod 可以访问公网 AI API（openai.com / anthropic.com 等）
- [ ] 其他服务（后端 API、前端）不能出站（不变）

### P1.3 更新 CI/CD · API Key 注入

**目标**：CI 流水线注入 `AI_API_KEY`，移除 GPU 环境检测

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `.github/workflows/build.yml` | 添加 `AI_API_KEY` 密文变量、移除 GPU 检测步骤 |
| `.github/workflows/deploy.yml` | 添加 `AI_API_KEY` 到 K8s Secret、移除 GPU 节点池选择 |
| `infra/k8s/secret-template.yaml` | **新建** 或 更新 — 包含 `api-key` 字段 |

**验收标准**
- [ ] CI 构建不依赖 GPU 硬件
- [ ] 部署时自动创建/更新 `AI_API_KEY` Secret
- [ ] 回滚不泄露 API Key（Secret 随部署版本隔离）

### P1.4 移除 GPU 节点池

**目标**：下线 GPU 节点池，释放 GPU 成本

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `infra/terraform/gpu-node-pool.tf` | 删除或 `count = 0` |
| `infra/k8s/gpu-daemonset.yaml` | 删除（DCGM / nvidia-device-plugin） |
| `infra/ansible/gpu-drivers.yml` | 删除或标记 deprecated |

**验收标准**
- [ ] 集群无 GPU 节点
- [ ] GPU 驱动 DaemonSet 已卸载
- [ ] Terraform `terraform plan` 显示 GPU 资源数 0

---

## P2 — 监控 + 告警迁移（Week 2–3）

### P2.1 添加 AI API 监控面板

**目标**：Grafana 面板替换 GPU 利用率为 API 费用 + 速率限制

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `monitoring/grafana/dashboards/ai-api-costs.json` | **新建** — API 费用面板 |
| `monitoring/grafana/dashboards/api-rate-limits.json` | **新建** — 速率限制面板 |
| `monitoring/prometheus/rules/api-alerts.yml` | **新建** — API 告警规则 |

**面板指标**
```
ai_api_requests_total{provider,model,status}    → 请求量
ai_api_latency_seconds{provider,model}          → P50/P95/P99 延迟
ai_api_cost_total{provider,model}               → 累计费用
ai_api_rate_limit_remaining{provider}           → 剩余配额
ai_circuit_breaker_state{provider}              → 熔断器状态
```

**验收标准**
- [ ] 面板可看到每 Provider + 每 Model 的请求/延迟/费用
- [ ] 熔断器状态有可视指示器（绿/黄/红）
- [ ] 月 API 费用预估与实际偏差 < 20%

### P2.2 移除 GPU 监控组件

**目标**：下架 GPU 相关监控

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `monitoring/grafana/dashboards/gpu-utilization.json` | 删除 |
| `monitoring/prometheus/rules/gpu-alerts.yml` | 删除 |
| `infra/k8s/dcgm-exporter.yaml` | 删除 |

**验收标准**
- [ ] 集群无 DCGM Exporter 运行
- [ ] GPU 告警规则已移除（无残留）
- [ ] Prometheus target 无 `dcgm` 或 `gpu-metrics` job

### P2.3 更新告警规则

**目标**：GPU 告警 → API 可用性 + 费用告警

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `monitoring/prometheus/rules/api-alerts.yml` | 写入以下告警规则 |

**告警规则**
| 告警名 | 条件 | 严重度 |
|--------|------|--------|
| `ApiHighErrorRate` | 5min 内错误率 > 10% | critical |
| `ApiHighLatency` | P99 > 10s 持续 5min | warning |
| `ApiCircuitBreakerOpen` | 熔断器全开 > 1min | critical |
| `ApiMonthlyCostAnomaly` | 日费用 > 日均的 2x | warning |
| `ApiRateLimitExhausted` | 剩余配额 < 10% | warning |

**验收标准**
- [ ] 以上 5 条告警规则已部署并生效
- [ ] 手动模拟触发验证告警可达（通过 PagerDuty/钉钉/企业微信）

---

## P3 — 评估框架 + 测试（Week 3–4）

### P3.1 更新评估 CI 模板

**目标**：CI 模板增加 `--api-key` / `--model` 参数

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `evaluation/ci/template.yml` （或等效路径） | 添加 API 参数支持 |

**验收标准**
- [ ] 评估 CI 可通过参数指定 model 版本（如 `gpt-4o-2026-05-01`）
- [ ] 评估报告包含 `api_model` / `api_provider` / `prompt_version` 字段

### P3.2 模型版本管理：API+Prmopt 双锚定

**目标**：`ai-evaluation-framework.md` 设计的"API 版本+Prmopt 版本"双锚定落地

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `evaluation/models/version-registry.json` | **新建** — 登记 API 版本→模型映射 |
| `evaluation/prompts/prompt-manifest.json` | 确保每次 prompt 变更更新版本号 |

**`version-registry.json` 结构**
```json
{
  "v1": {
    "api_model": "gpt-4o-2026-05-01",
    "provider": "openai",
    "prompt_version": "2026-07-01",
    "activated_at": "2026-07-01T00:00:00Z"
  }
}
```

**验收标准**
- [ ] 评估结果可追溯到具体 API 版本 + prompt 版本
- [ ] 切换模型版本后历史报告仍可区分

### P3.3 集成测试：API 故障模式

**目标**：覆盖 API 限流、超时、认证失败、Provider 切换场景

**涉及文件**：

| 文件 | 操作 |
|------|------|
| `backend/codex-integration/src/test/java/.../AiApiClientIntegrationTest.java` | **新建** — 集成测试 |

**测试场景**
1. 正常 API 调用 → 200 响应
2. API Key 无效 → 401 → 熔断器不触发（认证失败非系统故障）
3. API 限流 429 → 重试 3 次 → 最终失败 → 触发降级
4. API 5xx → 重试 3 次 → 熔断器打开 → 降级到 `API_FALLBACK`
5. API 超时 30s → 同 4
6. Provider A 熔断中 → 自动切换到 Provider B（多提供商配置时）

**验收标准**
- [ ] 全部 6 个场景测试通过
- [ ] 测试可独立运行（Mock API Server 模拟 Provider）

---

## P4 — 后续（Backlog）

| ID | 任务 | 优先级 |
|----|------|--------|
| P4.1 | 运维 runbook：API 模式运维操作指南（密钥轮换/Provider 切换/故障排查） | low |
| P4.2 | 混沌工程：随机 Provider 故障验证降级链路 | low |
| P4.3 | 前端 UI 适配：API 状态面板（当前 Provider/模型/配额） | low |
| P4.4 | API Key 自动轮换：定期轮换 Key 减少泄露风险 | low |
| P4.5 | 成本优化：Provider 间智能路由（按价格/延迟动态选择） | stretch |

---

## 风险评估

| 风险 | 影响 | 缓解 |
|------|------|------|
| API Key 泄露到外部 | 安全 | Secret 管理（K8s Secret + CI Secret）+ 定期轮换（P4.4） |
| Provider 服务中断 | 可用性 | 多 Provider 冗余（已设计）+ 熔断自动切换 |
| API 费用超出预期 | 成本 | P2 费用面板 + 费用告警 + 限流上限 |
| 现网 CLI 模式仍有用户 | 兼容 | 确认当前无 CLI 用户后再部署（需与用户确认） |

---

## 执行建议

1. **顺序**：严格按照 P0 → P1 → P2 → P3 执行，不可跳级
2. **验证**：每个 P0 子项完成后运行 `mvn test` 确保编译+单元测试通过
3. **回滚**：P1.4（删除 GPU 节点池）前确认监控面板已覆盖 API 模式，避免监控真空期
4. **评审**：P0.2（AiApiClient 设计）建议代码评审后再实现细节
