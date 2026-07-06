package com.codesec.domain.enums;

public enum ScanStatus {
    queued,
    running,
    completed,
    failed,
    canceled;

    public boolean isTerminal() {
        return this == completed || this == failed || this == canceled;
    }
}
