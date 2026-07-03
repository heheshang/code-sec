# CPG 代码属性图存储方案（Neo4j）

> **版本**: v1.0  
> **状态**: Draft  
> **关联**: SAD § 3.2 代码结构化解析引擎, backend/engine/judge/CallGraphBuilder.java  
> **当前状态**: CallGraphBuilder 构建的 `ProjectCallGraph` 为内存对象，扫描结束即销毁

---

## 1. 背景与问题

### 当前现状

```java
// 当前：内存中的 ProjectCallGraph，扫描完即丢弃
ProjectCallGraph graph = new CallGraphBuilder().build(parsedFiles);
// graph 在 ExploitabilityJudger 中使用后即被 GC
```

**问题清单**：
1. 微服务跨服务调用无法追踪（A 服务 → B 服务的调用链丢失）
2. 同一项目的多次扫描无法做历史对比（代码变更对调用图的影响）
3. 人工审计时无法回溯调用上下文（审计员只能看到单文件）
4. AI 阶段缺少跨模块的完整调用链信息

### 目标

构建基于 **Neo4j 图数据库** 的持久化 CPG 存储，支持：
- 方法级调用链查询（BFS 可达性）
- 跨服务/跨模块调用追踪
- 历史版本对比与增量更新
- 污点传播链路的图查询

---

## 2. 图数据模型

### 2.1 节点类型

```cypher
// 方法节点（核心节点）
(:Method {
    // 主键
    id: "com.codesec.api.UserController.getUser(java.lang.Long)",
    // 方法元数据
    name: "getUser",
    classFqn: "com.codesec.api.UserController",
    filePath: "src/main/java/.../UserController.java",
    lineStart: 42,
    lineEnd: 68,
    parameters: ["Long userId"],
    returnType: "ResponseEntity<User>",
    annotations: ["@GetMapping", "@RequestMapping"],
    modifiers: ["public"],
    // 项目归属
    projectId: "proj-001",
    scanId: "scan-2024-001",
    createdAt: "2024-12-15T10:30:00Z",
    isLatest: true
})

// 类节点
(:Class {
    id: "com.codesec.api.UserController",
    name: "UserController",
    package: "com.codesec.api",
    filePath: "src/main/java/.../UserController.java",
    annotations: ["@RestController"],
    projectId: "proj-001"
})

// 文件节点
(:File {
    id: "proj-001:src/main/java/.../UserController.java",
    path: "src/main/java/.../UserController.java",
    language: "java",
    hash: "sha256:abc123...",         // 文件内容哈希，用于增量检测
    projectId: "proj-001",
    lastModified: "2024-12-15T10:30:00Z"
})

// 入口节点（HTTP 入口点）
(:EntryPoint {
    id: "com.codesec.api.UserController.getUser",
    methodRef: "com.codesec.api.UserController.getUser(java.lang.Long)",
    httpMethod: "GET",
    httpPath: "/api/users/{id}",
    projectId: "proj-001"
})

// 污染源节点
(:TaintSource {
    id: "com.codesec.api.UserController.getUser.userId",
    methodRef: "com.codesec.api.UserController.getUser(java.lang.Long)",
    paramIndex: 0,
    paramName: "userId",
    sourceType: "REQUEST_PARAM",
    annotation: "@PathVariable",
    projectId: "proj-001"
})

// 污染汇聚点节点
(:TaintSink {
    id: "java.sql.Statement.executeQuery",
    methodRef: "java.sql.Statement.executeQuery",
    sinkType: "SQL_EXECUTION",
    cwe: "CWE-89",
    vulnerableParamIndex: 0
})
```

### 2.2 关系类型

```cypher
// 调用关系
(:Method)-[:CALLS {
    lineNumber: 55,
    callSignature: "findById(Long)",
    scanId: "scan-2024-001",
    isLatest: true
}]->(:Method)

// 类声明方法
(:Class)-[:DECLARES {
    lineStart: 42,
    lineEnd: 68
}]->(:Method)

// 文件包含类
(:File)-[:CONTAINS]->(:Class)

// HTTP 入口映射
(:EntryPoint)-[:ENTRY_OF]->(:Method)

// 污染传播
(:TaintSource)-[:TAINT_FLOW {
    propagationPath: ["userId", "id"],
    depth: 3,
    confidence: 0.85
}]->(:TaintSink)

// 方法调用污染源
(:Method)-[:HAS_SOURCE]->(:TaintSource)

// 方法到达污染汇聚点
(:Method)-[:REACHES_SINK]->(:TaintSink)

// 代码变更关系（增量扫描）
(:Method)-[:CHANGED_IN {
    scanId: "scan-2024-002",
    changeType: "MODIFIED|ADDED|DELETED"
}]->(:Method)
```

---

## 3. 核心查询

### 3.1 可达性查询（对应 ReachableAnalyzer）

```cypher
// 从 HTTP 入口到目标方法是否存在路径
// 等价于当前 ReachableAnalyzer 的 BFS 逻辑
MATCH path = (entry:EntryPoint {projectId: $projectId})
              -[:ENTRY_OF]->()
              -[:CALLS*1..15]->(sink:Method {id: $targetMethodId})
WHERE entry.isLatestScan = true
RETURN [node in nodes(path) | node.name] AS callChain,
       length(path) AS depth,
       [rel in relationships(path) | rel.lineNumber] AS lines
ORDER BY depth
LIMIT 10
```

### 3.2 跨服务调用查询

```cypher
// 追踪 A 服务 Controller 到 B 服务 DAO 的调用链
MATCH path = (controller:Method {
               projectId: "service-a",
               annotations: "@RestController"
             })-[:CALLS*]->(dao:Method {
               projectId: "service-b",
               name: "executeQuery"
             })
WHERE controller <> dao
  AND all(node IN nodes(path) WHERE node.projectId IN ["service-a", "service-b"])
RETURN [n IN nodes(path) | n.projectId + "." + n.classFqn + "." + n.name] AS crossServiceChain,
       length(path) AS hopCount
```

### 3.3 污点传播链路

```cypher
// 查询完整的污点传播路径
MATCH (source:TaintSource {projectId: $projectId})
MATCH (sink:TaintSink {cwe: $cwe})
MATCH path = (source)<-[:HAS_SOURCE]-(method:Method)
                -[:CALLS*1..20]->(sinkMethod:Method)
                -[:REACHES_SINK]->(sink)
WHERE method.projectId = $projectId
  AND sinkMethod.isLatest = true
RETURN source.paramName AS taintSource,
       sink.sinkType AS sinkType,
       [n IN nodes(path) WHERE n:Method | n.classFqn + "." + n.name] AS propagationChain,
       length(path) AS propagationDepth
ORDER BY propagationDepth
LIMIT 20
```

### 3.4 增量变更影响分析

```cypher
// 找出本次变更影响的方法和潜在影响范围
MATCH (changed:Method)-[:CHANGED_IN {scanId: $scanId}]->()
// 被变更方法调用的下游（影响分析）
OPTIONAL MATCH downstream = changed-[:CALLS*1..5]->(callee:Method)
// 调用变更方法的上游（影响范围）
OPTIONAL MATCH upstream = (caller:Method)-[:CALLS*1..5]->changed
WHERE caller.projectId = $projectId
RETURN changed.name AS changedMethod,
       [n IN downstream | n.name] AS affectedDownstream,
       [n IN upstream | n.name] AS affectedUpstream
```

---

## 4. 构建流程

### 4.1 全量构建

```
Engine 扫描完成
       │
       ▼
CpgBuilder.build(parsedFiles)
       │
       ├── 1. 解析每个文件的 AST
       ├── 2. 提取所有类/方法声明 → MethodNode + ClassNode
       ├── 3. 提取方法间调用关系 → CallEdge
       ├── 4. 标注 HTTP 入口 → EntryPoint
       ├── 5. 标注污染源/汇聚点 → TaintSource + TaintSink
       ├── 6. 计算所有可达路径
       │       └── 从每个 EntryPoint BFS 遍历调用图
       │       └── 记录可达深度和路径
       └── 7. 批量写入 Neo4j
               └── MERGE 去重写入（避免重复节点）
               └── 设置 isLatest=false 标记旧版本
```

### 4.2 增量更新

```cypher
// 流程：
// 1. 对比文件哈希，识别变更文件
// 2. 删除变更文件对应的旧节点和关系
// 3. 重新解析变更文件，写入新节点
// 4. 重新计算受影响的方法调用链

// 标记旧版本
MATCH (m:Method {projectId: $projectId, isLatest: true})
WHERE m.filePath IN $changedFiles
SET m.isLatest = false

// 删除旧的变更关系
MATCH (:Method)-[r:CHANGED_IN]->(:Method)
WHERE r.scanId IN $oldScanIds
DELETE r
```

### 4.3 数据清理策略

```cypher
// 保留策略：每个项目保留最近 10 次扫描的 CPG
MATCH (m:Method {projectId: $projectId})
WITH m, m.scanId AS scanId
ORDER BY m.createdAt DESC
SKIP 10
DELETE m

// 或基于时间：保留 30 天
MATCH (m:Method {createdAt: $thirtyDaysAgo})
DETACH DELETE m
```

---

## 5. Spring Data Neo4j 集成

### 5.1 实体定义

```java
@Node("Method")
public record MethodNode(
    @Id String id,
    String name,
    String classFqn,
    String filePath,
    int lineStart,
    int lineEnd,
    List<String> parameters,
    String returnType,
    List<String> annotations,
    String projectId,
    String scanId,
    ZonedDateTime createdAt,
    @Property("isLatest") boolean latest
) {}

@Relationship("CALLS")
public record CallRelation(
    @TargetNode MethodNode target,
    int lineNumber,
    String callSignature,
    String scanId
) {}
```

### 5.2 Repository

```java
@Repository
public interface MethodNodeRepository extends Neo4jRepository<MethodNode, String> {

    @Query("MATCH (m:Method {projectId: $projectId, isLatest: true}) " +
           "RETURN m")
    List<MethodNode> findLatestByProjectId(String projectId);

    @Query("MATCH path = (entry:EntryPoint {projectId: $projectId}) " +
           "-[:ENTRY_OF]->()-[:CALLS*1..15]->(sink:Method {id: $sinkId}) " +
           "RETURN path")
    List<Map<String, Object>> findReachablePaths(String projectId, String sinkId);
}
```

---

## 6. 与现有代码的集成

| 现有组件 | 变更方式 |
|----------|----------|
| `CallGraphBuilder.java` | 新增 `buildAndPersist()` 方法，在构建内存图后写入 Neo4j |
| `ReachableAnalyzer.java` | 新增 `neo4jReachable()` 方法，优先查 Neo4j，降级到内存 BFS |
| `TaintTracker.java` | 新增 `persistTaintPath()` 方法 |
| `Engine.scan(Path)` | 扫描完成后异步调用 `CpgService.importGraph()` |
| `ExploitabilityJudger` | CPG 数据源可从 Neo4j 获取（跨服务场景） |

### 架构分层

```
Engine.scan()
    │
    ├── 1. AST 解析（现有）
    ├── 2. 规则检测（现有）
    ├── 3. 内存 CPG 构建（现有 CallGraphBuilder）
    ├── 4. 可利用性判定（现有 ExploitabilityJudger）
    │
    └── 5. 异步写入 Neo4j（新增，不阻塞扫描流程）
            └── CpgService.importGraph(callGraph)
                    └── 批量 MERGE 节点 + 关系
```

---

## 7. 资源估算

| 指标 | 100KLOC 项目 | 500KLOC 项目 | 备注 |
|------|-------------|-------------|------|
| 方法节点数 | ~5,000 | ~25,000 | 每方法一个节点 |
| 调用关系数 | ~15,000 | ~75,000 | 约 3 倍于方法数 |
| 存储占用 | ~50MB | ~250MB | Neo4j 压缩存储 |
| 全量构建时间 | ~30s | ~120s | 含 AST 解析 |
| 可达性查询 P99 | <100ms | <500ms | BFS 深度限制 15 |

---

## 8. 配置

```yaml
# application.yml
cpg:
  enabled: true
  store: neo4j                  # neo4j | memory (memory=不持久化)
  neo4j:
    uri: bolt://localhost:7687
    username: neo4j
    password: ${NEO4J_PASSWORD}
    database: codesec
  build:
    mode: async                 # sync | async（异步不阻塞扫描）
    maxDepth: 15                # 调用链最大深度
    batchSize: 1000             # 批量写入大小
  retention:
    maxScansPerProject: 10      # 每个项目保留最近 N 次扫描
    ttlDays: 30                 # 或基于时间保留
```
