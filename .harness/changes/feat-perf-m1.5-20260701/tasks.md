# 任务拆解 — E-S3-PERF M1.5 性能优化

> 基于 `spec.md`（E-S3-PERF）拆解的可执行任务清单。
> **总工时**: 40h

---

## 任务列表

### 任务 1：引擎热点分析 + 详细 profiling

- **优先级**: P2
- **估计工时**: 8h
- **涉及模块**: `engine/`
- **涉及文件**:
  - `engine/src/main/java/com/codesec/engine/Engine.java` — 添加性能埋点
  - `engine/src/main/java/com/codesec/engine/util/Profiler.java` — 简易性能分析器（新增）
  - `engine/BENCHMARK.md` — 更新详细瓶颈数据
- **验收标准**:
  - [ ] Profiler 可输出各阶段耗时（解析/检测/调用图/Judgment）
  - [ ] 运行一次 benchmark 输出 profiling 报告
  - [ ] 瓶颈数据量化到方法级别
  - [ ] 不改变引擎输出结果
- **依赖任务**: 无

---

### 任务 2：ForkJoinPool 并行扫描

- **优先级**: P2
- **估计工时**: 12h
- **涉及模块**: `engine/`
- **涉及文件**:
  - `engine/src/main/java/com/codesec/engine/Engine.java` — 引入 ForkJoinPool 并行 scanBatch
  - `engine/src/main/java/com/codesec/engine/config/EngineConfig.java` — 添加 parallelism 配置项（新增）
  - `engine/src/main/java/com/codesec/engine/util/FileBatcher.java` — 文件分批工具（新增）
- **验收标准**:
  - [ ] 并行度可配置（默认 = availableProcessors - 1）
  - [ ] 10 万行扫描时间降至 ≤ 35s（42% 的 Judgment 阶段从 17s 降到 10s）
  - [ ] 并行版本结果与串行版本完全一致（排序无关）
  - [ ] 文件级异常不影响其他文件扫描
- **依赖任务**: T1

---

### 任务 3：Caffeine 缓存池

- **优先级**: P2
- **估计工时**: 12h
- **涉及模块**: `engine/`
- **涉及文件**:
  - `engine/pom.xml` — 添加 caffeine 依赖
  - `engine/src/main/java/com/codesec/engine/cache/ParsedFileCache.java` — CompilationUnit 缓存（新增）
  - `engine/src/main/java/com/codesec/engine/cache/CallGraphCache.java` — 调用图缓存（新增）
  - `engine/src/main/java/com/codesec/engine/cache/DetectionCache.java` — 检测结果缓存（新增）
  - `engine/src/main/java/com/codesec/engine/Engine.java` — 注入缓存
- **验收标准**:
  - [ ] ParsedFileCache 避免同一文件重复解析（命中率 ≥ 80%）
  - [ ] CallGraphCache 相同源码文件复用调用图
  - [ ] 10 万行扫描时间降至 ≤ 32s（37% 的解析阶段从 15s 降到 10s）
  - [ ] 缓存命中/未命中统计可导出
  - [ ] 内存峰值不因缓存增加而超 2GB
- **依赖任务**: T1

---

### 任务 4：Benchmark 验证 + 门禁

- **优先级**: P2
- **估计工时**: 8h
- **涉及模块**: `engine/`
- **涉及文件**:
  - `engine/scripts/benchmark.sh` — 更新 benchmark 脚本
  - `engine/BENCHMARK.md` — 更新对比数据
  - `engine/pom.xml` — 添加 benchmark profile 门禁
- **验收标准**:
  - [ ] Benchmark 跑完显示优化前后的对比
  - [ ] ≤ 30s 扫描时间（10 万行）/ ≤ 2GB 内存峰值
  - [ ] Precision/Recall 仍为 100%
  - [ ] `mvn test -Pbenchmark` 门禁通过
  - [ ] Benchmark 报告输出到 BENCHMARK.md
- **依赖任务**: T2, T3

---

## 排期

| 任务 | 优先级 | 工时 | 依赖 | 计划开始 |
|------|--------|------|------|----------|
| T1: 热点分析 | P2 | 8h | - | D8 |
| T2: ForkJoinPool | P2 | 12h | T1 | D8 |
| T3: Caffeine 缓存 | P2 | 12h | T1 | D8 |
| T4: Benchmark 验证 | P2 | 8h | T2,T3 | D9 |
