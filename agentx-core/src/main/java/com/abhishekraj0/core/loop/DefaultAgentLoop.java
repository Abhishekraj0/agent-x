package com.abhishekraj0.core.loop;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.context.*;
import com.abhishekraj0.api.event.AgentEvent;
import com.abhishekraj0.api.loop.*;
import com.abhishekraj0.api.model.ChatMessage;
import com.abhishekraj0.api.planner.Plan;
import com.abhishekraj0.api.planner.Planner;
import com.abhishekraj0.core.event.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of the primary agent reasoning and action loop.
 * Integrates with LoopController state machine.
 */
public class DefaultAgentLoop implements AgentLoop {

    private final AgentRequest request;
    private final ExecutionEngine executionEngine;
    private final ContextManager contextManager;
    private final Planner planner;
    private final RetryStrategy retryStrategy;
    private final LoopController loopController;
    private final List<AgentEvent> events = new ArrayList<>();

    public DefaultAgentLoop(AgentRequest request, ExecutionEngine executionEngine, ContextManager contextManager, Planner planner) {
        this(request, executionEngine, contextManager, planner, null);
    }

    public DefaultAgentLoop(AgentRequest request, ExecutionEngine executionEngine, ContextManager contextManager, Planner planner, RetryStrategy retryStrategy) {
        this(request, executionEngine, contextManager, planner, retryStrategy, null);
    }

    public DefaultAgentLoop(AgentRequest request, ExecutionEngine executionEngine, ContextManager contextManager, Planner planner, RetryStrategy retryStrategy, GoalEvaluator goalEvaluator) {
        this.request = request;
        this.executionEngine = executionEngine;
        this.contextManager = contextManager;
        this.planner = planner;
        this.retryStrategy = retryStrategy;

        if (executionEngine instanceof DefaultExecutionEngine dee) {
            this.loopController = new DefaultLoopController(
                    new DefaultActionSelector(dee.model()),
                    goalEvaluator != null ? goalEvaluator : new DefaultGoalEvaluator(),
                    new DefaultObservationHandler(),
                    new DefaultStateUpdater(),
                    contextManager,
                    dee.toolRegistry(),
                    dee.guardrails(),
                    dee.permissionManager(),
                    dee.approvalProvider(),
                    dee.eventBus()
            );
        } else {
            this.loopController = null;
        }
    }

    @Override
    public AgentResponse execute(AgentState state) {
        if (loopController != null) {
            LoopResult result = loopController.run(request, state);
            return new AgentResponse(result.output(), result.finalState(), new ArrayList<>(events));
        }

        // Legacy fallback loop
        AgentState currentState = state;

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

        publishEvent(new AgentStartedEvent(request.executionId(), Instant.now()));

        while (currentState.iterations() < maxIterations && "RUNNING".equals(currentState.status())) {

            AgentContext context = contextManager.buildContext(request, currentState);

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

            currentState = new AgentState(
                    currentState.executionId(),
                    currentState.history(),
                    currentState.plan(),
                    currentState.variables(),
                    currentState.iterations() + 1,
                    currentState.toolCalls(),
                    currentState.status()
            );

            ExecutionRequest stepRequest = new ExecutionRequest(request, currentState);
            ExecutionResult stepResult = executionEngine.execute(stepRequest);

            currentState = stepResult.state();

            if (!stepResult.success()) {
                if (retryStrategy != null) {
                    FailureContext fc = new FailureContext(currentState.executionId(), stepResult.error(), currentState.iterations(), "MODEL_CALL");
                    com.abhishekraj0.api.loop.RetryDecision decision = retryStrategy.onFailure(fc);
                    if (decision.shouldRetry()) {
                        if (decision.delay() != null && !decision.delay().isZero()) {
                            try {
                                Thread.sleep(decision.delay().toMillis());
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        continue;
                    }
                }

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
