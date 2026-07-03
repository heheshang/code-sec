package com.codesec.api.module.cpg;

import com.codesec.api.domain.entity.VulnFindingEntity;
import com.codesec.api.domain.repository.VulnFindingRepository;
import com.codesec.api.module.cpg.dto.CpgResponse;
import com.codesec.api.module.cpg.dto.CpgResponse.CpgNode;
import com.codesec.api.module.cpg.dto.CpgResponse.CpgEdge;
import com.codesec.engine.judge.CpgService;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Path;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/cpg")
public class CpgController {

    private final CpgService cpgService;
    private final VulnFindingRepository vulnRepo;

    public CpgController(CpgService cpgService, VulnFindingRepository vulnRepo) {
        this.cpgService = cpgService;
        this.vulnRepo = vulnRepo;
    }

    @GetMapping("/{vulnId}")
    public ResponseEntity<CpgResponse> getCpg(@PathVariable Long vulnId) {
        if (!cpgService.isAvailable()) {
            return ResponseEntity.notFound().build();
        }

        Optional<VulnFindingEntity> vulnOpt = vulnRepo.findById(vulnId);
        if (vulnOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String scanId = vulnOpt.get().getScanTaskId().toString();
        List<Record> methods = cpgService.findLatestByProjectId(scanId);
        List<Record> paths = cpgService.findReachablePaths(scanId);

        CpgResponse response = toResponse(methods, paths);
        return ResponseEntity.ok(response);
    }

    private CpgResponse toResponse(List<Record> methods, List<Record> paths) {
        List<CpgNode> nodes = new ArrayList<>();
        List<CpgEdge> edges = new ArrayList<>();

        for (Record method : methods) {
            Value n = method.get("n");
            nodes.add(new CpgNode(
                n.get("key").asString(),
                n.get("name", ""),
                n.get("className", ""),
                n.get("signatureKey", ""),
                n.get("startLine", 0)
            ));
        }

        for (Record path : paths) {
            Path p = path.get("path").asPath();
            for (Path.Segment seg : p) {
                edges.add(new CpgEdge(
                    seg.start().get("key").asString(),
                    seg.end().get("key").asString(),
                    "CALLS"
                ));
            }
        }

        return new CpgResponse(nodes, edges);
    }
}
