package com.codesec.api.application.event;

import com.codesec.engine.model.Finding;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.List;

/**
 * Published synchronously by VulnService.persistBatch() after findings are saved to MySQL.
 * E-S2-001 ES Indexer consumes this via @EventListener within the same process.
 * Sprint 3 upgrade path: replace with RabbitMQ message.
 */
@Getter
public class VulnIndexedEvent extends ApplicationEvent {
    private final List<Finding> findings;

    public VulnIndexedEvent(Object source, List<Finding> findings) {
        super(source);
        this.findings = findings;
    }
}
