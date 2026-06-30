package com.codesec.engine.judge;

import com.codesec.engine.model.Finding;

/**
 * Common interface for the three exploitability-judging algorithms.
 * Uses sealed interface for exhaustive pattern matching.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link ReachableAnalyzer} — code reachability from HTTP entry points</li>
 *   <li>{@link InputControllabilityAnalyzer} — user-controllable input detection</li>
 *   <li>{@link FrameworkProtectionDetector} — framework-level protection detection</li>
 * </ul>
 */
public sealed interface Algorithm
    permits ReachableAnalyzer, InputControllabilityAnalyzer, FrameworkProtectionDetector {

    /**
     * Returns the human-readable name of this algorithm (e.g. {@code "reachable"}).
     */
    String name();

    /**
     * Classifies a single {@link Finding} and returns an {@link AlgorithmResult}.
     *
     * @param finding the SAST finding to classify
     * @return the classification result
     */
    AlgorithmResult classify(Finding finding);
}
