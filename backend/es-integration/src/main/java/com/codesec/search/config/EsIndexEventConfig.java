package com.codesec.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Event configuration for ES indexing.
 * Uses synchronous ApplicationEventMulticaster (default Spring behavior).
 * No RabbitMQ / Kafka — per E-S2-CRITICAL § 3.8 方案 B.
 *
 * The @EventListener methods run in the calling thread (synchronous by default),
 * so ES indexing failures propagate back and trigger transaction rollback.
 */
@Configuration
public class EsIndexEventConfig {

    /**
     * Explicit synchronous multicaster.
     * Spring's default SimpleApplicationEventMulticaster is already synchronous
     * when no TaskExecutor is set — this bean makes the intent explicit.
     */
    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        // No TaskExecutor set = synchronous execution (calling thread)
        return multicaster;
    }
}
