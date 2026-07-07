package com.codesec.api.security;

public record OperationLogEvent(
    String action,
    Long userId,
    String ipAddress,
    String userAgent,
    int responseStatus
) {}
