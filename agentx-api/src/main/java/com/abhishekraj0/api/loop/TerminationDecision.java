package com.abhishekraj0.api.loop;

/**
 * Result representing whether the agent loop should terminate and with what target state.
 */
public record TerminationDecision(
        boolean shouldTerminate,
        String reason,
        LoopState targetState
) {
    public static TerminationDecision continueLoop() {
        return new TerminationDecision(false, null, null);
    }

    public static TerminationDecision terminate(String reason, LoopState targetState) {
        return new TerminationDecision(true, reason, targetState);
    }
}
