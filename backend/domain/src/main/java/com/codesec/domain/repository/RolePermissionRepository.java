package com.codesec.domain.repository;

import com.codesec.domain.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, Long> {
    List<RolePermissionEntity> findByRoleIdIn(List<Long> roleIds);
}
