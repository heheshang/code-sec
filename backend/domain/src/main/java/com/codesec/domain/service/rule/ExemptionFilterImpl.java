package com.codesec.domain.service.rule;

import com.codesec.domain.entity.RuleMetadataEntity;
import com.codesec.domain.repository.ProjectExemptionRepository;
import com.codesec.domain.repository.RuleMetadataRepository;
import com.codesec.common.dto.FindingDto;
import com.codesec.engineadapter.ExemptionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExemptionFilterImpl implements ExemptionFilter {
    private final RuleMetadataRepository ruleRepo;
    private final ProjectExemptionRepository exemptionRepo;

    @Override
    public List<FindingDto> filterExempted(List<FindingDto> findings, Long projectId) {
        if (findings == null || findings.isEmpty()) return findings;

        // Get all exempted rule IDs for this project
        var exemptions = exemptionRepo.findByProjectId(projectId);
        if (exemptions.isEmpty()) return findings;

        // Map exemption rule_ids to their rule_id strings
        Set<String> exemptedRuleIds = exemptions.stream()
            .map(e -> ruleRepo.findById(e.getRuleId()))
            .filter(java.util.Optional::isPresent)
            .map(java.util.Optional::get)
            .map(RuleMetadataEntity::getRuleId)
            .collect(Collectors.toSet());

        if (exemptedRuleIds.isEmpty()) return findings;

        // Filter out findings whose ruleId is in the exempted set
        return findings.stream()
            .filter(f -> !exemptedRuleIds.contains(f.ruleId()))
            .toList();
    }
}
