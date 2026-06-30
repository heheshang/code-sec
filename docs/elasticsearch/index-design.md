# ES Index Design — vuln & file_snippet

> **Epic**: E-S2-001  
> **Date**: 2026-07-01  
> **Version**: v1 (2-analyzer simplified per MF-4)

## 1. Index Overview

| Index | Purpose | Documents (est.) | Analyzer Strategy | Size (est.) |
|-------|---------|-----------------|-------------------|-------------|
| codesec_vuln | Vulnerability full-text search | 1M | `ik_smart` (title/desc) + `standard` (code_snippet) | ~3.5 GB |
| codesec_file_snippet | Code snippet file_path search (v1) | 500K | keyword only (no content in v1) | ~150 MB |

## 2. Vuln Index — 13 Fields

All 13 fields map to MySQL `vuln_finding` table columns (E-S2-CRITICAL § 3.2 注 2.1).

| # | ES Field | ES Type | Analyzer | MySQL Column | Notes |
|---|----------|---------|----------|-------------|-------|
| 1 | id | keyword | — | id BIGINT | Primary key |
| 2 | project_id | keyword | — | project_id BIGINT | Redundant (avoids repo join) |
| 3 | rule_id | keyword | — | rule_id VARCHAR | e.g. java/sql-injection-001 |
| 4 | severity | keyword | — | severity VARCHAR | critical/high/medium/low/info |
| 5 | exploitability | keyword | — | exploitability VARCHAR | EXPLOITABLE/POTENTIALLY/NOT |
| 6 | title | text | ik_smart (boost 3) | title VARCHAR | Chinese + English |
| 7 | description | text | ik_smart (boost 2) | description TEXT | Chinese + English |
| 8 | code_snippet | text | standard | code_snippet TEXT | Code search (no IK needed) |
| 9 | file_path | keyword | — | file_path VARCHAR | Exact match + prefix query |
| 10 | cwe | keyword | — | cwe VARCHAR | e.g. CWE-89 |
| 11 | engine | keyword | — | engine VARCHAR | self_sast/codeql/etc |
| 12 | discovered_at | date | — | discovered_at TIMESTAMP | Range filter |
| 13 | discovered_by | keyword | — | discovered_by VARCHAR | Scanner/user ID |

**NOT indexed**: `line_start`, `line_end` (retained in MySQL for detail page links), `scan_task_id`, `dedup_key`, `created_at`, `updated_at`.

## 3. File Snippet Index — 4 Fields (v1 alpha)

| # | ES Field | ES Type | Analyzer | Notes |
|---|----------|---------|----------|-------|
| 1 | file_path | keyword | — | Exact match + prefix query |
| 2 | project_id | keyword | — | Project association |
| 3 | language | keyword | — | java/go/python/etc |
| 4 | indexed_at | date | — | Index timestamp |

**NOT indexed in v1**: `content` (pushed to Sprint 3 to avoid ngram memory explosion).

## 4. Analyzer Strategy

### 4.1 Why 2 Analyzers (not 4)

| Analyzer | Used For | Reason |
|----------|----------|--------|
| `ik_smart` | title, description (Chinese + English) | Chinese tokenization for 中文搜索 |
| `standard` | code_snippet, title.standard, description.standard | English/code tokenization |

**Removed (MF-4)**:
- `edge_ngram(2-10)`: Replaced by `keyword` + prefix query for file_path
- `ngram(3-5)`: Removed entirely — would generate ~500M tokens on 1M docs (6GB heap insufficient)

### 4.2 file_path Prefix Query

```json
{
  "query": {
    "prefix": {
      "file_path": "src/main/java/com/example"
    }
  }
}
```

This replaces `edge_ngram` with equivalent functionality at zero indexing cost.

## 5. Reindex Procedure

When mapping changes are needed:

```bash
# 1. Create new index with updated mapping
curl -X PUT "localhost:9200/codesec_vuln_v2" -H 'Content-Type: application/json' -d @vuln_v2.json

# 2. Reindex from old to new
curl -X POST "localhost:9200/_reindex" -H 'Content-Type: application/json' -d '{
  "source": {"index": "codesec_vuln"},
  "dest": {"index": "codesec_vuln_v2"}
}'

# 3. Atomic alias switch
curl -X POST "localhost:9200/_aliases" -H 'Content-Type: application/json' -d '{
  "actions": [
    {"add": {"index": "codesec_vuln_v2", "alias": "codesec_vuln"}},
    {"remove": {"index": "codesec_vuln_old", "alias": "codesec_vuln"}}
  ]
}'

# 4. Verify then delete old index
curl -X DELETE "localhost:9200/codesec_vuln_old"
```

## 6. Index Size Estimation

| Index | Formula | Result |
|-------|---------|--------|
| vuln | 1M × 3KB (source) × 1.2 (inverted index overhead) | ~3.5 GB |
| file_snippet | 500K × 300B × 1.05 | ~150 MB |
| **Total** | | **~3.65 GB** |

✅ 6GB heap budget (4GB heap + 2GB filesystem cache overhead) is safe.
