package com.codesec.api.security;

import com.codesec.api.domain.entity.UserEntity;
import com.codesec.api.domain.repository.PermissionRepository;
import com.codesec.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
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

        // Default all permissions for authenticated users
        List<String> permissions = List.of(
            "repo:create","repo:read","repo:update","repo:delete",
            "scan:create","scan:read","scan:cancel",
            "vuln:read","vuln:audit","vuln:confirm","vuln:false_positive","vuln:need_retest",
            "ticket:read","ticket:assign","ticket:fix","ticket:close","ticket:retest","ticket:waive",
            "rule:read","rule:create","rule:update","rule:delete","rule:gray_release",
            "report:read","webhook:receive","internal:vuln-index"
        );

        String token = jwtService.issue(user.getId(), user.getUsername(), "SUPER_ADMIN", permissions);

        return Map.of(
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", "SUPER_ADMIN",
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
