# ES Search Benchmark Report — 2026 Q3 (v1)

> **Epic**: E-S2-001 — ES Full-Text Search
> **Date**: 2026-07-01
> **Environment**: Docker Compose single-node ES 8.13.0, 4GB heap, IK ik_smart

## 1. Environment

| Component | Specification |
|-----------|--------------|
| ES Version | 8.13.0 |
| Deployment | Docker Compose single-node |
| JVM Heap | 2GB min / 4GB max (G1GC) |
| Analyzers | standard + ik_smart (2-analyzer simplified strategy) |
| Shards × Replicas | 1 × 0 |
| path.data | 3 directories (multi-disk) |
| Snapshot | Daily 02:00, local backup volume |
| Test Data | 1M vuln docs + 500K snippet docs |

## 2. Index Sizes

| Index | Documents | Doc Size (avg) | Index Size (est.) | Analyzer Overhead |
|-------|-----------|---------------|-------------------|-------------------|
| codesec_vuln | 1,000,000 | ~3 KB | ~3.5 GB | ik_smart + standard (dual analyzer on title/desc) |
| codesec_file_snippet | 500,000 | ~300 B | ~150 MB | keyword only (v1, no content) |
| **Total** | 1,500,000 | — | **~3.65 GB** | 2-analyzer simplified, no ngram |

## 3. Performance Results

### 3.1 API Latency (1M docs, 1000 QPS × 60s)

| Metric | Target | Result | Status |
|--------|--------|--------|--------|
| P50 | < 100ms | TBD (run load-test-search.sh) | ⬜ |
| P99 | < 500ms | TBD | ⬜ |
| Error rate | < 0.1% | TBD | ⬜ |

### 3.2 Heap Usage

| Metric | Target | Result | Status |
|--------|--------|--------|--------|
| Peak heap | < 4GB | TBD (run monitor-heap.sh) | ⬜ |
| Avg heap | — | TBD | ⬜ |

### 3.3 Snapshot Impact

| Metric | Target | Result | Status |
|--------|--------|--------|--------|
| P99 delta during snapshot | < 100ms | TBD (run load-test-snapshot-window.sh) | ⬜ |

### 3.4 Bulk Import

| Metric | Target | Result | Status |
|--------|--------|--------|--------|
| 1M vulns bulk insert | < 10 min | TBD (run seed-mock-data.sh) | ⬜ |
| 500K snippets bulk insert | — | TBD | ⬜ |

## 4. 2-Analyzer vs 4-Analyzer Comparison

| Dimension | 4-Analyzer (original) | 2-Analyzer (simplified, MF-4) |
|-----------|----------------------|-------------------------------|
| Analyzers | standard, ik_smart, edge_ngram(2-10), ngram(3-5) | standard, ik_smart only |
| file_path indexing | edge_ngram(2-10) | keyword + prefix query |
| file_snippet content | ngram(3-5) on full content | Not indexed (v1, Sprint 3) |
| Estimated tokens (1M docs) | ~500M | ~50M |
| Heap budget | 6GB (estimated OOM) | < 4GB (safe) |
| Total index size | ~8 GB | ~3.65 GB |
| Chinese search | ik_smart (full) | ik_smart (full) ✅ |
| Code search (prefix) | edge_ngram | keyword prefix query ✅ |

## 5. Tuning Notes

- `refresh_interval`: 5s (vuln), 10s (file_snippet) — trades indexing latency for search throughput
- `max_result_window`: 10,000 — deep pagination ceiling
- `indices.query.bool.max_clause_count`: 4096 — allows complex filter combinations
- `indices.memory.index_buffer_size`: 20% — balances indexing vs search memory
- No ILM in v1 — all data retained 1+ year, monthly sharding at 1M+ scale

## 6. Optimization Recommendations

1. **Query cache**: Enable `index.queries.cache.enabled: true` for repeated filter queries
2. **Result window**: If pagination > 10K is needed, use `search_after` instead of `from/size`
3. **Index buffer**: Increase to 30% during bulk seeding, revert to 20% for steady-state
4. **Merges**: Set `index.merge.policy.max_merged_segment: 5gb` for large shards
5. **K8s migration**: M2 switch to 3-node ES with 1 replica (3 copies total) — eliminates SPOF
