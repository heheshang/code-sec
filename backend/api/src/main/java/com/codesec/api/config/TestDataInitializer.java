package com.codesec.api.config;

import com.codesec.api.domain.entity.*;
import com.codesec.api.domain.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

/**
 * Seeds test data for the "test" profile.
 * Uses EntityManager.persist() for role_permission inserts
 * to bypass JPA merge semantics (the entity @Id is on role_id only,
 * so saveAll() would overwrite with each entity having the same roleId=1).
 */
@Component
@Profile("test")
public class TestDataInitializer implements CommandLineRunner {
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;
    private final UserRoleRepository userRoleRepo;
    private final RolePermissionRepository rolePermissionRepo;
    private final PasswordEncoder encoder;

    public TestDataInitializer(UserRepository userRepo, RoleRepository roleRepo,
                                PermissionRepository permRepo,
                                UserRoleRepository userRoleRepo,
                                RolePermissionRepository rolePermissionRepo,
                                PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.permRepo = permRepo;
        this.userRoleRepo = userRoleRepo;
        this.rolePermissionRepo = rolePermissionRepo;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        // Create roles
        for (int i = 1; i <= 5; i++) {
            String name = switch (i) {
                case 1 -> "SUPER_ADMIN";
                case 2 -> "SECURITY_AUDITOR";
                case 3 -> "PROJECT_OWNER";
                case 4 -> "DEVELOPER";
                default -> "READONLY_VIEWER";
            };
            roleRepo.save(RoleEntity.builder().id((long) i).name(name).build());
        }

        // Seed 26 permissions
        String[][] perms = {
            {"repo:create","repo","create"},{"repo:read","repo","read"},{"repo:update","repo","update"},{"repo:delete","repo","delete"},
            {"scan:create","scan","create"},{"scan:read","scan","read"},{"scan:cancel","scan","cancel"},
            {"vuln:read","vuln","read"},{"vuln:audit","vuln","audit"},{"vuln:confirm","vuln","confirm"},
            {"vuln:false_positive","vuln","false_positive"},{"vuln:need_retest","vuln","need_retest"},
            {"ticket:read","ticket","read"},{"ticket:assign","ticket","assign"},{"ticket:fix","ticket","fix"},
            {"ticket:close","ticket","close"},{"ticket:retest","ticket","retest"},{"ticket:waive","ticket","waive"},
            {"rule:read","rule","read"},{"rule:create","rule","create"},{"rule:update","rule","update"},
            {"rule:delete","rule","delete"},{"rule:gray_release","rule","gray_release"},
            {"report:read","report","read"},{"webhook:receive","webhook","receive"},{"internal:vuln-index","internal","vuln-index"}
        };
        for (int i = 0; i < perms.length; i++) {
            permRepo.save(PermissionEntity.builder().id((long)(i+1)).name(perms[i][0]).resource(perms[i][1]).action(perms[i][2]).build());
        }

        // Assign SUPER_ADMIN (role_id=1) -> all 26 permissions
        // With auto-generated ID on RolePermissionEntity, saveAll() works correctly
        List<RolePermissionEntity> rpList = LongStream.rangeClosed(1, 26)
            .mapToObj(pid -> RolePermissionEntity.builder()
                .roleId(1L).permissionId(pid).build())
            .toList();
        rolePermissionRepo.saveAll(rpList);

        // Create admin user
        userRepo.save(UserEntity.builder()
            .id(1L).username("admin").email("admin@codesec.io")
            .passwordHash(encoder.encode("admin123"))
            .status("active").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build());

        // Assign admin -> SUPER_ADMIN
        userRoleRepo.save(UserRoleEntity.builder().userId(1L).roleId(1L)
            .grantedAt(LocalDateTime.now()).grantedBy(1L).build());
    }
}
