package com.codesec.api.security;

import com.codesec.common.exception.BadRequestException;
import com.codesec.domain.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        AuthService.LoginResult result = authService.login(username, password);
        String token = jwtService.issue(result.user().getId(), result.user().getUsername(),
            result.roleName(), result.permissions());

        return Map.of(
            "token", token,
            "user", Map.of(
                "id", result.user().getId(),
                "username", result.user().getUsername(),
                "email", result.user().getEmail(),
                "role", result.roleName().isEmpty() ? "READONLY_VIEWER" : result.roleName(),
                "permissions", result.permissions()
            )
        );
    }

    @GetMapping("/me")
    public UserPrincipal me() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
            return p;
        }
        throw new BadRequestException("Not authenticated");
    }
}
