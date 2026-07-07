package com.codesec.domain.service.rule;

import com.codesec.common.exception.NotFoundException;
import com.codesec.common.exception.BadRequestException;import com.codesec.domain.entity.ProjectExemptionEntity;
import com.codesec.domain.entity.RuleMetadataEntity;
import com.codesec.domain.repository.ProjectExemptionRepository;
import com.codesec.domain.repository.RuleMetadataRepository;
import com.codesec.common.dto.PaginatedResult;
import com.codesec.common.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleService {
    private final RuleMetadataRepository ruleRepo;
    private final ProjectExemptionRepository exemptionRepo;

    // ========== Rule Metadata CRUD ==========

    public PaginatedResult<RuleResponse> listRules(String severity, String language, String engine, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Specification<RuleMetadataEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (severity != null && !severity.isBlank()) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (language != null && !language.isBlank()) {
                predicates.add(cb.equal(root.get("language"), language));
            }
            if (engine != null && !engine.isBlank()) {
                predicates.add(cb.equal(root.get("engine"), engine));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        var p = ruleRepo.findAll(spec, pageable);
        var items = p.getContent().stream().map(this::toRuleResponse).toList();
        return PaginatedResult.of(items, p.getTotalElements(), page, size);
    }

    public RuleResponse getRule(Long id) {
        return ruleRepo.findById(id).map(this::toRuleResponse)
            .orElseThrow(() -> new NotFoundException("Rule not found: " + id));
    }

    @Transactional
    public RuleResponse updateRule(Long id, RuleUpdateRequest req) {
        var entity = ruleRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Rule not found: " + id));
        if (req.getEnabled() != null) entity.setEnabled(req.getEnabled());
        if (req.getSeverity() != null) entity.setSeverity(req.getSeverity());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        entity = ruleRepo.save(entity);
        return toRuleResponse(entity);
    }

    @Transactional
    public int syncFromEngine() {
        // Read engine YAML rules from classpath using RuleLoader
        var rules = loadEngineRules();
        int count = 0;
        for (var ruleData : rules) {
            if (ruleRepo.existsByRuleId(ruleData.ruleId())) {
                // Update existing
                var existing = ruleRepo.findByRuleId(ruleData.ruleId()).get();
                existing.setName(ruleData.name());
                existing.setSeverity(ruleData.severity());
                existing.setCwe(ruleData.cwe());
                existing.setEnabled(ruleData.enabled());
                existing.setDetectionType(ruleData.detectionType());
                ruleRepo.save(existing);
            } else {
                // Insert new
                var entity = RuleMetadataEntity.builder()
                    .ruleId(ruleData.ruleId())
                    .name(ruleData.name())
                    .severity(ruleData.severity())
                    .cwe(ruleData.cwe())
                    .language("java")
                    .engine("self_sast")
                    .detectionType(ruleData.detectionType())
                    .description(ruleData.description())
                    .fixSuggestion(ruleData.fixSuggestion())
                    .enabled(true)
                    .build();
                ruleRepo.save(entity);
            }
            count++;
        }
        log.info("Synced {} rules from engine YAML", count);
        return count;
    }

    // ========== Project Exemptions ==========

    public List<ExemptionResponse> listExemptions(Long projectId) {
        return exemptionRepo.findByProjectId(projectId).stream()
            .map(this::toExemptionResponse)
            .toList();
    }

    @Transactional
    public ExemptionResponse addExemption(Long projectId, ExemptionRequest req) {
        if (exemptionRepo.existsByProjectIdAndRuleId(projectId, req.getRuleId())) {
            throw new BadRequestException("Exemption already exists for this project and rule");
        }
        var entity = ProjectExemptionEntity.builder()
            .projectId(projectId)
            .ruleId(req.getRuleId())
            .reason(req.getReason())
            .createdBy("system")
            .expiresAt(req.getExpiresAt())
            .build();
        entity = exemptionRepo.save(entity);
        return toExemptionResponse(entity);
    }

    @Transactional
    public void removeExemption(Long projectId, Long ruleId) {
        if (!exemptionRepo.existsByProjectIdAndRuleId(projectId, ruleId)) {
            throw new NotFoundException("Exemption not found for project " + projectId + " and rule " + ruleId);
        }
        exemptionRepo.deleteByProjectIdAndRuleId(projectId, ruleId);
    }

    // ========== Internal: get exempted rule IDs for engine filtering ==========

    public List<Long> getExemptedRuleIds(Long projectId) {
        return exemptionRepo.findByProjectId(projectId).stream()
            .map(ProjectExemptionEntity::getRuleId)
            .toList();
    }

    // ========== Mappers ==========

    private RuleResponse toRuleResponse(RuleMetadataEntity e) {
        return RuleResponse.builder()
            .id(e.getId()).ruleId(e.getRuleId()).name(e.getName())
            .severity(e.getSeverity()).cwe(e.getCwe()).language(e.getLanguage())
            .engine(e.getEngine()).detectionType(e.getDetectionType())
            .description(e.getDescription()).fixSuggestion(e.getFixSuggestion())
            .enabled(e.getEnabled()).importedAt(e.getImportedAt()).updatedAt(e.getUpdatedAt())
            .build();
    }

    private ExemptionResponse toExemptionResponse(ProjectExemptionEntity e) {
        var rule = ruleRepo.findById(e.getRuleId()).orElse(null);
        return ExemptionResponse.builder()
            .id(e.getId()).projectId(e.getProjectId()).ruleId(e.getRuleId())
            .ruleName(rule != null ? rule.getName() : null)
            .ruleSeverity(rule != null ? rule.getSeverity() : null)
            .reason(e.getReason()).createdBy(e.getCreatedBy())
            .createdAt(e.getCreatedAt()).expiresAt(e.getExpiresAt())
            .build();
    }

    // ========== Engine YAML Rule Loader (mirrors engine's RuleLoader) ==========

    private record RuleData(String ruleId, String name, String severity, String cwe,
                            String detectionType, String description, String fixSuggestion, boolean enabled) {}

    private List<RuleData> loadEngineRules() {
        var rules = new ArrayList<RuleData>();
        ClassLoader cl = getClass().getClassLoader();

        String[] ruleFiles = {
            "rules/java/sql-injection-001.yml",
            "rules/java/hardcoded-password-001.yml",
            "rules/java/xss-001.yml",
            "rules/java/weak-crypto-001.yml"
        };

        try {
            var yaml = new org.yaml.snakeyaml.Yaml();
            for (String resourcePath : ruleFiles) {
                try (var in = cl.getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        log.warn("Rule file not found on classpath: {}", resourcePath);
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    var data = (java.util.Map<String, Object>) yaml.load(in);
                    rules.add(new RuleData(
                        getString(data, "id"),
                        getString(data, "name"),
                        getString(data, "severity"),
                        getString(data, "cwe"),
                        getDetectionType(data),
                        getString(data, "description"),
                        getFixSuggestion(data),
                        getBoolean(data, "enabled", true)
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Failed to load engine rules from classpath", e);
        }

        return rules;
    }

    @SuppressWarnings("unchecked")
    private String getDetectionType(java.util.Map<String, Object> data) {
        var detection = (java.util.Map<String, Object>) data.get("detection");
        return detection != null ? getString(detection, "type") : "ast";
    }

    @SuppressWarnings("unchecked")
    private String getFixSuggestion(java.util.Map<String, Object> data) {
        var fix = (java.util.Map<String, Object>) data.get("fix");
        return fix != null ? getString(fix, "description") : null;
    }

    private static String getString(java.util.Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private static boolean getBoolean(java.util.Map<String, Object> map, String key, boolean defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value instanceof Boolean b ? b : defaultValue;
    }
}
