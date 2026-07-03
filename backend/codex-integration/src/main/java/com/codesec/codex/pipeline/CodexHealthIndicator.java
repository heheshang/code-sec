package com.codesec.codex.pipeline;

import com.codesec.codex.client.CodexClient;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;

import java.util.List;

public class CodexHealthIndicator extends AbstractHealthIndicator {

    private final List<CodexClient> clients;

    public CodexHealthIndicator(List<CodexClient> clients) {
        this.clients = clients;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean allOk = true;
        for (CodexClient client : clients) {
            com.codesec.codex.model.CodexHealth ch = client.health();
            builder.withDetail(client.type().name(), ch.isOk());
            if (!ch.isOk()) {
                allOk = false;
            }
        }
        if (allOk) {
            builder.up();
        } else {
            builder.down();
        }
    }
}
