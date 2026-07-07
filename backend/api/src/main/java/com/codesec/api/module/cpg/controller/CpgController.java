package com.codesec.api.module.cpg.controller;

import com.codesec.api.module.cpg.CpgQueryService;
import com.codesec.api.module.cpg.dto.CpgResponse;
import com.codesec.engine.judge.CpgService;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/cpg")
@PreAuthorize("@perm.check('cpg:read')")
public class CpgController {

    private static final Logger log = LoggerFactory.getLogger(CpgController.class);
    private final CpgService cpgService;
    private final CpgQueryService cpgQueryService;

    public CpgController(CpgService cpgService, CpgQueryService cpgQueryService) {
        this.cpgService = cpgService;
        this.cpgQueryService = cpgQueryService;
    }

    @GetMapping("/{vulnId}")
    public ResponseEntity<CpgResponse> getCpg(@PathVariable Long vulnId) {
        Optional<CpgResponse> response = cpgQueryService.getCpgForVuln(vulnId);
        if (response.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response.get());
    }

    @PostMapping("/demo/{vulnId}")
    public ResponseEntity<String> importDemoData(@PathVariable Long vulnId) {
        if (!cpgService.isAvailable()) {
            return ResponseEntity.status(503).body("Neo4j not available");
        }

        var vulnOpt = cpgQueryService.findVulnById(vulnId);
        if (vulnOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var vuln = vulnOpt.get();
        String scanId = vuln.getScanTaskId().toString();
        String filePath = vuln.getFilePath();

        log.info("Importing demo CPG data for vuln {}, scanId={}", vulnId, scanId);

        try {
            importDemoGraph(scanId, filePath);
        } catch (Exception e) {
            log.error("Failed to import demo CPG data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Import failed: " + e.getMessage());
        }

        return ResponseEntity.ok("Demo CPG data imported for vuln " + vulnId);
    }

    private void importDemoGraph(String scanId, String filePath) {
        String pkg = "com.example.app";

        String entryKey = pkg + ".EntryController.handleRequest()";
        String serviceKey = pkg + ".UserService.authenticate(String)";
        String dbKey = pkg + ".UserRepository.findByUsername(String)";
        String cryptoKey = pkg + ".PasswordUtil.hash(String)";
        String vulnKey = pkg + ".LegacyAuth.checkPassword(String,String)";

        String methodsCypher = """
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
                n.projectId = m.scanId,
                n.scanId = m.scanId,
                n.isLatest = true
            """;

        List<Value> methodParams = List.of(
            methodParam(entryKey, "handleRequest", pkg + ".EntryController", "handleRequest()", "void",
                "@RequestMapping,@PostMapping", 25, 40, false, true, scanId),
            methodParam(serviceKey, "authenticate", pkg + ".UserService", "authenticate(String)", "boolean",
                "", 42, 55, false, true, scanId),
            methodParam(dbKey, "findByUsername", pkg + ".UserRepository", "findByUsername(String)", "User",
                "@Repository", 10, 15, false, false, scanId),
            methodParam(cryptoKey, "hash", pkg + ".PasswordUtil", "hash(String)", "String",
                "", 8, 12, true, true, scanId),
            methodParam(vulnKey, "checkPassword", pkg + ".LegacyAuth", "checkPassword(String,String)", "boolean",
                "@Deprecated", 15, 22, true, false, scanId)
        );

        String edgesCypher = """
            UNWIND $edges AS e
            MATCH (caller:Method {key: e.callerKey})
            MERGE (callee:Method {key: e.calleeKey})
            MERGE (caller)-[r:CALLS]->(callee)
            SET r.lineNumber = e.lineNumber,
                r.callSignature = e.callSignature,
                r.scanId = e.scanId
            """;

        List<Value> edgeParams = List.of(
            edgeParam(entryKey, serviceKey, "authenticate(String)", 30, scanId),
            edgeParam(serviceKey, dbKey, "findByUsername(String)", 45, scanId),
            edgeParam(serviceKey, cryptoKey, "hash(String)", 48, scanId),
            edgeParam(serviceKey, vulnKey, "checkPassword(String,String)", 50, scanId)
        );

        try (Session s = cpgService.getDriver().session()) {
            s.executeWrite(tx -> {
                tx.run(methodsCypher, Values.parameters("methods", methodParams));
                tx.run(edgesCypher, Values.parameters("edges", edgeParams));
                return null;
            });
        }

        log.info("Demo CPG imported: 5 methods, 4 edges for scanId={}", scanId);
    }

    private Value methodParam(String key, String name, String className, String signatureKey,
                              String returnType, String annotations, int startLine, int endLine,
                              boolean isStatic, boolean isPublic, String scanId) {
        return Values.parameters(
            "key", key, "name", name, "className", className,
            "signatureKey", signatureKey, "returnType", returnType,
            "annotations", annotations, "startLine", startLine, "endLine", endLine,
            "isStatic", isStatic, "isPublic", isPublic,
            "scanId", scanId
        );
    }

    private Value edgeParam(String callerKey, String calleeKey, String callSignature,
                            int lineNumber, String scanId) {
        return Values.parameters(
            "callerKey", callerKey, "calleeKey", calleeKey,
            "callSignature", callSignature, "lineNumber", lineNumber, "scanId", scanId
        );
    }

}
