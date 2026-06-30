package com.codesec.api.domain.repository;

import com.codesec.api.domain.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {
    @Query("SELECT p.name FROM PermissionEntity p JOIN RolePermissionEntity rp ON p.id = rp.permissionId WHERE rp.roleId = ?1")
    List<String> findPermissionNamesByRoleId(Long roleId);
}
