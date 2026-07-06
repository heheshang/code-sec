package com.codesec.api.config;

import com.codesec.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds test data for the "test" profile.
 * Uses native SQL INSERT to bypass Hibernate 7's rejection of
 * explicit IDs on IDENTITY-strategy entities (both persist and merge fail).
 */
@Component
@Profile("test")
public class TestDataInitializer implements CommandLineRunner {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @PersistenceContext
    private EntityManager em;

    public TestDataInitializer(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        String now = "NOW()";

        // Create roles
        em.createNativeQuery("INSERT INTO role (id, name, created_at) VALUES (1, 'SUPER_ADMIN', " + now + ")").executeUpdate();
        em.createNativeQuery("INSERT INTO role (id, name, created_at) VALUES (2, 'SECURITY_AUDITOR', " + now + ")").executeUpdate();
        em.createNativeQuery("INSERT INTO role (id, name, created_at) VALUES (3, 'PROJECT_OWNER', " + now + ")").executeUpdate();
        em.createNativeQuery("INSERT INTO role (id, name, created_at) VALUES (4, 'DEVELOPER', " + now + ")").executeUpdate();
        em.createNativeQuery("INSERT INTO role (id, name, created_at) VALUES (5, 'READONLY_VIEWER', " + now + ")").executeUpdate();

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
            {"report:read","report","read"},{"webhook:receive","webhook","receive"},{"internal:vuln-index","internal","vuln-index"},
            {"admin:crypto","admin","crypto"},{"admin:benchmark","admin","benchmark"},
            {"dashboard:read","dashboard","read"},{"cpg:read","cpg","read"}
        };
        for (int i = 0; i < perms.length; i++) {
            em.createNativeQuery("INSERT INTO permission (id, name, resource, action) VALUES (" + (i + 1) + ", '" + perms[i][0] + "', '" + perms[i][1] + "', '" + perms[i][2] + "')")
                .executeUpdate();
        }

        // Assign SUPER_ADMIN (role_id=1) -> all 30 permissions
        for (long pid = 1; pid <= 30; pid++) {
            em.createNativeQuery("INSERT INTO role_permission (role_id, permission_id) VALUES (1, " + pid + ")").executeUpdate();
        }

        // Create admin user
        String hash = encoder.encode("admin123");
        em.createNativeQuery("INSERT INTO \"user\" (id, username, email, password_hash, status, created_at, updated_at) VALUES (1, 'admin', 'admin@codesec.io', '" + hash + "', 'active', " + now + ", " + now + ")")
            .executeUpdate();

        // Assign admin -> SUPER_ADMIN
        em.createNativeQuery("INSERT INTO user_role (user_id, role_id, granted_at, granted_by) VALUES (1, 1, " + now + ", 1)").executeUpdate();
    }
}
