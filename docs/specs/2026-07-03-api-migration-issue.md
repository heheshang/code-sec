---
title: "[Spec] GPU/vLLM → 大模型 API 模式迁移"
labels: ["spec", "migration", "p0"]
assignees: []
---

## 概述

将 code-sec 平台从自管 GPU/vLLM/CLI 模式统一切换为"大模型 API"模式。
基线设计文档已更新，本 Issue 跟踪实现执行。

**基线文档**
- `docs/designs/codex-integration-design.md`
- `docs/designs/sla-resource-planning.md`
- `docs/designs/ai-evaluation-framework.md`
- `docs/c4-architecture.md`

**详细 Spec**
→ `docs/specs/2026-07-03-api-migration-spec.md`

---

## 执行项

### P0 — Core (Week 1)

- [ ] P0.1 删除 CLI 模式代码（CodexCliClient, ClientType.CLI, CLI config）
- [ ] P0.2 实现 AiApiClient（限流/熔断/重试）
- [ ] P0.3 更新降级策略框架（ApiHealth, 4→3 级降级）
- [ ] P0.4 更新配置模型（API-only config）

### P1 — Deploy + CI/CD (Week 1–2)

- [ ] P1.1 创建 ai-gateway K8s 服务（1C 2G 无 GPU）
- [ ] P1.2 更新网络策略（ai-gateway 允许 HTTPS 出站）
- [ ] P1.3 CI/CD API Key 注入 + 移除 GPU 检测
- [ ] P1.4 移除 GPU 节点池

### P2 — Monitoring (Week 2–3)

- [ ] P2.1 添加 AI API 监控面板（费用/速率/熔断器）
- [ ] P2.2 移除 GPU 监控组件（DCGM/nvidia）
- [ ] P2.3 更新告警规则（GPU→API 可用性+费用）

### P3 — Evaluation + Testing (Week 3–4)

- [ ] P3.1 更新评估 CI 模板（--api-key / --model）
- [ ] P3.2 模型版本双锚定（API 版本 + Prompt 版本）
- [ ] P3.3 集成测试：API 故障模式（6 场景）

### P4 — Backlog

- [ ] P4.1 运维 runbook
- [ ] P4.2 混沌工程
- [ ] P4.3 前端 API 状态面板
- [ ] P4.4 API Key 自动轮换
- [ ] P4.5 Provider 智能路由

---

## 如何参与

按 P0 → P1 → P2 → P3 顺序执行，不可跳级。
每个 PR 引用本 Issue 号。
