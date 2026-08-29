package com.agentx.core.context;

import com.agentx.api.agent.AgentRequest;
import com.agentx.api.context.TokenBudget;
import com.agentx.api.context.TokenBudgetManager;
import com.agentx.api.model.TokenUsage;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of TokenBudgetManager that tracks global consumed tokens.
 */
public class DefaultTokenBudgetManager implements TokenBudgetManager {

    private final int globalMaxTokens;
    private final AtomicInteger totalConsumed = new AtomicInteger(0);

    public DefaultTokenBudgetManager(int globalMaxTokens) {
        this.globalMaxTokens = globalMaxTokens;
    }

    @Override
    public TokenBudget allocate(AgentRequest request) {
        int max = globalMaxTokens;
        if (request.options() != null && request.options().additionalOptions() != null) {
            Object maxTokensObj = request.options().additionalOptions().get("maxTokens");
            if (maxTokensObj instanceof Number num) {
                max = Math.min(globalMaxTokens, num.intValue());
            }
        }
        int consumed = totalConsumed.get();
        int remaining = Math.max(0, max - consumed);
        return new TokenBudget(max, consumed, remaining);
    }

    @Override
    public boolean canCallModel(TokenBudget budget) {
        return budget != null && !budget.isExceeded() && totalConsumed.get() < budget.maxTokens();
    }

    @Override
    public void consume(TokenUsage usage) {
        if (usage != null) {
            totalConsumed.addAndGet(usage.totalTokens());
        }
    }

    public int totalConsumed() {
        return totalConsumed.get();
    }
}
