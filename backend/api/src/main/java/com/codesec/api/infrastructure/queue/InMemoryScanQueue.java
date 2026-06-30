package com.codesec.api.infrastructure.queue;

import com.codesec.api.domain.entity.ScanTaskEntity;
import org.springframework.stereotype.Component;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class InMemoryScanQueue {
    private final BlockingQueue<ScanTaskEntity> queue = new LinkedBlockingQueue<>(100);

    public void enqueue(ScanTaskEntity task) {
        try {
            queue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to enqueue scan task", e);
        }
    }

    public ScanTaskEntity dequeue() throws InterruptedException {
        return queue.take();
    }
}
