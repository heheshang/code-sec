package com.codesec.api.security;

import com.codesec.api.domain.entity.OperationLogEntity;
import com.codesec.api.domain.repository.OperationLogRepository;
import com.codesec.api.domain.repository.PermissionRepository;
import com.codesec.api.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!test")
public class OperationLogAspect {
    private final OperationLogRepository logRepo;

    @Around("@within(org.springframework.web.bind.annotation.RestController) || " +
            "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        OperationLogEntity log = new OperationLogEntity();
        log.setAction(joinPoint.getSignature().getName());
        log.setResourceType("api");
        log.setResourceId(0L);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
            log.setUserId(p.getUserId());
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            log.setIpAddress(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
        }

        try {
            Object result = joinPoint.proceed();
            log.setResponseStatus(200);
            return result;
        } catch (Throwable e) {
            log.setResponseStatus(500);
            throw e;
        } finally {
            logRepo.save(log);
        }
    }
}
