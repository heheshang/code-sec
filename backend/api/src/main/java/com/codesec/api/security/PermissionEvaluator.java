package com.codesec.api.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("perm")
public class PermissionEvaluator {
    public boolean check(String permission) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) return false;
        return ((UserPrincipal) auth.getPrincipal()).hasPermission(permission);
    }
}
