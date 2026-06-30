package com.codesec.engine.config;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration for {@link com.codesec.engine.judge.ExploitabilityJudger}.
 *
 * <p>Default values are tuned for a single-machine scan of a mid-size Java
 * project (50k-100k LOC). Adjust {@code perFileTimeout} and
 * {@code algorithmToggles} for larger codebases or when a particular algorithm
 * is known to be noisy or slow.</p>
 *
 * <p>Loadable from system properties:
 * <ul>
 *   <li>{@code -Djudge.enabled=true|false}</li>
 *   <li>{@code -Djudge.timeout=5s}</li>
 * </ul>
 * </p>
 */
public record JudgeConfig(
    boolean enabled,
    Duration perFileTimeout,
    Map<String, String> excludePaths,
    Map<String, Boolean> algorithmToggles
) {

    /** Returns the default configuration suitable for general use. */
    public static JudgeConfig defaults() {
        return new JudgeConfig(
            true,
            Duration.ofSeconds(5),
            Map.of(
                "test/**", "true",
                "mock/**", "true",
                "generated/**", "true",
                "example/**", "true",
                "fixture/**", "true"
            ),
            Map.of(
                "reachable", true,
                "userControllable", true,
                "frameworkProtection", true
            )
        );
    }

    /**
     * Reads configuration from system properties.
     *
     * @return a {@code JudgeConfig} populated from system properties, falling
     *         back to {@link #defaults()} for missing keys
     */
    public static JudgeConfig fromSystemProperties() {
        boolean enabled = Boolean.parseBoolean(
            System.getProperty("judge.enabled", "true"));
        Duration perFileTimeout = parseDuration(
            System.getProperty("judge.timeout", "5s"));
        Map<String, Boolean> toggles = Map.of(
            "reachable", Boolean.parseBoolean(
                System.getProperty("judge.reachable", "true")),
            "userControllable", Boolean.parseBoolean(
                System.getProperty("judge.userControllable", "true")),
            "frameworkProtection", Boolean.parseBoolean(
                System.getProperty("judge.frameworkProtection", "true"))
        );
        return new JudgeConfig(enabled, perFileTimeout, defaults().excludePaths(), toggles);
    }

    /**
     * Parses a human-readable duration string (e.g. {@code "5s"}, {@code "10s"})
     * into a {@link Duration}. Returns the default of 5 seconds on parse failure.
     */
    private static Duration parseDuration(String value) {
        if (value == null || value.isBlank()) {
            return Duration.ofSeconds(5);
        }
        String trimmed = value.trim().toLowerCase();
        try {
            if (trimmed.endsWith("s")) {
                long seconds = Long.parseLong(trimmed.substring(0, trimmed.length() - 1));
                return Duration.ofSeconds(seconds);
            }
            if (trimmed.endsWith("ms")) {
                long millis = Long.parseLong(trimmed.substring(0, trimmed.length() - 2));
                return Duration.ofMillis(millis);
            }
            return Duration.ofSeconds(Long.parseLong(trimmed));
        } catch (NumberFormatException e) {
            return Duration.ofSeconds(5);
        }
    }
}
