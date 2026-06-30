package com.codesec.search.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enables Spring Retry for ES indexing operations.
 * EsUpsertService uses manual retry loop with exponential backoff (1s/3s/5s)
 * per E-S2-CRITICAL § 3.8 降级策略.
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Spring Retry enabled via @EnableRetry.
    // EsUpsertService uses manual retry loop for explicit control over backoff timing.
}
