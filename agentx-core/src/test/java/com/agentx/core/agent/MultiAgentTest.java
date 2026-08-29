package com.agentx.core.agent;

import static org.junit.jupiter.api.Assertions.*;

import com.agentx.api.agent.*;
import com.agentx.api.tool.ToolContext;
import com.agentx.api.tool.ToolResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class MultiAgentTest {

    @Test
    public void testCoordinatorAndDelegationTool() {
        DefaultAgentRegistry registry = new DefaultAgentRegistry();

        // Register a mock researcher agent
        Agent researcher = new Agent() {
            @Override
            public AgentResponse run(AgentRequest request) {
                AgentState state = new AgentState("e1", List.of(), null, Map.of(), 0, 0, "COMPLETED");
                return new AgentResponse("Research details on " + request.input(), state, List.of());
            }

            @Override
            public AgentResponse run(String input) {
                AgentState state = new AgentState("e1", List.of(), null, Map.of(), 0, 0, "COMPLETED");
                return new AgentResponse("Research details on " + input, state, List.of());
            }

            @Override
            public void reset() {}

            @Override
            public AgentState state() {
                return null;
            }
        };
        registry.register("Researcher", researcher);

        DefaultAgentCoordinator coordinator = new DefaultAgentCoordinator(registry);

        // 1. Direct Coordinator Delegation Test
        AgentTask task = new AgentTask("task-123", "Java 21 Virtual Threads", "Researcher", Map.of());
        AgentResponse response = coordinator.delegate(task);

        assertEquals("COMPLETED", response.state().status());
        assertEquals("Research details on Java 21 Virtual Threads", response.output());

        // 2. Delegation Tool Execution Test
        DelegationTool tool = new DelegationTool(coordinator);
        ToolContext toolCtx = new ToolContext("exec-99", Map.of(
                "assignee", "Researcher",
                "taskDescription", "Spring Boot WebFlux"
        ), Map.of());

        ToolResult toolResult = tool.execute(toolCtx);
        assertTrue(toolResult.success());
        assertEquals("Research details on Spring Boot WebFlux", toolResult.output());

        // 3. Failed Delegation Test (non-existent assignee)
        ToolContext badCtx = new ToolContext("exec-99", Map.of(
                "assignee", "NonExistentAgent",
                "taskDescription", "Should fail"
        ), Map.of());
        ToolResult badResult = tool.execute(badCtx);
        assertFalse(badResult.success());
        assertEquals("DELEGATION_FAILED", badResult.error().code());
    }
}
