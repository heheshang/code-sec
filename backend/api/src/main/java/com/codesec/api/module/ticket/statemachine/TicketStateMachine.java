package com.codesec.api.module.ticket.statemachine;

import java.util.Map;
import java.util.Set;

/**
 * 8-state transition whitelist (WAIVED deferred to Sprint 3 per OP-3).
 * Checks only if a transition is syntactically allowed; RBAC is enforced at controller level.
 */
public class TicketStateMachine {
    private static final Map<String, Set<String>> ALLOWED = Map.ofEntries(
        Map.entry("pending_scan", Set.of("pending_audit")),
        Map.entry("pending_audit", Set.of("confirmed", "false_positive", "pending_retest")),
        Map.entry("confirmed", Set.of("pending_fix")),
        Map.entry("false_positive", Set.of()),          // terminal
        Map.entry("pending_fix", Set.of("fixing")),
        Map.entry("fixing", Set.of("pending_retest", "closed")),
        Map.entry("pending_retest", Set.of("closed", "confirmed")),
        Map.entry("closed", Set.of())                   // terminal
    );

    public static boolean isValid(String from, String to) {
        if (from == null || to == null) return false;
        Set<String> targets = ALLOWED.get(from.toLowerCase());
        return targets != null && targets.contains(to.toLowerCase());
    }

    public static void assertValid(String from, String to, String actorRole) {
        if (!isValid(from, to)) {
            throw new IllegalStateTransitionException(
                "Illegal transition: " + from + " → " + to);
        }
    }
}
