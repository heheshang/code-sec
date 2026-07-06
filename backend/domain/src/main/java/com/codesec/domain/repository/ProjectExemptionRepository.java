package com.codesec.domain.repository;

import com.codesec.domain.entity.ProjectExemptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectExemptionRepository extends JpaRepository<ProjectExemptionEntity, Long> {
    List<ProjectExemptionEntity> findByProjectId(Long projectId);
    Optional<ProjectExemptionEntity> findByProjectIdAndRuleId(Long projectId, Long ruleId);
    boolean existsByProjectIdAndRuleId(Long projectId, Long ruleId);
    void deleteByProjectIdAndRuleId(Long projectId, Long ruleId);

}
