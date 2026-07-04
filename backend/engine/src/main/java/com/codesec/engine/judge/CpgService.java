package com.codesec.engine.judge;

import com.codesec.engine.config.CpgConfiguration;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CpgService {
    private static final Logger log = LoggerFactory.getLogger(CpgService.class);

    private final CpgConfiguration config;
    private volatile Driver driver;

    public CpgService(CpgConfiguration config) {
        this.config = config;
        if (config.isEnabled()) {
            this.driver = config.getDriver();
            verifyConnection();
        }
    }

    private void verifyConnection() {
        try {
            driver.verifyConnectivity();
            log.info("Neo4j connected: {}", config.getUri());
        } catch (Exception e) {
            log.warn("Neo4j connection failed: {}. CPG will stay in memory.", e.getMessage());
            this.driver = null;
        }
    }

    /**
     * Asynchronously imports a ProjectCallGraph into Neo4j.
     * Uses UNWIND + MERGE for batch insertion.
     */
    public CompletableFuture<Void> importGraphAsync(ProjectCallGraph graph, String scanId) {
        return CompletableFuture.runAsync(() -> importGraph(graph, scanId));
    }

    /**
     * Synchronously imports a ProjectCallGraph into Neo4j.
     */
    public void importGraph(ProjectCallGraph graph, String scanId) {
        if (driver == null) {
            log.warn("Neo4j driver unavailable; skipping CPG persistence");
            return;
        }

        List<MethodNode> methods = graph.getAllMethods();
        if (methods.isEmpty()) {
            log.info("Empty CPG; nothing to persist");
            return;
        }

        long start = System.currentTimeMillis();
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                importMethods(tx, methods, scanId);
                return null;
            });
            session.executeWrite(tx -> {
                importEdges(tx, methods, graph, scanId);
                return null;
            });
            long elapsed = System.currentTimeMillis() - start;
            log.info("Imported {} methods and {} edges into Neo4j in {}ms",
                methods.size(), graph.edgeCount(), elapsed);
        } catch (Exception e) {
            log.error("Failed to import CPG into Neo4j: {}", e.getMessage(), e);
        }
    }

    private void importMethods(org.neo4j.driver.TransactionContext tx, List<MethodNode> methods, String scanId) {
        String cypher = """
            UNWIND $methods AS m
            MERGE (n:Method {key: m.key})
            SET n.name = m.name,
                n.className = m.className,
                n.signatureKey = m.signatureKey,
                n.returnType = m.returnType,
                n.annotations = m.annotations,
                n.startLine = m.startLine,
                n.endLine = m.endLine,
                n.isStatic = m.isStatic,
                n.isPublic = m.isPublic,
                n.projectId = m.projectId,
                n.scanId = m.scanId,
                n.isLatest = m.isLatest
            """;

        List<org.neo4j.driver.Value> params = new ArrayList<>();
        for (MethodNode m : methods) {
            params.add(Values.parameters(
                "key", m.key(),
                "name", m.methodName(),
                "className", m.className(),
                "signatureKey", m.signatureKey(),
                "returnType", m.returnType(),
                "annotations", String.join(",", m.annotations()),
                "startLine", m.startLine(),
                "endLine", m.endLine(),
                "isStatic", m.isStatic(),
                "isPublic", m.isPublic(),
                "projectId", scanId,
                "scanId", scanId,
                "isLatest", true
            ));
        }

        tx.run(cypher, Values.parameters("methods", params));
    }

    private void importEdges(org.neo4j.driver.TransactionContext tx, List<MethodNode> methods,
                             ProjectCallGraph graph, String scanId) {
        List<org.neo4j.driver.Value> edgeParams = new ArrayList<>();
        for (MethodNode caller : methods) {
            List<CallEdge> outgoing = graph.getOutgoingCalls(caller);
            for (CallEdge edge : outgoing) {
                edgeParams.add(Values.parameters(
                    "callerKey", edge.caller().key(),
                    "calleeKey", edge.caller().className() + "." + edge.calleeName() +
                        "(" + edge.calleeSignature() + ")",
                    "calleeName", edge.calleeName(),
                    "calleeSignature", edge.calleeSignature(),
                    "callSiteLine", edge.callSiteLine(),
                    "scanId", scanId
                ));
            }
        }

        if (edgeParams.isEmpty()) return;

        String cypher = """
            UNWIND $edges AS e
            MATCH (caller:Method {key: e.callerKey})
            MERGE (callee:Method {key: e.calleeKey})
            MERGE (caller)-[r:CALLS]->(callee)
            SET r.lineNumber = e.callSiteLine,
                r.callSignature = e.calleeName + '(' + e.calleeSignature + ')',
                r.scanId = e.scanId
            """;

        tx.run(cypher, Values.parameters("edges", edgeParams));
    }

    /**
     * Finds all methods marked as latest for a project.
     */
    public List<Record> findLatestByProjectId(String scanId) {
        if (driver == null) return List.of();
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (n:Method) WHERE n.scanId = $scanId AND n.isLatest = true RETURN n",
                Values.parameters("scanId", scanId)
            );
            return result.list();
        }
    }

    /**
     * Finds reachable paths from entry points to sink methods using Cypher.
     */
    public List<Record> findReachablePaths(String scanId) {
        if (driver == null) return List.of();
        try (Session session = driver.session()) {
            String cypher = """
                MATCH path = (entry:Method {isLatest: true, scanId: $scanId})
                    -[:CALLS*1..10]->(sink:Method {scanId: $scanId})
                WHERE entry.annotations CONTAINS '@RequestMapping'
                   OR entry.annotations CONTAINS '@GetMapping'
                   OR entry.annotations CONTAINS '@PostMapping'
                RETURN path
                LIMIT 100
                """;
            Result result = session.run(cypher, Values.parameters("scanId", scanId));
            return result.list();
        }
    }

    /**
     * Clears all CPG data for a given scan (used before re-import).
     */
    public void clearScan(String scanId) {
        if (driver == null) return;
        try (Session session = driver.session()) {
            session.run(
                "MATCH (n:Method {scanId: $scanId}) DETACH DELETE n",
                Values.parameters("scanId", scanId)
            );
            log.info("Cleared CPG data for scan: {}", scanId);
        }
    }

    /**
     * Marks a previous scan's methods as not latest (before activating new scan).
     */
    public void markPreviousNotLatest(String previousScanId) {
        if (driver == null) return;
        try (Session session = driver.session()) {
            session.run(
                "MATCH (n:Method {scanId: $scanId}) SET n.isLatest = false",
                Values.parameters("scanId", previousScanId)
            );
        }
    }

    public boolean isAvailable() {
        return driver != null;
    }

    public Driver getDriver() {
        return driver;
    }

    public void close() {
        config.close();
    }
}
