package com.abhishekraj0.api.context;

import com.abhishekraj0.api.agent.AgentRequest;
import com.abhishekraj0.api.model.TokenUsage;

/**
 * Interface to manage and monitor token budgets for model invocations.
 */
public interface TokenBudgetManager {

    /**
     * Allocates a token budget for the given request.
     *
     * @param request the agent request
     * @return the allocated token budget
     */
    TokenBudget allocate(AgentRequest request);

    /**
     * Checks if a model call can be made based on the current budget status.
     *
     * @param budget the token budget
     * @return true if call is allowed, false otherwise
     */
    boolean canCallModel(TokenBudget budget);

    /**
     * Consumes tokens from the budget.
     *
     * @param usage the token usage details
     */
    void consume(TokenUsage usage);
}
