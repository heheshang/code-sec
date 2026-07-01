package com.codesec.engineadapter;

import com.codesec.engine.model.Finding;
import java.util.List;

/**
 * Filter for exempted rules. Implemented in the API module where
 * project_rule_exemption data is accessible.
 *
 * Sprint 3: injected into EngineAdapterImpl to filter findings
 * against project-level rule exemptions.
 */
@FunctionalInterface
public interface ExemptionFilter {
    /**
     * Filter out findings that match exempted rules for the given project.
     *
     * @param findings  all findings from the scan
     * @param projectId the project (repo) ID
     * @return findings with exempted rules removed
     */
    List<Finding> filterExempted(List<Finding> findings, Long projectId);
}
