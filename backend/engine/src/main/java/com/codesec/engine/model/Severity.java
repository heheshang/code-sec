package com.codesec.engine.model;

public enum Severity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO;

    public static Severity fromString(String value) {
        if (value == null) {
            return INFO;
        }
        return switch (value.toLowerCase()) {
            case "critical" -> CRITICAL;
            case "high" -> HIGH;
            case "medium" -> MEDIUM;
            case "low" -> LOW;
            default -> INFO;
        };
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
