package com.codesec.domain.service.auth;

import com.codesec.common.exception.BadRequestException;
import com.codesec.domain.entity.PermissionEntity;
import com.codesec.domain.entity.RoleEntity;
import com.codesec.domain.entity.UserEntity;
import com.codesec.domain.entity.UserRoleEntity;
import com.codesec.domain.repository.PermissionRepository;
import com.codesec.domain.repository.RolePermissionRepository;
import com.codesec.domain.repository.RoleRepository;
import com.codesec.domain.repository.UserRepository;
import com.codesec.domain.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Encapsulates authentication and dynamic permission resolution.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final UserRoleRepository userRoleRepo;
    private final RoleRepository roleRepo;
    private final RolePermissionRepository rolePermissionRepo;
    private final PermissionRepository permRepo;

    /**
     * Authenticates a user and resolves their dynamic permission list.
     *
     * @return a map containing {@code user} entity and {@code permissions} list
     * @throws BadRequestException if credentials are invalid
     */
    public LoginResult login(String username, String password) {
        UserEntity user = userRepo.findByUsername(username)
            .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Invalid credentials");
        }

        // 1. Find user's role assignments
        List<UserRoleEntity> userRoles = userRoleRepo.findByUserId(user.getId());

        // 2. Collect unique role IDs
        List<Long> roleIds = userRoles.stream()
            .map(UserRoleEntity::getRoleId)
            .distinct()
            .collect(Collectors.toList());

        // 3. Collect unique permission IDs for those roles
        List<Long> permissionIds = rolePermissionRepo.findByRoleIdIn(roleIds)
            .stream()
            .map(rp -> rp.getPermissionId())
            .distinct()
            .collect(Collectors.toList());

        // 4. Resolve permission names
        List<String> permissions = permRepo.findAllById(permissionIds)
            .stream()
            .map(PermissionEntity::getName)
            .collect(Collectors.toList());

        // 5. Determine role name (use first role's name, fallback to empty)
        String roleName = "";
        if (!roleIds.isEmpty()) {
            roleName = roleRepo.findById(roleIds.get(0))
                .map(RoleEntity::getName)
                .orElse("");
        }

        return new LoginResult(user, permissions, roleName);
    }

    /**
     * Result of a successful login.
     */
    public record LoginResult(UserEntity user, List<String> permissions, String roleName) {}
}
