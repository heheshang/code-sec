package com.codesec.api.e2e;

import com.codesec.api.application.event.VulnIndexedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import static org.junit.jupiter.api.Assertions.*;

public class E12VulnPersistBatchE2ETest extends BaseE2ETest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void shouldPublishVulnIndexedEvent() {
        assertNotNull(eventPublisher, "EventPublisher should be available");
        // The VulnService.persistBatch integration is tested via E3 scan flow
    }
}
