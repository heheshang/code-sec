package com.codesec.api.domain.enums;

public enum VulnStatus {
    pending_scan,
    pending_audit,
    confirmed,
    false_positive,
    pending_fix,
    pending_retest,
    fixing,
    closed;

    public boolean isFinal() {
        return this == false_positive || this == closed;
    }
}
