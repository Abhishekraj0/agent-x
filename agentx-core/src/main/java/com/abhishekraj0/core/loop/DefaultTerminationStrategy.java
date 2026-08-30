package com.abhishekraj0.core.loop;

import com.abhishekraj0.api.agent.AgentOptions;
import com.abhishekraj0.api.agent.AgentState;
import com.abhishekraj0.api.loop.LoopState;
import com.abhishekraj0.api.loop.TerminationDecision;
import com.abhishekraj0.api.loop.TerminationStrategy;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Evaluates iterations, tool calls, timeouts, budgets, and cancellation to terminate the loop.
 */
public class DefaultTerminationStrategy implements TerminationStrategy {

    private final AgentOptions options;
    private final Instant startTime;
    private final Supplier<Boolean> cancellationCheck;

    public DefaultTerminationStrategy(AgentOptions options, Instant startTime, Supplier<Boolean> cancellationCheck) {
        this.options = options != null ? options : AgentOptions.defaultOptions();
        this.startTime = startTime;
        this.cancellationCheck = cancellationCheck;
    }

    @Override
    public TerminationDecision evaluate(AgentState state) {
        if (cancellationCheck != null && Boolean.TRUE.equals(cancellationCheck.get())) {
            return TerminationDecision.terminate("Execution was cancelled", LoopState.CANCELLED);
        }

        if (state.iterations() >= options.maxIterations()) {
            return TerminationDecision.terminate("Max iterations (" + options.maxIterations() + ") reached", LoopState.FAILED);
        }

        if (state.toolCalls() >= options.maxToolCalls()) {
            return TerminationDecision.terminate("Max tool calls (" + options.maxToolCalls() + ") reached", LoopState.FAILED);
        }

        if (options.timeout() != null) {
            Duration elapsed = Duration.between(startTime, Instant.now());
            if (elapsed.compareTo(options.timeout()) > 0) {
                return TerminationDecision.terminate("Execution timed out", LoopState.TIMEOUT);
            }
        }

        if (state.variables() != null) {
            String policy = "ESTIMATED_OR_ACTUAL";
            if (options.additionalOptions() != null) {
                Object p = options.additionalOptions().get("budgetEnforcementPolicy");
                if (p instanceof String s) {
                    policy = s;
                }
            }

            Object costLimitObj = state.variables().get("costBudget");
            if (costLimitObj instanceof Number) {
                double costLimit = ((Number) costLimitObj).doubleValue();
                double currentCost = ((Number) state.variables().getOrDefault("accumulatedCost", 0.0)).doubleValue();
                double estCost = ((Number) state.variables().getOrDefault("estimatedCost", 0.0)).doubleValue();

                boolean exceeded = false;
                if ("ACTUAL_ONLY".equalsIgnoreCase(policy)) {
                    exceeded = currentCost >= costLimit;
                } else if ("ESTIMATED_ONLY".equalsIgnoreCase(policy)) {
                    exceeded = estCost >= costLimit;
                } else {
                    exceeded = (currentCost + estCost) >= costLimit;
                }

                if (exceeded) {
                    return TerminationDecision.terminate("Cost budget exceeded: " + (currentCost + estCost) + " >= " + costLimit, LoopState.FAILED);
                }
            }

            Object tokenLimitObj = state.variables().get("tokenBudget");
            if (tokenLimitObj instanceof Number) {
                int tokenLimit = ((Number) tokenLimitObj).intValue();
                int currentTokens = ((Number) state.variables().getOrDefault("accumulatedTokens", 0)).intValue();
                int estTokens = ((Number) state.variables().getOrDefault("estimatedTokens", 0)).intValue();

                boolean exceeded = false;
                if ("ACTUAL_ONLY".equalsIgnoreCase(policy)) {
                    exceeded = currentTokens >= tokenLimit;
                } else if ("ESTIMATED_ONLY".equalsIgnoreCase(policy)) {
                    exceeded = estTokens >= tokenLimit;
                } else {
                    exceeded = (currentTokens + estTokens) >= tokenLimit;
                }

                if (exceeded) {
                    return TerminationDecision.terminate("Token budget exceeded: " + (currentTokens + estTokens) + " >= " + tokenLimit, LoopState.FAILED);
                }
            }
        }

        if ("COMPLETED".equals(state.status())) {
            return TerminationDecision.continueLoop(); // Handled by evaluator
        }

        if ("FAILED".equals(state.status()) || "TIMEOUT".equals(state.status()) || "CANCELLED".equals(state.status())) {
            return TerminationDecision.continueLoop(); // Handled by evaluator
        }

        return TerminationDecision.continueLoop();
    }
}
