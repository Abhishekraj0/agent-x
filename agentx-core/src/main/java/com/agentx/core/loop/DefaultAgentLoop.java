package com.agentx.core.loop;

import com.agentx.api.agent.*;
import com.agentx.api.context.*;
import com.agentx.api.event.AgentEvent;
import com.agentx.api.loop.*;
import com.agentx.api.model.ChatMessage;
import com.agentx.api.planner.Plan;
import com.agentx.api.planner.Planner;
import com.agentx.core.event.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of the primary agent reasoning and action loop.
 */
public class DefaultAgentLoop implements AgentLoop {

    private final AgentRequest request;
    private final ExecutionEngine executionEngine;
    private final ContextManager contextManager;
    private final Planner planner;
    private final List<AgentEvent> events = new ArrayList<>();

    public DefaultAgentLoop(AgentRequest request, ExecutionEngine executionEngine, ContextManager contextManager, Planner planner) {
        this.request = request;
        this.executionEngine = executionEngine;
        this.contextManager = contextManager;
        this.planner = planner;
    }

    @Override
    public AgentResponse execute(AgentState state) {
        AgentState currentState = state;

        // Add user message to history if empty
        if (currentState.history().isEmpty()) {
            List<ChatMessage> history = new ArrayList<>();
            history.add(ChatMessage.user(request.input()));
            currentState = new AgentState(
                    currentState.executionId(),
                    history,
                    currentState.plan(),
                    currentState.variables(),
                    currentState.iterations(),
                    currentState.toolCalls(),
                    "RUNNING"
            );
        }

        AgentOptions options = request.options();
        int maxIterations = options != null ? options.maxIterations() : 10;

        // Publish start event
        publishEvent(new AgentStartedEvent(request.executionId(), Instant.now()));

        while (currentState.iterations() < maxIterations && "RUNNING".equals(currentState.status())) {

            // 1. Context Manager build context
            AgentContext context = contextManager.buildContext(request, currentState);

            // 2. Planning (if no plan yet, create one)
            if (currentState.plan() == null && planner != null) {
                Plan plan = planner.createPlan(request, context);
                currentState = new AgentState(
                        currentState.executionId(),
                        currentState.history(),
                        plan,
                        currentState.variables(),
                        currentState.iterations(),
                        currentState.toolCalls(),
                        currentState.status()
                );
                publishEvent(new PlanCreatedEvent(request.executionId(), plan, Instant.now()));
            }

            // 3. Increment iteration
            currentState = new AgentState(
                    currentState.executionId(),
                    currentState.history(),
                    currentState.plan(),
                    currentState.variables(),
                    currentState.iterations() + 1,
                    currentState.toolCalls(),
                    currentState.status()
            );

            // 4. Execution Step
            ExecutionRequest stepRequest = new ExecutionRequest(request, currentState);
            ExecutionResult stepResult = executionEngine.execute(stepRequest);

            currentState = stepResult.state();

            if (!stepResult.success()) {
                currentState = new AgentState(
                        currentState.executionId(),
                        currentState.history(),
                        currentState.plan(),
                        currentState.variables(),
                        currentState.iterations(),
                        currentState.toolCalls(),
                        "FAILED"
                    );
                publishEvent(new AgentFailedEvent(request.executionId(), stepResult.error(), Instant.now()));
                return new AgentResponse(stepResult.output(), currentState, new ArrayList<>(events));
            }

            // Check if goal completed
            if ("COMPLETED".equals(currentState.status())) {
                publishEvent(new AgentCompletedEvent(request.executionId(), stepResult.output(), Instant.now()));
                return new AgentResponse(stepResult.output(), currentState, new ArrayList<>(events));
            }
        }

        if ("RUNNING".equals(currentState.status())) {
            currentState = new AgentState(
                    currentState.executionId(),
                    currentState.history(),
                    currentState.plan(),
                    currentState.variables(),
                    currentState.iterations(),
                    currentState.toolCalls(),
                    "FAILED"
            );
            publishEvent(new AgentFailedEvent(request.executionId(), new RuntimeException("Max iterations reached"), Instant.now()));
        }

        return new AgentResponse("Execution did not complete successfully.", currentState, new ArrayList<>(events));
    }

    private void publishEvent(AgentEvent event) {
        events.add(event);
    }
}
