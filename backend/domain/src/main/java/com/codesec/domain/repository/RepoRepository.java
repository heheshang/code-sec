package com.codesec.domain.repository;

import com.codesec.domain.entity.RepoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface RepoRepository extends JpaRepository<RepoEntity, Long>, JpaSpecificationExecutor<RepoEntity> {
    Optional<RepoEntity> findByGitlabProjectId(Long gitlabProjectId);
}
