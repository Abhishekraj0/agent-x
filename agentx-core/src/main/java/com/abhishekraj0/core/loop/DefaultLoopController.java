package com.abhishekraj0.core.loop;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.context.*;
import com.abhishekraj0.api.event.AgentEvent;
import com.abhishekraj0.api.event.EventBus;
import com.abhishekraj0.api.failure.AgentFailure;
import com.abhishekraj0.api.failure.FailureType;
import com.abhishekraj0.api.loop.*;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.security.*;
import com.abhishekraj0.api.tool.*;
import com.abhishekraj0.core.event.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Default implementation of LoopController coordinating the agent loop state machine with durable execution support.
 */
public class DefaultLoopController implements LoopController {

    private final ActionSelector actionSelector;
    private final GoalEvaluator goalEvaluator;
    private final ObservationHandler observationHandler;
    private final StateUpdater stateUpdater;
    private final ContextManager contextManager;
    private final ToolRegistry toolRegistry;
    private final List<Guardrail> guardrails;
    private final PermissionManager permissionManager;
    private final ApprovalProvider approvalProvider;
    private final EventBus eventBus;
    private final AgentStateMachine stateMachine;
    private final CostCalculator costCalculator = new DefaultCostCalculator();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Durable execution components
    private final AgentExecutionStore executionStore;
    private final CheckpointManager checkpointManager;
    private final IdempotencyManager idempotencyManager;

    // Optional tool-level circuit breakers
    private final Map<String, CircuitBreaker> toolCircuitBreakers = new ConcurrentHashMap<>();

    public DefaultLoopController(
            ActionSelector actionSelector,
            GoalEvaluator goalEvaluator,
            ObservationHandler observationHandler,
            StateUpdater stateUpdater,
            ContextManager contextManager,
            ToolRegistry toolRegistry,
            List<Guardrail> guardrails,
            PermissionManager permissionManager,
            ApprovalProvider approvalProvider,
            EventBus eventBus
    ) {
        this(actionSelector, goalEvaluator, observationHandler, stateUpdater, contextManager, toolRegistry, guardrails,
                permissionManager, approvalProvider, eventBus, null, null, null);
    }

    public DefaultLoopController(
            ActionSelector actionSelector,
            GoalEvaluator goalEvaluator,
            ObservationHandler observationHandler,
            StateUpdater stateUpdater,
            ContextManager contextManager,
            ToolRegistry toolRegistry,
            List<Guardrail> guardrails,
            PermissionManager permissionManager,
            ApprovalProvider approvalProvider,
            EventBus eventBus,
            AgentExecutionStore executionStore,
            CheckpointManager checkpointManager,
            IdempotencyManager idempotencyManager
    ) {
        this.actionSelector = actionSelector;
        this.goalEvaluator = goalEvaluator;
        this.observationHandler = observationHandler;
        this.stateUpdater = stateUpdater;
        this.contextManager = contextManager;
        this.toolRegistry = toolRegistry;
        this.guardrails = guardrails != null ? guardrails : List.of();
        this.permissionManager = permissionManager;
        this.approvalProvider = approvalProvider;
        this.eventBus = eventBus;
        this.stateMachine = new AgentStateMachine();
        this.stateMachine.registerDefaultTransitions();
        this.executionStore = executionStore;
        this.checkpointManager = checkpointManager;
        this.idempotencyManager = idempotencyManager;
    }

    public void registerToolCircuitBreaker(String toolName, CircuitBreaker cb) {
        if (toolName != null && cb != null) {
            toolCircuitBreakers.put(toolName, cb);
        }
    }

    private void saveCheckpoint(AgentState state) {
        if (checkpointManager != null && executionStore != null) {
            AgentCheckpoint checkpoint = checkpointManager.checkpoint(state);
            AgentExecutionSnapshot snapshot = new AgentExecutionSnapshot(
                    state.executionId(),
                    "default-agent",
                    state.history().isEmpty() ? "" : state.history().get(0).content(),
                    state,
                    state.status(),
                    state.plan(),
                    state.iterations(),
                    state.toolCalls(),
                    List.of(),
                    List.of(),
                    null,
                    null,
                    state.variables(),
                    Instant.now(),
                    Map.of()
            );
            executionStore.save(snapshot);
        }
    }

    @Override
    public LoopResult run(AgentRequest request, AgentState state) {
        try {
            return runInternal(request, state);
        } catch (AgentFailure af) {
            throw af;
        } catch (Exception e) {
            throw new AgentFailure(FailureType.AGENT_FAILURE, "RUN_ERROR", "Unexpected agent error: " + e.getMessage(), false, state.executionId(), e);
        }
    }

    private LoopResult runInternal(AgentRequest request, AgentState state) {
        Instant startTime = Instant.now();
        AgentState currentState = state;

        // Initialize state variables to track budgets
        Map<String, Object> variables = new HashMap<>(state.variables() != null ? state.variables() : Map.of());
        variables.putIfAbsent("accumulatedTokens", 0);
        variables.putIfAbsent("accumulatedCost", 0.0);
        if (request.options() != null && request.options().additionalOptions() != null) {
            Object costBudget = request.options().additionalOptions().get("costBudget");
            if (costBudget != null) {
                variables.put("costBudget", costBudget);
            }
            Object tokenBudget = request.options().additionalOptions().get("tokenBudget");
            if (tokenBudget != null) {
                variables.put("tokenBudget", tokenBudget);
            }
        }

        currentState = new AgentState(
                state.executionId(),
                state.history(),
                state.plan(),
                variables,
                state.iterations(),
                state.toolCalls(),
                state.status()
        );

        // Helper to check if runtime cancellation was requested
        Supplier<Boolean> cancellationCheck = () -> {
            Object cancelled = variables.get("cancelled");
            if (Boolean.TRUE.equals(cancelled)) {
                return true;
            }
            // Check cancellation token
            if (request.options() != null && request.options().additionalOptions() != null) {
                Object tokenObj = request.options().additionalOptions().get("cancellationToken");
                if (tokenObj instanceof CancellationToken ct) {
                    return ct.isCancelled();
                }
            }
            return false;
        };

        DefaultTerminationStrategy terminationStrategy = new DefaultTerminationStrategy(request.options(), startTime, cancellationCheck);

        // CREATED -> INITIALIZING
        if ("INITIALIZED".equals(currentState.status())) {
            currentState = transition(currentState, LoopState.INITIALIZING);
            publishEvent(new AgentStartedEvent(request.executionId(), startTime));
        }

        // INITIALIZING -> UNDERSTANDING
        if ("INITIALIZING".equals(currentState.status())) {
            currentState = transition(currentState, LoopState.UNDERSTANDING);
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
                        currentState.status()
                );
            }
        }

        while (true) {
            // Check cancellation before reasoning
            if (cancellationCheck.get()) {
                currentState = transition(currentState, LoopState.CANCELLED);
                publishEvent(new AgentFailedEvent(request.executionId(), new RuntimeException("Execution was cancelled"), Instant.now()));
                return new LoopResult(currentState, "Cancelled", false, new AgentFailure(FailureType.CANCELLATION, "CANCELLED", "Execution cancelled", false, currentState.executionId(), null));
            }

            // Check termination limits
            TerminationDecision term = terminationStrategy.evaluate(currentState);
            if (term.shouldTerminate()) {
                currentState = transition(currentState, term.targetState());
                publishEvent(new AgentFailedEvent(request.executionId(), new RuntimeException(term.reason()), Instant.now()));
                FailureType type = "Execution timed out".equals(term.reason()) ? FailureType.TIMEOUT : FailureType.BUDGET_EXCEEDED;
                return new LoopResult(currentState, "Terminated: " + term.reason(), false, new AgentFailure(type, "TERMINATED", term.reason(), false, currentState.executionId(), null));
            }

            // Check goal evaluation
            GoalStatus goalStatus = goalEvaluator.evaluate(currentState);
            if (goalStatus == GoalStatus.COMPLETE) {
                currentState = transition(currentState, LoopState.COMPLETED);
                String lastOutput = currentState.history().isEmpty() ? "" : currentState.history().get(currentState.history().size() - 1).content();
                publishEvent(new AgentCompletedEvent(request.executionId(), lastOutput, Instant.now()));
                saveCheckpoint(currentState);
                return new LoopResult(currentState, lastOutput, true, null);
            } else if (goalStatus == GoalStatus.FAILED) {
                currentState = transition(currentState, LoopState.FAILED);
                publishEvent(new AgentFailedEvent(request.executionId(), new RuntimeException("Goal evaluation returned FAILED"), Instant.now()));
                saveCheckpoint(currentState);
                return new LoopResult(currentState, "Failed", false, new AgentFailure(FailureType.AGENT_FAILURE, "GOAL_FAILED", "Goal failed", false, currentState.executionId(), null));
            } else {
                if ("COMPLETED".equals(currentState.status())) {
                    currentState = new AgentState(
                            currentState.executionId(),
                            currentState.history(),
                            currentState.plan(),
                            currentState.variables(),
                            currentState.iterations(),
                            currentState.toolCalls(),
                            "EVALUATING_GOAL"
                    );
                }
            }

            // BUILDING_CONTEXT
            currentState = transition(currentState, LoopState.BUILDING_CONTEXT);
            AgentContext context = contextManager.buildContext(request, currentState);

            // RETRIEVING_MEMORY
            currentState = transition(currentState, LoopState.RETRIEVING_MEMORY);

            // RESOLVING_TOOLS
            currentState = transition(currentState, LoopState.RESOLVING_TOOLS);
            List<AgentTool> availableTools = toolRegistry != null ? new ArrayList<>(toolRegistry.all()) : List.of();

            // PLANNING
            currentState = transition(currentState, LoopState.PLANNING);

            // DECIDING
            currentState = transition(currentState, LoopState.DECIDING);
            AgentDecision decision;
            try {
                decision = actionSelector.select(context, availableTools);
            } catch (Exception e) {
                currentState = transition(currentState, LoopState.FAILED);
                publishEvent(new AgentFailedEvent(request.executionId(), e, Instant.now()));
                return new LoopResult(currentState, "Model decision failed: " + e.getMessage(), false, new AgentFailure(FailureType.MODEL_FAILURE, "MODEL_ERROR", e.getMessage(), true, currentState.executionId(), e));
            }

            // Track Token Usage and Cost
            int currentTokens = (int) variables.getOrDefault("accumulatedTokens", 0);
            double currentCost = (double) variables.getOrDefault("accumulatedCost", 0.0);
            currentTokens += 500;
            currentCost += 0.005;
            variables.put("accumulatedTokens", currentTokens);
            variables.put("accumulatedCost", currentCost);

            // Update iterations
            currentState = new AgentState(
                    currentState.executionId(),
                    currentState.history(),
                    currentState.plan(),
                    currentState.variables(),
                    currentState.iterations() + 1,
                    currentState.toolCalls(),
                    currentState.status()
            );

            // Apply decisions
            if (decision instanceof FinalResponseDecision frd) {
                currentState = transition(currentState, LoopState.VALIDATING);
                List<ChatMessage> updatedHistory = new ArrayList<>(currentState.history());
                updatedHistory.add(ChatMessage.assistant(frd.response()));
                currentState = new AgentState(
                        currentState.executionId(),
                        updatedHistory,
                        currentState.plan(),
                        currentState.variables(),
                        currentState.iterations(),
                        currentState.toolCalls(),
                        "COMPLETED"
                );
            } else if (decision instanceof ToolCallDecision tcd) {
                List<ChatMessage> updatedHistory = new ArrayList<>(currentState.history());
                updatedHistory.add(ChatMessage.assistant(null, tcd.toolCalls()));
                currentState = new AgentState(
                        currentState.executionId(),
                        updatedHistory,
                        currentState.plan(),
                        currentState.variables(),
                        currentState.iterations(),
                        currentState.toolCalls(),
                        currentState.status()
                );

                for (ToolCall tc : tcd.toolCalls()) {
                    // Check cancellation before tool call
                    if (cancellationCheck.get()) {
                        currentState = transition(currentState, LoopState.CANCELLED);
                        return new LoopResult(currentState, "Cancelled during tool execution", false, new AgentFailure(FailureType.CANCELLATION, "CANCELLED", "Cancelled during tool execution", false, currentState.executionId(), null));
                    }

                    // RESOLVING TOOL
                    Optional<AgentTool> toolOpt = toolRegistry != null ? toolRegistry.get(new ToolId(tc.name())) : Optional.empty();
                    if (toolOpt.isEmpty()) {
                        AgentObservation obs = new AgentObservation(tc.id(), tc.name(), "Tool not found", false, new IllegalArgumentException("Tool not found"));
                        currentState = transition(currentState, LoopState.OBSERVING);
                        currentState = observationHandler.handle(obs, currentState);
                        currentState = transition(currentState, LoopState.UPDATING_STATE);
                        continue;
                    }

                    AgentTool tool = toolOpt.get();
                    AgentAction action = new AgentAction(
                            UUID.randomUUID().toString(),
                            "TOOL_CALL",
                            Map.of("toolName", tool.id().name(), "arguments", tc.argumentsJson())
                    );

                    // VALIDATING (Guardrails)
                    currentState = transition(currentState, LoopState.VALIDATING);
                    boolean passedGuardrails = true;
                    for (Guardrail guardrail : guardrails) {
                        GuardrailResult gr = guardrail.validate(action, context);
                        if (!gr.passed()) {
                            AgentObservation obs = new AgentObservation(tc.id(), tc.name(), "Guardrail validation failed: " + gr.failureReason(), false, null);
                            currentState = transition(currentState, LoopState.OBSERVING);
                            currentState = observationHandler.handle(obs, currentState);
                            currentState = transition(currentState, LoopState.UPDATING_STATE);
                            passedGuardrails = false;
                            break;
                        }
                    }
                    if (!passedGuardrails) {
                        continue;
                    }

                    // AUTHORIZING (Permissions and Approvals)
                    currentState = transition(currentState, LoopState.AUTHORIZING);
                    boolean authorized = true;

                    if (permissionManager != null) {
                        PermissionDecision permDec = permissionManager.check(action, context);
                        if (permDec.status() == PermissionStatus.DENY) {
                            AgentObservation obs = new AgentObservation(tc.id(), tc.name(), "Permission Denied: " + permDec.reason(), false, null);
                            currentState = transition(currentState, LoopState.OBSERVING);
                            currentState = observationHandler.handle(obs, currentState);
                            currentState = transition(currentState, LoopState.UPDATING_STATE);
                            authorized = false;
                        } else if (permDec.status() == PermissionStatus.REQUIRE_APPROVAL || tool.metadata().requiresApproval()) {
                            // Check if approval result is already stored in variables (restoring from pause)
                            String approvalKey = "approval_result_" + tc.id();
                            ApprovalResult resumedApproval = (ApprovalResult) currentState.variables().get(approvalKey);

                            if (resumedApproval != null) {
                                if (!resumedApproval.approved()) {
                                    AgentObservation obs = new AgentObservation(tc.id(), tc.name(), "Approval Rejected: " + resumedApproval.reason(), false, null);
                                    currentState = transition(currentState, LoopState.OBSERVING);
                                    currentState = observationHandler.handle(obs, currentState);
                                    currentState = transition(currentState, LoopState.UPDATING_STATE);
                                    authorized = false;
                                }
                            } else {
                                // Pause and checkpoint
                                if (executionStore != null) {
                                    ApprovalRequest approvalReq = new ApprovalRequest(
                                            UUID.randomUUID().toString(),
                                            "Approval required for tool " + tc.name(),
                                            tc.argumentsJson()
                                    );
                                    WaitForApprovalDecision pauseDecision = new WaitForApprovalDecision(
                                            UUID.randomUUID().toString(),
                                            "Approval required for tool " + tc.name(),
                                            approvalReq
                                    );

                                    Map<String, Object> updatedVars = new HashMap<>(currentState.variables());
                                    updatedVars.put("pending_tool_call_id", tc.id());

                                    currentState = new AgentState(
                                            currentState.executionId(),
                                            currentState.history(),
                                            currentState.plan(),
                                            updatedVars,
                                            currentState.iterations(),
                                            currentState.toolCalls(),
                                            "WAITING_APPROVAL"
                                    );

                                    AgentExecutionSnapshot snapshot = new AgentExecutionSnapshot(
                                            currentState.executionId(),
                                            "default-agent",
                                            request.input(),
                                            currentState,
                                            "WAITING_FOR_APPROVAL",
                                            currentState.plan(),
                                            currentState.iterations(),
                                            currentState.toolCalls(),
                                            List.of(),
                                            List.of(),
                                            pauseDecision,
                                            "PENDING",
                                            currentState.variables(),
                                            Instant.now(),
                                            Map.of()
                                    );
                                    executionStore.save(snapshot);

                                    return new LoopResult(currentState, "Suspended waiting for approval", false, null);
                                } else {
                                    // Synchronous/blocking fallback
                                    if (approvalProvider == null) {
                                        AgentObservation obs = new AgentObservation(tc.id(), tc.name(), "Approval required but no provider configured", false, null);
                                        currentState = transition(currentState, LoopState.OBSERVING);
                                        currentState = observationHandler.handle(obs, currentState);
                                        currentState = transition(currentState, LoopState.UPDATING_STATE);
                                        authorized = false;
                                    } else {
                                        currentState = transition(currentState, LoopState.WAITING_FOR_APPROVAL);
                                        ApprovalRequest approvalReq = approvalProvider.request(new ApprovalContext(state.executionId(), action, context));
                                        ApprovalResult approvalRes = approvalProvider.waitFor(approvalReq);
                                        if (!approvalRes.approved()) {
                                            AgentObservation obs = new AgentObservation(tc.id(), tc.name(), "Approval Rejected: " + approvalRes.reason(), false, null);
                                            currentState = transition(currentState, LoopState.OBSERVING);
                                            currentState = observationHandler.handle(obs, currentState);
                                            currentState = transition(currentState, LoopState.UPDATING_STATE);
                                            authorized = false;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!authorized) {
                        continue;
                    }

                    // EXECUTING
                    currentState = transition(currentState, LoopState.EXECUTING);
                    Map<String, Object> parsedArgs = Map.of();
                    try {
                        parsedArgs = objectMapper.readValue(tc.argumentsJson(), new TypeReference<>() {});
                    } catch (Exception ignored) {}

                    ToolContext toolContext = new ToolContext(currentState.executionId(), parsedArgs, currentState.variables());

                    // Check Circuit Breaker for tool
                    CircuitBreaker cb = toolCircuitBreakers.get(tool.id().name());
                    if (cb != null) {
                        Permission p = cb.acquire();
                        if (!p.isAllowed()) {
                            throw new AgentFailure(FailureType.TOOL_FAILURE, "CIRCUIT_OPEN", "Circuit open for tool " + tool.id().name(), false, currentState.executionId(), null);
                        }
                    }

                    // Execute tool with timeout and idempotency
                    Duration toolTimeout = Duration.ofSeconds(10);
                    if (request.options() != null && request.options().additionalOptions() != null) {
                        Object customToolTimeout = request.options().additionalOptions().get("toolTimeout");
                        if (customToolTimeout instanceof Duration d) {
                            toolTimeout = d;
                        } else if (customToolTimeout instanceof Number n) {
                            toolTimeout = Duration.ofMillis(n.longValue());
                        }
                    }

                    IdempotencyDecision idempotencyDec = IdempotencyDecision.executeNew();
                    String idempotencyKey = tool.id().name() + "_" + tc.argumentsJson();
                    if (idempotencyManager != null) {
                        ToolExecutionRequest req = new ToolExecutionRequest(
                                currentState.executionId(),
                                tc.id(),
                                tool.id().name(),
                                1,
                                idempotencyKey,
                                Instant.now()
                        );
                        idempotencyDec = idempotencyManager.check(req);
                    }

                    ToolResult toolResult;
                    if (idempotencyDec.isDuplicate()) {
                        if (idempotencyDec.success()) {
                            toolResult = ToolResult.success(idempotencyDec.cachedOutput());
                        } else {
                            toolResult = ToolResult.failure("CACHED_ERROR", idempotencyDec.errorMessage(), new RuntimeException(idempotencyDec.errorMessage()));
                        }
                    } else {
                        // Checkpoint BEFORE tool execution
                        saveCheckpoint(currentState);

                        try {
                            toolResult = executeToolWithTimeout(tool, toolContext, toolTimeout);
                            if (cb != null) {
                                cb.recordSuccess();
                            }
                        } catch (Exception e) {
                            if (cb != null) {
                                cb.recordFailure(e);
                            }
                            throw e;
                        }

                        // Record idempotency
                        if (idempotencyManager != null) {
                            ToolExecutionResult res = new ToolExecutionResult(
                                    currentState.executionId(),
                                    tc.id(),
                                    tool.id().name(),
                                    idempotencyKey,
                                    toolResult.success(),
                                    toolResult.success() ? toolResult.output() : null,
                                    toolResult.success() ? null : (toolResult.error() != null ? toolResult.error().message() : "Error"),
                                    Instant.now(),
                                    toolResult.success() ? "COMPLETED" : "FAILED"
                            );
                            idempotencyManager.record(res);
                        }
                    }

                    // OBSERVING
                    currentState = transition(currentState, LoopState.OBSERVING);
                    AgentObservation observation = new AgentObservation(
                            tc.id(),
                            tc.name(),
                            toolResult.success() ? toolResult.output() : (toolResult.error() != null ? toolResult.error().message() : "Error executing tool"),
                            toolResult.success(),
                            toolResult.error() != null ? toolResult.error().cause() : null
                    );
                    currentState = observationHandler.handle(observation, currentState);

                    // UPDATING_STATE
                    currentState = transition(currentState, LoopState.UPDATING_STATE);
                    publishEvent(new ToolCalledEvent(currentState.executionId(), tc.name(), tc.argumentsJson(), observation.output()));

                    // Checkpoint AFTER tool execution
                    saveCheckpoint(currentState);
                }
            } else if (decision instanceof AskUserDecision aud) {
                currentState = transition(currentState, LoopState.WAITING_FOR_USER);
                List<ChatMessage> updatedHistory = new ArrayList<>(currentState.history());
                updatedHistory.add(ChatMessage.assistant(aud.question()));
                currentState = new AgentState(
                        currentState.executionId(),
                        updatedHistory,
                        currentState.plan(),
                        currentState.variables(),
                        currentState.iterations(),
                        currentState.toolCalls(),
                        "WAITING_FOR_USER"
                );

                if (executionStore != null) {
                    AgentExecutionSnapshot snapshot = new AgentExecutionSnapshot(
                            currentState.executionId(),
                            "default-agent",
                            request.input(),
                            currentState,
                            "WAITING_FOR_USER",
                            currentState.plan(),
                            currentState.iterations(),
                            currentState.toolCalls(),
                            List.of(),
                            List.of(),
                            aud,
                            "PENDING",
                            currentState.variables(),
                            Instant.now(),
                            Map.of()
                    );
                    executionStore.save(snapshot);
                }

                return new LoopResult(currentState, aud.question(), true, null);
            } else if (decision instanceof DelegateDecision dd) {
                currentState = transition(currentState, LoopState.EXECUTING);
                AgentObservation observation = new AgentObservation(
                        UUID.randomUUID().toString(),
                        "delegate",
                        "Delegated to " + dd.targetAgentId() + " successfully.",
                        true,
                        null
                );
                currentState = transition(currentState, LoopState.OBSERVING);
                currentState = observationHandler.handle(observation, currentState);
                currentState = transition(currentState, LoopState.UPDATING_STATE);
            } else if (decision instanceof ReplanDecision rd) {
                currentState = transition(currentState, LoopState.REPLANNING);
                List<com.abhishekraj0.api.planner.PlanStep> newSteps = new ArrayList<>();
                newSteps.add(new com.abhishekraj0.api.planner.PlanStep("replan-step-1", rd.replanDetails()));
                com.abhishekraj0.api.planner.Plan newPlan = new com.abhishekraj0.api.planner.Plan(rd.decisionId(), currentState.plan() != null ? currentState.plan().goal() : "Replanned Goal", newSteps);
                currentState = new AgentState(
                        currentState.executionId(),
                        currentState.history(),
                        newPlan,
                        currentState.variables(),
                        currentState.iterations(),
                        currentState.toolCalls(),
                        currentState.status()
                );
            } else if (decision instanceof com.abhishekraj0.api.agent.RetryDecision rtd) {
                if (rtd.delay() != null && !rtd.delay().isZero()) {
                    try {
                        Thread.sleep(rtd.delay().toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            if (!"COMPLETED".equals(currentState.status()) && !"FAILED".equals(currentState.status()) && !"CANCELLED".equals(currentState.status()) && !"TIMEOUT".equals(currentState.status())) {
                currentState = transition(currentState, LoopState.EVALUATING_GOAL);
            }
        }
    }

    private ToolResult executeToolWithTimeout(AgentTool tool, ToolContext context, Duration timeout) {
        Future<ToolResult> future = executorService.submit(() -> {
            try {
                return tool.execute(context);
            } catch (Exception e) {
                return ToolResult.failure(ToolErrorType.UNKNOWN.name(), e.getMessage(), e);
            }
        });

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return ToolResult.failure(ToolErrorType.TIMEOUT.name(), "Tool execution timed out", e);
        } catch (Exception e) {
            return ToolResult.failure(ToolErrorType.UNKNOWN.name(), e.getMessage(), e);
        }
    }

    private AgentState transition(AgentState state, LoopState target) {
        AgentState newState = stateMachine.transition(state, target);
        publishEvent(new StateTransitionEvent(state.executionId(), state.status(), target.name()));
        return newState;
    }

    private void publishEvent(AgentEvent event) {
        if (eventBus != null) {
            eventBus.publish(event);
        }
    }
}
