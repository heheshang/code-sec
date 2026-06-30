# ES 集群运维手册

> 适用范围: code-sec v1 Docker Compose 单节点 ES 8.13.0
> 对应 Epic: E-S2-001

## 1. 启动与停止

```bash
# 启动 ES 集群
cd backend/es-integration
docker compose -f docker/docker-compose.yml up -d

# 查看日志
docker compose -f docker/docker-compose.yml logs -f elasticsearch

# 停止
docker compose -f docker/docker-compose.yml down

# 重启
docker compose -f docker/docker-compose.yml restart
```

## 2. 健康检查

```bash
# 集群状态
curl -s http://localhost:9200/_cluster/health?pretty

# 节点统计
curl -s http://localhost:9200/_nodes/stats/jvm?pretty

# 已安装插件
curl -s http://localhost:9200/_cat/plugins?v

# 索引列表
curl -s http://localhost:9200/_cat/indices?v

# 文档数量
curl -s http://localhost:9200/codesec_vuln/_count
```

## 3. Snapshot 备份

```bash
# 创建 snapshot 仓库（首次）
./scripts/es-snapshot.sh create

# 手动触发 snapshot
./scripts/es-snapshot.sh create snap_manual_$(date +%Y%m%d_%H%M%S)

# 查看 snapshot 列表
./scripts/es-snapshot.sh list

# 从 snapshot 恢复
./scripts/es-restore.sh

# Cron 自动备份（每日 02:00）
# 添加到 crontab:
# 0 2 * * * /path/to/backend/es-integration/scripts/es-snapshot-cron.sh
```

## 4. 索引管理

```bash
# 查看 mapping
curl -s http://localhost:9200/codesec_vuln/_mapping?pretty

# 重建索引 (reindex)
curl -X POST "http://localhost:9200/_reindex?pretty" -H 'Content-Type: application/json' -d '{
  "source": {"index": "codesec_vuln"},
  "dest": {"index": "codesec_vuln_v2"}
}'

# 原子切换别名
curl -X POST "http://localhost:9200/_aliases" -H 'Content-Type: application/json' -d '{
  "actions": [
    {"remove": {"index": "codesec_vuln", "alias": "codesec_vuln_read"}},
    {"add": {"index": "codesec_vuln_v2", "alias": "codesec_vuln_read"}}
  ]
}'

# 删除索引
curl -X DELETE "http://localhost:9200/codesec_vuln_v2"
```

## 5. 性能监控

```bash
# JVM heap 采样
./scripts/monitor-heap.sh 120

# 搜索压测
./scripts/load-test-search.sh 1000 60

# Snapshot 窗口压测
./scripts/load-test-snapshot-window.sh

# 造数脚本
./scripts/seed-mock-data.sh 1000000 500000
```

## 6. 故障排查

```bash
# ES 无法启动 → 检查 JVM heap 是否超限
docker logs codesec-es | grep -i "outofmemory\|heap"

# 搜索慢 → 检查慢日志
curl -s "http://localhost:9200/codesec_vuln/_search?pretty" -H 'Content-Type: application/json' -d '{
  "profile": true, "query": {"match_all": {}}
}'

# 磁盘满 → 清理旧 snapshot
curl -X DELETE "http://localhost:9200/_snapshot/local_backup/snap_OLD_DATE"

# 内存不足 → 调低 index_buffer_size
curl -X PUT "http://localhost:9200/_cluster/settings" -H 'Content-Type: application/json' -d '{
  "persistent": {"indices.memory.index_buffer_size": "10%"}
}'
```

## 7. 扩容路径

| 阶段 | 部署 | 规格 |
|------|------|------|
| M1 (v1) | Docker Compose 单节点 | 1 shard × 0 replica, 4GB heap |
| M1 末 | K8s 3 节点 | 1 shard × 1 replica (3 copies), 4GB/node |
| M2+ | K8s ILM 冷热分层 | hot: SSD 2 shards, warm: HDD 1 shard |

## 8. 常用命令速查

1. `curl localhost:9200/_cluster/health?pretty` — 集群健康
2. `curl localhost:9200/_cat/indices?v` — 索引概览
3. `curl localhost:9200/_cat/nodes?v` — 节点列表
4. `curl localhost:9200/_cat/plugins?v` — 插件列表
5. `curl -X POST localhost:9200/codesec_vuln/_refresh` — 手动刷新
6. `curl localhost:9200/_snapshot/local_backup/_all?pretty` — 查看备份
7. `curl localhost:9200/_nodes/stats/jvm?pretty` — JVM 统计
8. `curl -X DELETE localhost:9200/codesec_vuln` — 删除索引 (危险!)
9. `docker compose -f docker/docker-compose.yml logs -f` — 实时日志
10. `docker compose -f docker/docker-compose.yml down -v` — 完全销毁 (包括数据!)
