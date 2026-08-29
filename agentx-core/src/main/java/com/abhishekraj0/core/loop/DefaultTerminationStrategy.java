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

        if ("COMPLETED".equals(state.status())) {
            return TerminationDecision.continueLoop(); // Handled by evaluator
        }

        if ("FAILED".equals(state.status()) || "TIMEOUT".equals(state.status()) || "CANCELLED".equals(state.status())) {
            return TerminationDecision.continueLoop(); // Handled by evaluator
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
            Object costLimitObj = state.variables().get("costBudget");
            Object currentCostObj = state.variables().get("accumulatedCost");
            if (costLimitObj instanceof Number && currentCostObj instanceof Number) {
                double costLimit = ((Number) costLimitObj).doubleValue();
                double currentCost = ((Number) currentCostObj).doubleValue();
                if (currentCost >= costLimit) {
                    return TerminationDecision.terminate("Cost budget exceeded: " + currentCost + " >= " + costLimit, LoopState.FAILED);
                }
            }

            Object tokenLimitObj = state.variables().get("tokenBudget");
            Object currentTokensObj = state.variables().get("accumulatedTokens");
            if (tokenLimitObj instanceof Number && currentTokensObj instanceof Number) {
                int tokenLimit = ((Number) tokenLimitObj).intValue();
                int currentTokens = ((Number) currentTokensObj).intValue();
                if (currentTokens >= tokenLimit) {
                    return TerminationDecision.terminate("Token budget exceeded: " + currentTokens + " >= " + tokenLimit, LoopState.FAILED);
                }
            }
        }

        return TerminationDecision.continueLoop();
    }
}
