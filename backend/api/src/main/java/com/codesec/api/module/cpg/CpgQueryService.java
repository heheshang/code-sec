package com.codesec.api.module.cpg;

import com.codesec.api.module.cpg.dto.CpgResponse;
import com.codesec.api.module.cpg.dto.CpgResponse.CpgEdge;
import com.codesec.api.module.cpg.dto.CpgResponse.CpgNode;
import com.codesec.domain.entity.VulnFindingEntity;
import com.codesec.domain.repository.VulnFindingRepository;
import com.codesec.engine.judge.CpgService;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Path;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service layer that queries CPG (Code Property Graph) data from Neo4j
 * and maps raw driver records into {@link CpgResponse} DTOs.
 */
@Service
public class CpgQueryService {

    private final CpgService cpgService;
    private final VulnFindingRepository vulnRepo;

    public CpgQueryService(CpgService cpgService, VulnFindingRepository vulnRepo) {
        this.cpgService = cpgService;
        this.vulnRepo = vulnRepo;
    }

    /**
     * Checks whether Neo4j CPG is available.
     */
    public boolean isCpgAvailable() {
        return cpgService.isAvailable();
    }

    /**
     * Looks up a vuln finding entity by id.
     */
    public Optional<VulnFindingEntity> findVulnById(Long vulnId) {
        return vulnRepo.findById(vulnId);
    }

    /**
     * Returns the CPG response for the given vuln, or empty if
     * Neo4j is unavailable or the vuln does not exist.
     */
    public Optional<CpgResponse> getCpgForVuln(Long vulnId) {
        if (!cpgService.isAvailable()) {
            return Optional.empty();
        }

        Optional<VulnFindingEntity> vulnOpt = vulnRepo.findById(vulnId);
        if (vulnOpt.isEmpty()) {
            return Optional.empty();
        }

        String scanId = vulnOpt.get().getScanTaskId().toString();
        List<Record> methods = cpgService.findLatestByProjectId(scanId);
        List<Record> paths = cpgService.findReachablePaths(scanId);

        return Optional.of(toResponse(methods, paths));
    }

    private CpgResponse toResponse(List<Record> methods, List<Record> paths) {
        List<CpgNode> nodes = new ArrayList<>();
        List<CpgEdge> edges = new ArrayList<>();

        for (Record method : methods) {
            Value n = method.get("n");
            nodes.add(new CpgNode(
                n.get("key").asString(),
                n.get("name", ""),
                n.get("annotations", ""),
                n.get("className", ""),
                n.get("startLine", 0)
            ));
        }

        Set<String> seenEdges = new HashSet<>();

        for (Record path : paths) {
            Path p = path.get("path").asPath();
            for (Path.Segment seg : p) {
                String src = seg.start().get("key").asString();
                String tgt = seg.end().get("key").asString();
                String edgeKey = src + "->" + tgt;
                if (seenEdges.add(edgeKey)) {
                    edges.add(new CpgEdge(src, tgt, "CALLS"));
                }
            }
        }

        return new CpgResponse(nodes, edges);
    }
}
