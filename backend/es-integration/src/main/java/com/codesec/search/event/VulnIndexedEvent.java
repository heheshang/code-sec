package com.codesec.search.event;

import org.springframework.context.ApplicationEvent;

import java.util.Collections;
import java.util.List;

/**
 * Event published by VulnService.persistBatch() after MySQL insert.
 * Consumed synchronously by EsIndexListener to upsert into ES.
 *
 * Per E-S2-CRITICAL § 3.8 方案 B: synchronous ApplicationEventPublisher.
 * Failure in listener → VulnIndexingFailedException → transaction rollback.
 */
public class VulnIndexedEvent extends ApplicationEvent {

    private final List<Long> findingIds;
    private final String projectId;

    public VulnIndexedEvent(Object source, List<Long> findingIds, String projectId) {
        super(source);
        this.findingIds = Collections.unmodifiableList(findingIds);
        this.projectId = projectId;
    }

    public List<Long> getFindingIds() {
        return findingIds;
    }

    public String getProjectId() {
        return projectId;
    }

    @Override
    public String toString() {
        return "VulnIndexedEvent{findingIds=" + findingIds.size() + ", projectId=" + projectId + "}";
    }
}
