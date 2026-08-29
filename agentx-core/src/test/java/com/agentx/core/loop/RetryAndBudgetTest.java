package com.agentx.core.loop;

import static org.junit.jupiter.api.Assertions.*;

import com.agentx.api.agent.AgentOptions;
import com.agentx.api.agent.AgentRequest;
import com.agentx.api.context.TokenBudget;
import com.agentx.api.loop.FailureContext;
import com.agentx.api.loop.RetryDecision;
import com.agentx.api.model.TokenUsage;
import com.agentx.core.context.DefaultTokenBudgetManager;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class RetryAndBudgetTest {

    @Test
    public void testSimpleRetryStrategy() {
        SimpleRetryStrategy strategy = new SimpleRetryStrategy(3, Duration.ofMillis(10));

        // Attempt 1: should retry
        FailureContext fc1 = new FailureContext("ex-1", new RuntimeException("test error"), 1, "MODEL_CALL");
        RetryDecision decision1 = strategy.onFailure(fc1);
        assertTrue(decision1.shouldRetry());
        assertEquals(Duration.ofMillis(10), decision1.delay());

        // Attempt 3: should stop retrying
        FailureContext fc3 = new FailureContext("ex-1", new RuntimeException("test error"), 3, "MODEL_CALL");
        RetryDecision decision3 = strategy.onFailure(fc3);
        assertFalse(decision3.shouldRetry());
        assertTrue(decision3.reason().contains("Max attempts exceeded"));
    }

    @Test
    public void testTokenBudgetManager() {
        DefaultTokenBudgetManager manager = new DefaultTokenBudgetManager(100);

        // Allocate budget with no special request options
        AgentRequest request1 = new AgentRequest("hello");
        TokenBudget budget1 = manager.allocate(request1);
        assertEquals(100, budget1.maxTokens());
        assertEquals(0, budget1.consumedTokens());
        assertEquals(100, budget1.remainingTokens());
        assertTrue(manager.canCallModel(budget1));

        // Allocate budget with custom maxTokens option
        AgentRequest requestCustom = new AgentRequest("ex-1", null, new AgentOptions(10, 20, Duration.ofMinutes(5), 0.7, Map.of("maxTokens", 80)));
        TokenBudget budgetCustom = manager.allocate(requestCustom);
        assertEquals(80, budgetCustom.maxTokens());
        assertEquals(0, budgetCustom.consumedTokens());
        assertEquals(80, budgetCustom.remainingTokens());

        // Consume tokens
        manager.consume(new TokenUsage(20, 30, 50));
        assertEquals(50, manager.totalConsumed());

        // Allocate again, remaining should decrease
        TokenBudget budget2 = manager.allocate(request1);
        assertEquals(100, budget2.maxTokens());
        assertEquals(50, budget2.consumedTokens());
        assertEquals(50, budget2.remainingTokens());
        assertTrue(manager.canCallModel(budget2));

        // Consume to exceed budget
        manager.consume(new TokenUsage(30, 30, 60));
        assertEquals(110, manager.totalConsumed());

        TokenBudget budget3 = manager.allocate(request1);
        assertEquals(100, budget3.maxTokens());
        assertEquals(110, budget3.consumedTokens());
        assertEquals(0, budget3.remainingTokens());
        assertTrue(budget3.isExceeded());
        assertFalse(manager.canCallModel(budget3));
    }
}
