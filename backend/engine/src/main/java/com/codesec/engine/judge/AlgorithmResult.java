package com.codesec.engine.judge;

/**
 * Sealed hierarchy representing the result of an exploitability algorithm.
 * <p>
 * Each result carries a human-readable reason string (Chinese) suitable for
 * populating {@code Finding.exploitReason}.
 */
public sealed interface AlgorithmResult
    permits AlgorithmResult.Yes, AlgorithmResult.No, AlgorithmResult.Undetermined {

    /** The verdict: the user-controllable input was confirmed. */
    record Yes(String reason) implements AlgorithmResult {}

    /** The verdict: the user-controllable input was definitively ruled out. */
    record No(String reason) implements AlgorithmResult {}

    /** The verdict: the algorithm could not reach a conclusion. */
    record Undetermined(String reason) implements AlgorithmResult {}

    /** Factory for a positive verdict. */
    static Yes yes(String reason) {
        return new Yes(reason);
    }

    /** Factory for a negative verdict. */
    static No no(String reason) {
        return new No(reason);
    }

    /** Factory for an undetermined verdict with a specific reason. */
    static Undetermined undetermined(String reason) {
        return new Undetermined(reason);
    }

    /** Factory for an undetermined verdict with a default reason. */
    static Undetermined undetermined() {
        return new Undetermined("无法确定");
    }
}
