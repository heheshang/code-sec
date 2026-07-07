package com.codesec.engineadapter;

import java.util.List;

/**
 * Filter for exempted rules. Implemented in the domain module where
 * project_rule_exemption data is accessible.
 *
 * The filter operates on {@link FindingDto} so the domain layer
 * never needs to depend on the engine's internal model.
 */
@FunctionalInterface
public interface ExemptionFilter {
    /**
     * Filter out findings that match exempted rules for the given project.
     *
     * @param findings  all findings from the scan (as DTOs)
     * @param projectId the project (repo) ID
     * @return findings with exempted rules removed (as DTOs)
     */
    List<FindingDto> filterExempted(List<FindingDto> findings, Long projectId);
}
