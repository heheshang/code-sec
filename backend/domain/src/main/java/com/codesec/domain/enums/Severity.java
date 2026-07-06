package com.codesec.domain.enums;

public enum Severity {
    critical,
    high,
    medium,
    low,
    info;

    public static Severity fromString(String value) {
        if (value == null) return info;
        return switch (value.toLowerCase()) {
            case "critical" -> critical;
            case "high" -> high;
            case "medium" -> medium;
            case "low" -> low;
            default -> info;
        };
    }
}
