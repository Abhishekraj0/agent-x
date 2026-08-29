package com.abhishekraj0.core.agent;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.loop.*;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.planner.*;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.api.context.AgentContext;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.model.mock.MockChatModel;
import com.abhishekraj0.core.tool.DefaultToolRegistry;
import com.abhishekraj0.core.tool.FunctionTool;
import com.abhishekraj0.core.loop.DefaultGoalEvaluator;
import com.abhishekraj0.core.loop.DefaultExecutionEngine;
import com.abhishekraj0.core.loop.DefaultLoopController;
import com.abhishekraj0.core.loop.DefaultObservationHandler;
import com.abhishekraj0.core.loop.DefaultStateUpdater;
import com.abhishekraj0.core.loop.DefaultAgentLoop;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class AutonomousAgentLoopValidationTest {

    @Test
    public void testGoalEvaluationSemanticCheck() {
        // GoalEvaluator predicate: required tool action must be performed (toolCalls > 0)
        GoalEvaluator customEvaluator = new DefaultGoalEvaluator(state -> state.toolCalls() > 0);

        MockChatModel model = new MockChatModel();
        // Model immediately says "done" without executing tools
        model.setHandler(request -> new ChatResponse(
                ChatMessage.assistant("The task is done!"),
                new TokenUsage(5, 5, 10),
                "STOP"
        ));

        Agent agent = AgentX.builder()
                .model(model)
                .goalEvaluator(customEvaluator)
                .build();

        // Run the agent with max iterations limit to prevent infinite loops since goal evaluator won't complete
        AgentOptions options = new AgentOptions(3, 10, Duration.ofSeconds(5), 0.7, Map.of());
        AgentRequest request = new AgentRequest("Run task", java.util.UUID.randomUUID().toString(), options);

        AgentResponse response = agent.run(request);

        // Verification: The loop did NOT complete as COMPLETED because toolCalls is 0, failing the semantic check.
        // It terminated with Max Iterations Reached (FAILED) instead of COMPLETED.
        assertEquals("FAILED", response.state().status());
        assertEquals(0, response.state().toolCalls());
    }

    @Test
    public void testNoInfiniteLoopOnMaxIterations() {
        MockChatModel model = new MockChatModel();
        // Model keeps requesting tools in an infinite loop
        model.setHandler(request -> {
            ToolCall call = new ToolCall("call-inf", "some-tool", "{}");
            return new ChatResponse(
                    ChatMessage.assistant(null, List.of(call)),
                    new TokenUsage(5, 5, 10),
                    "TOOL_USE"
            );
        });

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("some-tool"),
                "dummy tool",
                ToolSchema.empty(),
                context -> ToolResult.success("ok")
        ));

        Agent agent = AgentX.builder()
                .model(model)
                .tools(toolRegistry)
                .build();

        // Set maxIterations to 3
        AgentOptions options = new AgentOptions(3, 10, Duration.ofSeconds(5), 0.7, Map.of());
        AgentRequest request = new AgentRequest("Infinite loop task", java.util.UUID.randomUUID().toString(), options);

        AgentResponse response = agent.run(request);

        // The agent must terminate at exactly 3 iterations and report FAILED
        assertEquals("FAILED", response.state().status());
        assertEquals(3, response.state().iterations());
    }

    @Test
    public void testDynamicReplanningScenario() {
        MockChatModel model = new MockChatModel();
        model.setHandler(request -> {
            int iterations = request.messages().size() / 2;
            if (iterations == 0) {
                // First turn: invoke database query tool
                ToolCall call = new ToolCall("db-call", "query-payments", "{}");
                return new ChatResponse(
                        ChatMessage.assistant(null, List.of(call)),
                        new TokenUsage(5, 5, 10),
                        "TOOL_USE"
                );
            } else if (iterations == 1) {
                return new ChatResponse(
                        ChatMessage.assistant("Database unavailable. Replanning to use fallback CSV cache."),
                        new TokenUsage(5, 5, 10),
                        "STOP"
                );
            }
            return new ChatResponse(
                    ChatMessage.assistant("Report created from CSV cache."),
                    new TokenUsage(5, 5, 10),
                    "STOP"
            );
        });

        // Let's create a custom action selector that returns a ReplanDecision when it sees "Database unavailable"
        ActionSelector replanActionSelector = new ActionSelector() {
            private boolean replanned = false;

            @Override
            public AgentDecision select(AgentContext context, List<AgentTool> tools) {
                if (replanned) {
                    return new FinalResponseDecision("final-123", "Completed successfully with CSV fallback", "Report created from CSV cache.");
                }
                List<ChatMessage> history = context.messages();
                if (!history.isEmpty()) {
                    ChatMessage lastMsg = history.get(history.size() - 1);
                    if (lastMsg.content() != null && lastMsg.content().contains("Database unavailable")) {
                        replanned = true;
                        return new ReplanDecision("replan-123", "Database down", "Use fallback CSV cache");
                    }
                    if (lastMsg.content() != null && lastMsg.content().contains("CSV cache")) {
                        return new FinalResponseDecision("final-123", "Completed successfully with CSV fallback", "Report created from CSV cache.");
                    }
                    // If last message is a TOOL role and has error
                    boolean hasDbError = history.stream().anyMatch(msg -> msg.role() == ChatMessageRole.TOOL && msg.content() != null && msg.content().contains("Database Connection Failed"));
                    if (hasDbError) {
                        replanned = true;
                        return new ReplanDecision("replan-123", "Database down", "Use fallback CSV cache");
                    }
                }
                ToolCall call = new ToolCall("db-call", "query-payments", "{}");
                return new ToolCallDecision("db-call-decision", "Querying payments", List.of(call));
            }
        };

        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new FunctionTool(
                new ToolId("query-payments"),
                "database query tool",
                ToolSchema.empty(),
                context -> ToolResult.failure("TRANSIENT", "Database Connection Failed", new RuntimeException("DB down"))
        ));

        // Use custom loop controller with our ActionSelector and GoalEvaluator
        Agent agent = new Agent() {
            @Override
            public AgentResponse run(AgentRequest req) {
                DefaultExecutionEngine engine = new DefaultExecutionEngine(model, toolRegistry, List.of(), null, null, null);
                DefaultLoopController controller = new DefaultLoopController(
                        replanActionSelector,
                        new DefaultGoalEvaluator(),
                        new DefaultObservationHandler(),
                        new DefaultStateUpdater(),
                        new com.abhishekraj0.core.context.SimpleContextManager(),
                        toolRegistry,
                        List.of(),
                        null,
                        null,
                        null
                );
                DefaultAgentLoop loop = new DefaultAgentLoop(req, engine, new com.abhishekraj0.core.context.SimpleContextManager(), null, null) {
                    @Override
                    public AgentResponse execute(AgentState state) {
                        LoopResult result = controller.run(req, state);
                        return new AgentResponse(result.output(), result.finalState(), List.of());
                    }
                };
                return new DefaultAgentRuntime(loop).execute(req);
            }

            @Override
            public AgentResponse run(String input) {
                return run(new AgentRequest(input));
            }

            @Override
            public void reset() {}

            @Override
            public AgentState state() {
                return null;
            }
        };

        AgentResponse response = agent.run("Create payment report");

        // The agent should detect DB error, trigger the ReplanDecision, transition through LoopState.REPLANNING,
        // update the Plan inside AgentState, and complete using the fallback CSV cache step.
        assertNotNull(response.state().plan());
        assertEquals("replan-step-1", response.state().plan().steps().get(0).stepId());
        assertEquals("Use fallback CSV cache", response.state().plan().steps().get(0).description());
        assertEquals("COMPLETED", response.state().status());
    }
}
