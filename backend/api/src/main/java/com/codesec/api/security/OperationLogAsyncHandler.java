package com.codesec.api.security;

import com.codesec.domain.entity.OperationLogEntity;
import com.codesec.domain.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OperationLogAsyncHandler {
    private final OperationLogRepository logRepo;

    @Async
    @EventListener
    public void handleOperationLog(OperationLogEvent event) {
        try {
            OperationLogEntity log = new OperationLogEntity();
            log.setAction(event.action());
            log.setResourceType("api");
            log.setResourceId(0L);
            log.setUserId(event.userId());
            log.setIpAddress(event.ipAddress());
            log.setUserAgent(event.userAgent());
            log.setResponseStatus(event.responseStatus());
            logRepo.save(log);
        } catch (Exception e) {
            log.error("Failed to persist operation log asynchronously", e);
        }
    }
}
