package com.codesec.domain.repository;

import com.codesec.domain.entity.RuleMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface RuleMetadataRepository extends JpaRepository<RuleMetadataEntity, Long>, JpaSpecificationExecutor<RuleMetadataEntity> {
    Optional<RuleMetadataEntity> findByRuleId(String ruleId);
    boolean existsByRuleId(String ruleId);
}
