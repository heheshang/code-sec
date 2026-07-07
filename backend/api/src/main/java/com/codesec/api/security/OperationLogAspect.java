package com.codesec.api.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!test")
public class OperationLogAspect {
    private final ApplicationEventPublisher eventPublisher;

    @Around("@within(org.springframework.web.bind.annotation.RestController) || " +
            "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String action = joinPoint.getSignature().getName();
        Long userId = null;

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
            userId = p.getUserId();
        }

        String ipAddress = null;
        String userAgent = null;
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            ipAddress = request.getRemoteAddr();
            userAgent = request.getHeader("User-Agent");
        }

        try {
            Object result = joinPoint.proceed();
            eventPublisher.publishEvent(new OperationLogEvent(action, userId, ipAddress, userAgent, 200));
            return result;
        } catch (Throwable e) {
            eventPublisher.publishEvent(new OperationLogEvent(action, userId, ipAddress, userAgent, 500));
            throw e;
        }
    }
}
