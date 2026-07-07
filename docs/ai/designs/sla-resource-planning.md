# SLA 定义与资源规划方案

> **版本**: v1.0  
> **状态**: Draft  
> **关联**: PRD § 4 非功能需求, SAD § 6 部署架构, docs/architecture.md § 10 部署架构  
> **前置依赖**: codex-integration-design.md (AI 推理资源), cpg-storage-design.md (Neo4j 资源)

---

## 1. 设计目标

为平台 7 大核心服务定义多级 SLA（服务等级协议），并基于业务场景给出精确的 CPU/存储资源估算。

**核心原则**：
- **混合部署**：平台服务可私有化部署，AI 推理通过大模型 API 网关调用（需公网或内网代理）
- **API 优先**：AI 能力通过远程 API 调用，无需自管 GPU/vLLM 基础设施
- **渐进扩展**：从单机 Docker Compose 到生产 K8s 集群提供不同资源模板
- **降级保底**：大模型 API 不可用时退化为传统 SAST，保障扫描流水线不阻断

---

## 2. SLA 定义

### 2.1 服务组件清单

| 编号 | 服务 | 关键依赖 | 关键指标 |
|------|------|----------|----------|
| S1 | 平台 API（Spring Boot） | PG, Redis, ES, MinIO | 可用性, P99 延迟 |
| S2 | Scan Worker | PG, Redis, 沙箱 | 吞吐量, 最大扫描时长 |
| S3 | AI API 网关 | 大模型 API 可用性, 网络 | API 延迟, API 成功率 |
| S4 | LLM API 网关 | 大模型 API 可用性, 网络 | API 延迟, API 成功率 |
| S5 | 搜索引擎（ES） | 磁盘 IO, 内存 | 查询 P99 延迟 |
| S6 | CPG 图存储（Neo4j） | 磁盘 IO, 内存 | 查询 P99 延迟 |
| S7 | 文件存储（MinIO） | 磁盘 | 上传/下载吞吐 |

### 2.2 SLA 等级

#### 2.2.1 定义

| 等级 | 可用性 | 适用组件 | 适用客户场景 |
|------|--------|----------|-------------|
| **Platinum** | 99.99%（年宕机 ≤52min） | S1 平台 API | 金融/关键基础设施 |
| **Gold** | 99.9%（年宕机 ≤8.7h） | S1-S7（默认） | 企业私有化部署 |
| **Silver** | 99.5%（年宕机 ≤43h） | S5-S7 存储组件 | 开发测试环境 |
| **Best Effort** | 无保证 | 沙箱 Pod | 扫描执行期间 |

#### 2.2.2 各组件 SLA 矩阵

```yaml
# 平台默认 SLA（Gold 等级）
components:
  api:
    availability: "99.9%"
    p99_latency: "500ms"          # 常规 API 响应
    p99_scan_trigger: "2s"       # 触发扫描 API
    measurement: "请求成功率（非 5xx）/ 滑动窗口 5min"
  
  scan_engine:
    availability: "99.9%"         # Worker 服务可用，非单个扫描
    max_duration:
      small: "5min"               # < 50KLOC
      medium: "10min"             # 50-500KLOC
      large: "20min"              # 500KLOC-2MLOC
      xlarge: "45min"             # > 2MLOC
    measurement: "扫描任务完成率 / 超时率"

  ai_inference:
    availability: "99.0%"         # 依赖大模型 API 可用性 + 网络
    vuln_analysis_p99: "20s"      # 单漏洞分析（含网络延迟）
    batch_filter_throughput: "60/min"  # 误报过滤吞吐（API 并发更高）
    poc_generation_p99: "30s"     # POC 生成
    patch_generation_p99: "20s"   # 补丁生成
    measurement: "API 请求成功率 / 超时率 / 429 率"
    degradation: "降级至 LLM API-only 或 SAST-only"
    api_vendor_sla: "依赖上游模型 API 提供商（OpenAI/Anthropic/DeepSeek 等）"

  search_es:
    availability: "99.9%"
    p99_simple_query: "200ms"     # 关键词搜索
    p99_complex_query: "1s"       # 聚合/过滤搜索
    measurement: "查询 P99 延迟 / 错误率"

  cpg_neo4j:
    availability: "99.9%"
    p99_reachability: "200ms"     # 可达性查询
    p99_taint_chain: "500ms"      # 污点传播链路
    measurement: "图查询 P99 延迟"

  git_integration:
    webhook_p99: "500ms"          # Webhook 收到 → 入队
    clone_p99: "30s"             # 增量克隆
    full_clone_p99: "5min"       # 首次全量克隆
```

### 2.3 SLI 采集方案

| SLI | 采集方式 | 指标源 |
|-----|----------|--------|
| API 可用性 | Prometheus `up` + 请求成功率 | Spring Boot Actuator `/actuator/prometheus` |
| API 延迟 | Micrometer 分桶直方图 | `http_server_requests_seconds_bucket` |
| 扫描完成率 | Worker 埋点 | `scan_completed_total / scan_started_total` |
| API 推理延迟 | CodexAdapter 埋点 | Micrometer Timer |
| API 错误率 | CodexAdapter 埋点 | `api_error_total{status=429/401/5xx}` |
| ES 查询延迟 | ES 自带 | `_cluster/stats` + Prometheus ES exporter |
| CPG 查询延迟 | Neo4j 埋点 | Micrometer + Neo4j metrics |
| 降级触发率 | FallbackStrategy 埋点 | `fallback_triggered_total` |

---

## 3. AI API 资源与成本估算

### 3.1 API 模型选型

| 模型角色 | 推荐 API | 模型版本 | 适用场景 |
|----------|---------|----------|----------|
| 代码模型 | OpenAI / Claude / DeepSeek | GPT-4o / Claude 3.5 Sonnet / DeepSeek-Coder-V2 | 漏洞分析、误报过滤、POC/补丁生成 |
| 语义模型 | OpenAI / Claude / DeepSeek | GPT-4o / Claude 3.5 Sonnet / DeepSeek-V2 | 漏洞归因、报告生成、RAG 问答 |

### 3.2 API 调用成本估算

| 能力 | 每次请求 Tokens | 并发量 | 扫描 50 项目/天 月成本 |
|------|----------------|--------|---------------------|
| 单漏洞分析（含上下文） | ~4K input + ~500 output | 20 并发 | ¥500-1,000 |
| 误报过滤（100 条一批） | ~8K input + ~200 output | 1 批 | ¥200-500 |
| POC 生成 | ~2K input + ~1K output | 10 并发 | ¥300-600 |
| 补丁生成 | ~3K input + ~1K output | 10 并发 | ¥300-600 |
| **合计** | | | **¥1,300-2,700/月** |

> **注**：以上为 API 调用费用估算，实际成本取决于所选模型定价（如 GPT-4o ≈ ¥20/1M input tokens）。API 价格持续下降趋势明显。

### 3.3 API 可用性 SLA

| 提供商 | 典型可用性 SLA | 备注 |
|--------|---------------|------|
| OpenAI | 99.9% (Platform) | 速率限制(RPM/TPM)需提前申请 |
| Anthropic | 99.9% (Claude API) | 支持 Batch API（50% 折扣） |
| DeepSeek | 99.5% | 经济实惠，偶有排队 |
| Azure OpenAI | 99.95% | 企业级 SLA，建议生产环境选用 |

---

## 4. CPU / 内存资源估算

### 4.1 微服务资源清单

```yaml
services:
  # ===================== 平台层 =====================
  frontend:
    replicas: 2
    cpu_request: "500m"
    cpu_limit: "1"
    memory_request: "512Mi"
    memory_limit: "1Gi"
    notes: "Nginx 静态托管，低资源消耗"
  
  api:
    replicas: 3                    # Gold 等级最小 3 副本
    cpu_request: "2"
    cpu_limit: "4"
    memory_request: "4Gi"
    memory_limit: "8Gi"
    notes: "Spring Boot 模块化单体，11 个领域模块"
    jvm_heap: "-Xms4g -Xmx6g"
  
  worker:
    replicas: 2
    cpu_request: "2"
    cpu_limit: "4"
    memory_request: "4Gi"
    memory_limit: "8Gi"
    notes: "扫描队列消费者，CPU 密集型"
    jvm_heap: "-Xms4g -Xmx6g"
  
  scheduler:
    replicas: 1
    cpu_request: "1"
    cpu_limit: "2"
    memory_request: "2Gi"
    memory_limit: "4Gi"
    notes: "定时任务 + 调度逻辑"
  
  # ===================== 引擎层 =====================
  sandbox_pool:
    max_concurrent: 4              # 每 worker 最多 4 个并行沙箱
    cpu_request: "2"               # 每沙箱
    memory_request: "2Gi"          # 每沙箱
    ephemeral_storage: "5Gi"       # 代码拉取 + 编译缓存
    notes: "每个扫描任务一个临时 Pod，用完即销毁"
  
  # ===================== AI API 网关层 =====================
  ai_gateway:
    replicas: 2
    cpu_request: "1"
    cpu_limit: "2"
    memory_request: "1Gi"
    memory_limit: "2Gi"
    notes: "大模型 API 调用网关：限流、重试、熔断、API Key 管理"
  
  # ===================== 数据层 =====================
  postgresql:
    replicas: 2                    # Patroni 主从
    cpu_request: "2"
    cpu_limit: "4"
    memory_request: "8Gi"
    memory_limit: "16Gi"
    storage: "200Gi"               # SSD，按需扩展
  
  redis:
    replicas: 3                    # Sentinel 模式
    cpu_request: "500m"
    cpu_limit: "1"
    memory_request: "2Gi"
    memory_limit: "4Gi"
    storage: "20Gi"                # AOF 持久化
  
  # elasticsearch:        deprecated — replaced by PG FTS
  #   replicas: 3
  #   cpu_request: "2"
  #   cpu_limit: "4"
  #   memory_request: "8Gi"
  #   memory_limit: "16Gi"
  #   storage: "500Gi"
  #   notes: "Removed — full-text search now runs on PostgreSQL tsvector/tsquery"
  
  neo4j:
    replicas: 2                    # 主从 / Causal Cluster
    cpu_request: "2"
    cpu_limit: "4"
    memory_request: "8Gi"
    memory_limit: "16Gi"
    storage: "200Gi"               # SSD，图数据
    heap: "-Xms6g -Xmx6g"
  
  minio:
    replicas: 4
    cpu_request: "1"
    cpu_limit: "2"
    memory_request: "4Gi"
    memory_limit: "8Gi"
    storage: "500Gi"               # 每节点，含纠删码
```

### 4.2 节点池规划

```yaml
node_pools:
  cpu_general:
    instance_type: "8C 32G"        # 例如 c7i.xlarge
    min_nodes: 3
    max_nodes: 10
    taints: []
    deployments:
      - frontend
      - api
      - worker
      - scheduler
      - postgresql
      - redis
      # - elasticsearch  deprecated — replaced by PG FTS
      - neo4j
      - minio
      - monitoring (prometheus/grafana/loki)
  
  gpu_inference: ~         # 不再需要 GPU 节点——AI 推理通过 API 远程调用
  
  storage_heavy:
    instance_type: "8C 64G + NVMe"
    min_nodes: 3
    max_nodes: 6
    taints:
      - key: "node-type"
        value: "storage"
        effect: "NoSchedule"
    deployments:
      # - elasticsearch   deprecated — replaced by PG FTS
      - minio
```

### 4.3 中型部署汇总（Gold SLA）

```
┌──────────────────────────────────────────────────────┐
│               K8s 集群 (8 CPU 节点)                    │
├──────────────────────────────────────────────────────┤
│                                                      │
│  Node Pool: cpu-general (3-10 节点 × 8C 32G)        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐  │
│  │ API ×3   │ │ Worker ×2│ │ Frontend │ │ PG 主从 │  │
│  │ 4C 8G    │ │ 4C 8G    │ │ ×2       │ │ 4C 16G  │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐  │
│  │ ES ×3    │ │ Neo4j ×2 │ │ Redis ×3 │ │ MinIO  │  │
│  │ 4C 16G   │ │ 4C 16G   │ │ 1C 4G    │ │ ×4     │  │
│  │ +500G    │ │ +200G    │ │ +20G     │ │ 2C 8G  │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────┘  │
│  ┌──────────┐                                        │
│  │ AI API   │                                        │
│  │ Gateway  │                                        │
│  │ ×2 1C 2G │                                        │
│  └──────────┘                                        │
│                                                      │
└──────────────────────────────────────────────────────┘
```

> **注：** 相比原方案，去掉了 GPU 节点池（2 节点 × A10 24GB），新增轻量级 AI API 网关（2 副本 × 1C 2G）。AI 推理依赖大模型 API 远程调用，不再需要 GPU 基础设施。

---

## 5. 存储资源估算

### 5.1 存储增长模型

| 存储类型 | 每 100KLOC 扫描 | 每项目每天 50 次扫描 | 月增长（50 项目） |
|----------|----------------|---------------------|-------------------|
| PostgreSQL | 5MB（结果数据） | 250MB | 7.5GB |
| ES 索引 | 20MB（含代码片段） | 1GB | 30GB |
| Neo4j CPG | 50MB | 2.5GB | 75GB |
| MinIO 制品 | 10MB（报告+日志） | 500MB | 15GB |
| 合计 | 85MB | 4.25GB | 127.5GB |

### 5.2 存储细分

#### PostgreSQL

| 表 | 行数估计 | 行大小 | 总大小 | 增长速率 |
|----|---------|--------|--------|---------|
| scan_task | 500K/月 | ~500B | 250MB | 低速 |
| finding | 5M/月 | ~1KB | 5GB | 高速 |
| ticket | 50K/月 | ~2KB | 100MB | 中速 |
| audit_log | 1M/月 | ~500B | 500MB | 高速 |
| user_repo | 5K | ~2KB | 10MB | 静态 |
| rule | 200 | ~5KB | 1MB | 静态 |

#### ~~Elasticsearch~~ (已迁移至 PostgreSQL FTS)

| 索引 | 文档数 | 每文档 | 总大小 | 保留策略 |
|------|--------|--------|--------|---------|
| vuln (PG FTS) | 5M/月 | ~2KB | —（继承 PG 资源）| 12 个月 |
| code-snippet | 5M/月 | ~1KB | 5GB/月 | 12 个月 |
| audit-trail | 1M/月 | ~500B | 500MB/月 | 6 个月 |

#### Neo4j

| 节点类型 | 节点数 | 关系数 | 存储（含索引）|
|----------|--------|--------|-------------|
| Method | 25,000/项目 | - | ~50MB/项目 |
| CALLS 关系 | - | 75,000/项目 | ~30MB/项目 |
| 其他节点/关系 | - | - | ~20MB/项目 |
| 合计/项目 | - | - | ~100MB/项目 |
| 50 项目保留 10 版 | - | - | ~50GB |

### 5.3 保留策略

```yaml
retention:
  postgresql:
    scan_task: "永久"              # 扫描历史不可丢
    finding: "永久"                # 漏洞证据不可丢
    ticket: "永久"
    audit_log: "12 个月"           # 合规要求
    temp_data: "7 天"             # 临时缓存

  # elasticsearch:  deprecated — replaced by PG FTS
  #   vuln_finding: "12 个月"
  #   code_snippet: "12 个月"
  #   audit_trail: "6 个月"

  neo4j:
    cpg_versions: "每个项目最近 10 次扫描"  # 或 30 天 TTL

  minio:
    scan_artifacts: "30 天"        # 原始扫描日志
    pdf_reports: "12 个月"
    temp_uploads: "24 小时"
```

---

## 6. 场景化容量规划

### 6.1 场景 A：小型团队（5 项目, 100KLOC, 50 扫描/天）

```yaml
节点:
  cpu: 3 节点 × 8C 32G
  gpu: 无（AI 推理通过 API 远程调用）
  storage: 1TB NVMe（全组件共享卷）

deployment:
  api: 2 副本
  worker: 2 副本
  ai_gateway: 2 副本（大模型 API 调用）
  其余组件各 1 副本

月存储增长: ~15GB
建议初始存储: 500GB
部署形态: Docker Compose / 轻量 K8s
每月成本估算: ¥1,000-2,000（不含大模型 API 调用费）
大模型 API 月调用费: ¥1,300-2,700（按用量计）
```

### 6.2 场景 B：中型团队（50 项目, 2MLOC, 200 扫描/天）

```yaml
节点:
  cpu: 6 节点 × 8C 32G
  gpu: 无（AI 推理通过 API 远程调用）
  storage: 2TB SSD（数据） + 500GB NVMe（日志）

deployment:
  api: 3 副本
  worker: 3 副本
  ai_gateway: 2 副本（大模型 API 调用 + 限流熔断）
  es: 3 节点
  neo4j: 2 节点（主从）
  minio: 4 节点（纠删码）

月存储增长: ~130GB
建议初始存储: 2TB
部署形态: K8s 生产集群
每月成本估算: ¥8,000-15,000（不含大模型 API 调用费）
大模型 API 月调用费: ¥3,000-6,000（按用量计）
```

### 6.3 场景 C：企业级（200 项目, 10MLOC, 1,000 扫描/天）

```yaml
节点:
  cpu: 15 节点 × 8C 32G
  gpu: 无（AI 推理通过多地 API 冗余）
  storage: 10TB SSD（数据） + 2TB NVMe（缓存）

deployment:
  api: 6 副本（HPA: CPU > 70% → scale）
  worker: 6 副本（HPA: 队列深度 > 20 → scale）
  ai_gateway: 3 副本（主备 + 多 API 提供商冗余）
  es: 6 节点（3 热 + 3 冷）
  neo4j: 3 节点（Causal Cluster）
  minio: 6 节点（纠删码 4+2）

  # 额外组件
  kafka: 3 节点（扫描事件总线）
  patroni: 3 节点（PG HA）
  redis_cluster: 6 节点

月存储增长: ~650GB
建议初始存储: 10TB
部署形态: 多 AZ K8s 生产集群
SLA 等级: Platinum
每月成本估算: ¥40,000-80,000（不含大模型 API 调用费）
大模型 API 月调用费: ¥10,000-20,000（可签预付费折扣）
```

---

## 7. 高可用设计

### 7.1 AI API 网关高可用

```yaml
ha_strategies:
  api_failover:
    primary: "多 API 提供商冗余（OpenAI + Azure + 自备）"
    detection: "API 健康检查（5s 间隔，连续失败阈值 3）"
    failover: "自动切换备选 API 提供商"
    data: "无状态——无需数据迁移"
  
  api_degradation:
    - condition: "代码模型 API 超时/429"
      action: "FallbackStrategy → LLM_ONLY 路径"
      impact: "失去 POC/补丁生成，保留分析能力"
    - condition: "双模型 API 均不可用"
      action: "FallbackStrategy → SAST_ONLY 路径"
      impact: "失去 AI 审计能力，基础 SAST 不受影响"

  rate_limit_management:
    strategy: "令牌桶 + 队列削峰"
    queue: "API 请求排队等待（可配置最大排队时间）"
    backpressure: "接近 Rate Limit 时自动降速"
    vendor: "关键 API 提供商签预付费合同提高 RPM 限制"
```

### 7.2 数据库高可用

| 组件 | HA 方案 | RPO | RTO |
|------|---------|-----|-----|
| PostgreSQL | Patroni + 异步流复制 | < 1min | < 30s |
| Redis | Redis Sentinel / Cluster | 秒级 | < 10s |
| PostgreSQL FTS | 继承 PG 高可用 | 继承 PG | 继承 PG |
| Neo4j | Causal Cluster（主写从读）| < 1s | < 30s |
| MinIO | 纠删码 4+2 或 8+4 | 0 | < 1min |

### 7.3 应用层高可用

```yaml
pod_anti_affinity:
  api: "preferred: 分散到不同节点"
  worker: "required: 每个节点最多 1 个"
  ai_gateway: "preferred: 分散到不同节点"
  es: "required: 每节点最多 1 个 ES Pod"

pdb_budgets:
  api: "minAvailable: 2"         # 3 副本最多 1 不可用
  worker: "minAvailable: 1"
  ai_gateway: "minAvailable: 1"    # 2 副本至少 1 可用
  es: "minAvailable: 2"

hpa_config:
  api:
    metric: "cpu > 70%"
    min: 3
    max: 10
    cooldown: "3min"
  worker:
    metric: "custom/queue_depth > 20"
    min: 2
    max: 10
    cooldown: "5min"
```

---

## 8. 网络策略

```yaml
network_policies:
  # 通用：禁止所有出口流量
  default_deny_egress:
    policyTypes: ["Egress"]
    egress: []
  
  # 平台服务可互相通信
  allow_platform_internal:
    podSelector: {}
    ingress:
      - from:
          - namespaceSelector:
              matchLabels:
                kubernetes.io/metadata.name: code-sec-platform
    egress:
      - to:
          - namespaceSelector: {}    # 集群内部全放通
      - to:
          - ipBlock:
              cidr: "10.0.0.0/8"    # 内网段
    
  # AI API 网关出站规则
  ai_gateway_egress:
    podSelector:
      matchLabels:
        app: "ai-gateway"
    egress:
      - to:                          # 大模型 API 出站
          - ipBlock:
              cidr: "0.0.0.0/0"     # 公网 API 调用（或配置内网代理）
        ports:
          - protocol: TCP
            port: 443
  
  # Git 出口：仅允许到内网 GitLab
  git_egress:
    podSelector:
      matchLabels:
        app: "sandbox"
    egress:
      - to:
          - ipBlock:
              cidr: "10.88.0.0/16"  # GitLab 所在网段
        ports:
          - protocol: TCP
            port: 22
          - protocol: TCP
            port: 443
```

---

## 9. 运维建议

### 9.1 AI API 运维清单

| 项目 | 工具 | 频率 |
|------|------|------|
| API 调用延迟/错误率 | Prometheus + Grafana | 实时 |
| API 可用性（多提供商） | 自定义探针 + 黑盒监控 | 1min |
| 速率限制（RPM/TPM）使用率 | CodexAdapter 埋点 | 实时 |
| API Key 轮换管理 | Vault / K8s Secret + 自动轮换 | 每月 |
| API 调用费用追踪 | 自定义仪表盘 + 预算告警 | 每日 |
| 多提供商切换演练 | 手动/自动切换脚本 | 每季 |

### 9.2 降级演练计划

| 演练场景 | 频率 | 验证目标 |
|----------|------|----------|
| 代码模型 API 超时 | 每月 | Fallback → LLM API-only 自动切换 < 10s |
| 双模型 API 均不可用 | 每季 | Fallback → SAST-only，扫描不中断 |
| API Key 轮换 | 每月 | 新旧 Key 平滑切换，零中断 |
| ES 集群下线 | 每季 | 搜索降级到 PG LIKE 查询 |
| PG 主库宕机 | 每月 | Patroni 自动切换 < 30s |
| 全集群断电 | 每年 | 恢复后所有数据一致性验证 |

### 9.3 容量预警阈值

| 指标 | 警告 | 报警 | 紧急 |
|------|------|------|------|
| API 调用错误率 | > 2% (5min) | > 5% (5min) | > 10% (5min) |
| API P99 延迟 | > 15s | > 25s | > 40s |
| 速率限制(RPM) 使用率 | > 70% | > 85% | > 95% |
| API 调用费用异常 | > 日预算 120% | > 日预算 150% | > 日预算 200% |
| 存储使用率 | > 60% | > 80% | > 90% |
| ES 查询 P99 | > 500ms | > 1s | > 3s |
| CPG Neo4j P99 | > 200ms | > 500ms | > 1s |
| 扫描队列深度 | > 10 | > 50 | > 100 |
| 降级触发率 | > 1% (1h) | > 5% (1h) | > 20% (1h) |

---

## 10. 与现有文档的关系

```yaml
alignment:
  - docs/architecture.md §10:
      current: "缺少 AI API 网关、neo4j 容器"
      update: "新增 AI API 网关容器（替代原有 GPU 推理服务）"
  
  - docs/c4-architecture.md Level 2 Container:
      current: "无 AI API 网关和 Neo4j 容器"
      update: "补充 ai-gateway / neo4j 容器定义"
  
  - docs/designs/codex-integration-design.md:
      api_design: "本节 §3 AI API 资源与成本估算对齐 CodeX API 设计"
  
  - docs/designs/cpg-storage-design.md:
      neo4j_deployment: "本节 §7 高可用 + §4.1 资源估算覆盖 Neo4j 资源需求"
  
  - project-plan.md Sprint 17-18:
      target_sla: "SLA 99.9% 作为商业化上线目标"
```

---

## 附录 A：最小部署配置（开发/演示环境）

```yaml
# 单机 Docker Compose 部署（无需 GPU）
hardware:
  cpu: "8 核"
  memory: "16GB"
  gpu: "无（大模型 API 远程调用）"
  storage: "500GB NVMe"

services:
  ai_gateway: "2 副本（API 调用 + 限流）"
  databases: "Docker 单实例"
  api/worker: "单副本"

capacity:
  scan_throughput: "~10 scans/day"
  ai_throughput: "~500 vuln analyses/day (受 API 速率限制)"
```

## 附录 B：大模型 API 配置指南（私有化环境）

| 配置项 | 说明 | 必填 |
|--------|------|------|
| `CODEX_API_ENDPOINT` | 大模型 API 地址（需可访问公网或内网代理） | 是 |
| `CODEX_API_KEY` | API 密钥（建议通过 Vault/Secret 管理） | 是 |
| `CODEX_MODEL_NAME` | 模型名称（如 `gpt-4o`、`claude-3-sonnet`、`deepseek-coder`） | 是 |
| `CODEX_RATE_LIMIT` | 每分钟请求限制（需与 API 提供商合同对齐） | 否 |
| `CODEX_PROXY_URL` | 内网代理地址（私有化环境无公网直连时） | 否 |
| `LLM_API_KEY` | 语义模型 API 密钥（可与代码模型不同） | 是 |
| `LLM_MODEL_NAME` | 语义模型名称（如 `gpt-4o`、`claude-3.5-sonnet`） | 是 |

> **私有化部署网络要求**：AI API 网关 Pod 需要有出站 HTTPS（443）能力，可配置内网 HTTP 代理转发到公网大模型 API。其他服务仍保持内网闭环。
