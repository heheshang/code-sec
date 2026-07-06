package com.codesec.api.security;

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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final UserRoleRepository userRoleRepo;
    private final RoleRepository roleRepo;
    private final RolePermissionRepository rolePermissionRepo;
    private final PermissionRepository permRepo;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        UserEntity user = userRepo.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        // --- Dynamic permission resolution ---
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

        String token = jwtService.issue(user.getId(), user.getUsername(), roleName, permissions);

        return Map.of(
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", roleName.isEmpty() ? "READONLY_VIEWER" : roleName,
                "permissions", permissions
            )
        );
    }

    @GetMapping("/me")
    public UserPrincipal me() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
            return p;
        }
        throw new RuntimeException("Not authenticated");
    }
}
