# 需求规格说明书 — E-S3-PERF M1.5 性能优化

> **Epic ID**: E-S3-PERF
> **Epic 名称**: M1.5 性能优化（QG-6 40s→30s）
> **变更目录**: `.harness/changes/feat-perf-m1.5-20260701/`
> **创建日期**: 2026-07-01
> **对应 Sprint**: Sprint 3（M1 第三冲刺）
> **总工时**: 40h
> **优先级**: P2
> **关联文档**:
> - 冲刺合同：`.harness/changes/sprint-3/contract.md` § 5.1
> - Benchmark 报告：`engine/BENCHMARK.md`
> - 引擎架构：`engine/src/main/java/com/codesec/engine/`
> - 调用图构建：`engine/src/main/java/com/codesec/engine/judge/`

---

## 1. 背景

`engine/BENCHMARK.md` 测得当前引擎对 10 万行代码的性能为 **~46s 扫描时间**、**~2.4GB 内存峰值**。

**目标**（M1.5）：≤ **30s 扫描时间**、≤ **2GB 内存峰值**。

瓶颈分析（BENCHMARK.md § Bottleneck Analysis）：

| 阶段 | 耗时 | 占比 | 瓶颈类型 |
|------|------|------|----------|
| JavaParser AST 解析 | ~15,000ms | 37% | CPU+IO |
| CallGraphBuilder | ~8,000ms | 20% | CPU |
| Judgment（3 analyzers） | ~17,000ms | 42% | CPU |
| 其他 | ~77ms | <1% | — |

---

## 2. 需求描述

### 2.1 范围与边界

**范围内**：
- JavaParser 解析缓存（相同文件重复解析优化）
- ForkJoinPool 并行扫描优化
- Caffeine 一级缓存（RuleRegistry、检测结果缓存）
- Judgment 阶段并行度优化
- 内存优化（减少中间对象分配）
- Benchmark 对比验证

**范围外**：
- ❌ JavaParser 替换为 tree-sitter — M2（已独立为 E-S3-MULTI）
- ❌ JVM GC 调优 — 独立 devops 任务
- ❌ 分布式扫描 — M4
- ❌ 增量扫描缓存（已有 GitLab 联调实现）

### 2.2 交付物清单

| # | 交付物 | 工时 |
|---|--------|------|
| T1 | 引擎热点分析 + 详细 profiling | 8h |
| T2 | 并行扫描优化（ForkJoinPool） | 12h |
| T3 | 缓存策略优化（Caffeine 引入） | 12h |
| T4 | Benchmark 验证 + 回归门禁 | 8h |

---

## 3. 技术方案

### 3.1 ForkJoinPool 并行扫描

当前 `Engine.scan()` 串行遍历文件：

```java
// 当前（伪代码）
for (Path file : allFiles) {
    ParsedFile parsed = parser.parse(file);
    List<Finding> findings = detect(parsed, rules);
    allFindings.addAll(findings);
}
```

目标改为并行：

```java
// 优化后
ForkJoinPool pool = new ForkJoinPool(parallelism);
List<Path> batches = partition(allFiles, chunkSize); // 每批 20-50 文件
List<Future<List<Finding>>> futures = batches.stream()
    .map(batch -> pool.submit(() -> scanBatch(batch, parser, rules)))
    .toList();
```

### 3.2 Caffeine 缓存

| 缓存 | Key | Value | TTL | 预期效果 |
|------|-----|-------|-----|----------|
| RuleRegistry | ruleId | Rule | 10min | 减少 YAML 重复加载 |
| ParsedFileCache | file absolute path | CompilationUnit | session | 避免重复解析（XSS + SQL 检测重复使用） |
| CallGraphCache | digest(fileContent) | CallGraph | session | 相同源码重复扫描命中缓存 |

### 3.3 Judgment 阶段优化

当前 Judgment 为每个 finding 启动 3 个 Future：

- 合并 ReachableAnalyzer + FrameworkProtectionDetector → 一次调用图遍历
- InputControllabilityAnalyzer 在 Reachable 基础上分析，减少重复数据流遍历
- 预期 Judgment 阶段从 17s → 10s

---

## 4. 验收标准

- [ ] `mvn -pl engine test` 测试全部通过（零回归）
- [ ] Benchmark 脚本 `engine/scripts/benchmark.sh` 运行通过
- [ ] 扫描时间从 ~46s 降至 ≤ 30s（10 万行 synthetic 项目）
- [ ] 内存峰值从 ~2.4GB 降至 ≤ 2GB
- [ ] 精度无退化：Precision/Recall 保持 100%
- [ ] Benchmark 差异在报告中有记录
