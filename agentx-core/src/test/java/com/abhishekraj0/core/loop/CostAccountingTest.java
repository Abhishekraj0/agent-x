package com.abhishekraj0.core.loop;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.loop.GoalStatus;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CostAccountingTest {

    @Test
    public void testActualUsageAndEnforcement() {
        String executionId = UUID.randomUUID().toString();
        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();

        MockChatModel model = new MockChatModel();
        model.setHandler(request -> new ChatResponse(
                ChatMessage.assistant("Verification complete"),
                new TokenUsage(100, 50, 150),
                "STOP"
        ));

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .goalEvaluator(state -> {
                    if (state.history().stream().anyMatch(msg -> msg.content() != null && msg.content().contains("Verification complete"))) {
                        return GoalStatus.COMPLETE;
                    }
                    return GoalStatus.IN_PROGRESS;
                })
                .build();

        // Configure budget and enforcement policy in options
        AgentOptions options = new AgentOptions(
                5,
                5,
                Duration.ofSeconds(30),
                0.7,
                Map.of(
                        "tokenBudget", 250,
                        "costBudget", 0.01,
                        "budgetEnforcementPolicy", "ACTUAL_ONLY"
                )
        );

        AgentRequest request = new AgentRequest("Goal description", executionId, options);
        AgentResponse response = agent.run(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.state().status());

        // Verify actual token usage is recorded in state variables
        Map<String, Object> variables = response.state().variables();
        assertEquals(150, ((Number) variables.get("accumulatedTokens")).intValue());
        assertFalse((Boolean) variables.get("isEstimatedUsage"));
        assertTrue(((Number) variables.get("accumulatedCost")).doubleValue() > 0);
    }

    @Test
    public void testEstimatedUsageAndEnforcement() {
        String executionId = UUID.randomUUID().toString();
        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();

        MockChatModel model = new MockChatModel();
        // Return null/zero token usage
        model.setHandler(request -> new ChatResponse(
                ChatMessage.assistant("Verification complete"),
                null,
                "STOP"
        ));

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .goalEvaluator(state -> {
                    if (state.history().stream().anyMatch(msg -> msg.content() != null && msg.content().contains("Verification complete"))) {
                        return GoalStatus.COMPLETE;
                    }
                    return GoalStatus.IN_PROGRESS;
                })
                .build();

        AgentOptions options = new AgentOptions(
                5,
                5,
                Duration.ofSeconds(30),
                0.7,
                Map.of(
                        "tokenBudget", 600,
                        "costBudget", 0.02,
                        "budgetEnforcementPolicy", "ESTIMATED_ONLY"
                )
        );

        AgentRequest request = new AgentRequest("Goal description", executionId, options);
        AgentResponse response = agent.run(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.state().status());

        // Verify estimated usage is recorded and flagged
        Map<String, Object> variables = response.state().variables();
        assertEquals(500, ((Number) variables.get("estimatedTokens")).intValue());
        assertTrue((Boolean) variables.get("isEstimatedUsage"));
        assertTrue(((Number) variables.get("estimatedCost")).doubleValue() > 0);
    }

    @Test
    public void testBudgetExceededACTUAL_ONLY() {
        String executionId = UUID.randomUUID().toString();
        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();

        MockChatModel model = new MockChatModel();
        model.setHandler(request -> new ChatResponse(
                ChatMessage.assistant("Deciding"),
                new TokenUsage(100, 50, 150),
                "STOP"
        ));

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .goalEvaluator(state -> GoalStatus.IN_PROGRESS) // keeps running to hit budget
                .build();

        // Token budget set to 100, actual usage is 150
        AgentOptions options = new AgentOptions(
                5,
                5,
                Duration.ofSeconds(30),
                0.7,
                Map.of(
                        "tokenBudget", 100,
                        "budgetEnforcementPolicy", "ACTUAL_ONLY"
                )
        );

        AgentRequest request = new AgentRequest("Goal description", executionId, options);
        AgentResponse response = agent.run(request);

        assertNotNull(response);
        assertEquals("FAILED", response.state().status());
        assertTrue(response.output().contains("Token budget exceeded"));
    }

    @Test
    public void testBudgetExceededESTIMATED_ONLY() {
        String executionId = UUID.randomUUID().toString();
        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();

        MockChatModel model = new MockChatModel();
        model.setHandler(request -> new ChatResponse(
                ChatMessage.assistant("Deciding"),
                null, // Force estimated usage
                "STOP"
        ));

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .goalEvaluator(state -> GoalStatus.IN_PROGRESS)
                .build();

        // Token budget set to 400, estimated is 500
        AgentOptions options = new AgentOptions(
                5,
                5,
                Duration.ofSeconds(30),
                0.7,
                Map.of(
                        "tokenBudget", 400,
                        "budgetEnforcementPolicy", "ESTIMATED_ONLY"
                )
        );

        AgentRequest request = new AgentRequest("Goal description", executionId, options);
        AgentResponse response = agent.run(request);

        assertNotNull(response);
        assertEquals("FAILED", response.state().status());
        assertTrue(response.output().contains("Token budget exceeded"));
    }
}
