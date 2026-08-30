package com.abhishekraj0.core.agent;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.context.ContextManager;
import com.abhishekraj0.api.loop.AgentLoop;
import com.abhishekraj0.api.loop.ExecutionEngine;
import com.abhishekraj0.api.loop.RetryStrategy;
import com.abhishekraj0.api.memory.MemoryStore;
import com.abhishekraj0.api.model.ChatModel;
import com.abhishekraj0.api.planner.Planner;
import com.abhishekraj0.api.security.ApprovalProvider;
import com.abhishekraj0.api.security.Guardrail;
import com.abhishekraj0.api.security.PermissionManager;
import com.abhishekraj0.api.tool.IdempotencyManager;
import com.abhishekraj0.api.tool.ToolRegistry;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.context.SimpleContextManager;
import com.abhishekraj0.core.loop.DefaultAgentLoop;
import com.abhishekraj0.core.loop.DefaultExecutionEngine;
import java.util.List;

/**
 * Default implementation of Agent executing the runtime and looping sequences, supporting durable resume.
 */
public class DefaultAgent implements Agent, ResumableAgentRuntime, AgentRuntime {

    private final ChatModel model;
    private final Planner planner;
    private final MemoryStore memory;
    private final ToolRegistry tools;
    private final List<Guardrail> guardrails;
    private final PermissionManager permissionManager;
    private final ApprovalProvider approvalProvider;
    private final RetryStrategy retryStrategy;
    private final ContextManager contextManager;
    private final com.abhishekraj0.api.event.EventBus eventBus;
    private final com.abhishekraj0.api.loop.GoalEvaluator goalEvaluator;

    // Durable execution components
    private final AgentExecutionStore executionStore;
    private final CheckpointManager checkpointManager;
    private final IdempotencyManager idempotencyManager;

    private final java.util.Map<String, DefaultAgentRuntime> activeRuntimes = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Boolean> pendingCancellations = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, AgentExecution> completedExecutions = new java.util.concurrent.ConcurrentHashMap<>();
 
    private AgentState lastState;

    public DefaultAgent(AgentX.Builder builder) {
        this.model = builder.getModel();
        this.planner = builder.getPlanner();
        this.memory = builder.getMemory();
        this.tools = builder.getTools();
        this.guardrails = builder.getGuardrails() != null ? builder.getGuardrails() : List.of();
        this.permissionManager = builder.getPermissionManager();
        this.approvalProvider = builder.getApprovalProvider();
        this.retryStrategy = builder.getRetryStrategy();
        this.contextManager = builder.getContextManager() != null ? builder.getContextManager() : new SimpleContextManager();
        this.eventBus = builder.getEventBus();
        this.goalEvaluator = builder.getGoalEvaluator();
        this.executionStore = builder.getExecutionStore();
        this.checkpointManager = builder.getCheckpointManager();
        this.idempotencyManager = builder.getIdempotencyManager();
        reset();
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        return run(request);
    }

    @Override
    public AgentResponse run(AgentRequest request) {
        ExecutionEngine engine = new DefaultExecutionEngine(model, tools, guardrails, permissionManager, approvalProvider, eventBus);
        AgentLoop loop = new DefaultAgentLoop(
                request, engine, contextManager, planner, retryStrategy, goalEvaluator,
                executionStore, checkpointManager, idempotencyManager
        );
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(loop, executionStore, checkpointManager);
        activeRuntimes.put(request.executionId(), runtime);
        if (Boolean.TRUE.equals(pendingCancellations.get(request.executionId()))) {
            runtime.cancel(request.executionId());
        }

        try {
            AgentResponse response = runtime.execute(request);
            this.lastState = response.state();
            return response;
        } finally {
            AgentExecution exec = runtime.getExecution(request.executionId());
            if (exec != null) {
                completedExecutions.put(request.executionId(), exec);
            }
            activeRuntimes.remove(request.executionId());
            pendingCancellations.remove(request.executionId());
        }
    }

    @Override
    public AgentResponse resume(String executionId, ResumeInput input) {
        ExecutionEngine engine = new DefaultExecutionEngine(model, tools, guardrails, permissionManager, approvalProvider, eventBus);
        AgentLoop loop = new DefaultAgentLoop(
                new AgentRequest(""), engine, contextManager, planner, retryStrategy, goalEvaluator,
                executionStore, checkpointManager, idempotencyManager
        );
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(loop, executionStore, checkpointManager);
        activeRuntimes.put(executionId, runtime);
        if (Boolean.TRUE.equals(pendingCancellations.get(executionId))) {
            runtime.cancel(executionId);
        }

        try {
            AgentResponse response = runtime.resume(executionId, input);
            this.lastState = response.state();
            return response;
        } finally {
            AgentExecution exec = runtime.getExecution(executionId);
            if (exec != null) {
                completedExecutions.put(executionId, exec);
            }
            activeRuntimes.remove(executionId);
            pendingCancellations.remove(executionId);
        }
    }

    @Override
    public void cancel(String executionId) {
        pendingCancellations.put(executionId, true);
        DefaultAgentRuntime runtime = activeRuntimes.get(executionId);
        if (runtime != null) {
            runtime.cancel(executionId);
        }
    }

    @Override
    public AgentExecution getExecution(String executionId) {
        DefaultAgentRuntime runtime = activeRuntimes.get(executionId);
        if (runtime != null) {
            return runtime.getExecution(executionId);
        }
        AgentExecution completed = completedExecutions.get(executionId);
        if (completed != null) {
            return completed;
        }
        if (executionStore != null) {
            return executionStore.find(executionId)
                    .map(snapshot -> new AgentExecution(executionId, new AgentRequest(snapshot.goal()), snapshot.state(), snapshot.timestamp(), null))
                    .orElse(null);
        }
        return null;
    }

    @Override
    public AgentResponse run(String input) {
        return run(new AgentRequest(input));
    }

    @Override
    public void reset() {
        this.lastState = null;
    }

    @Override
    public AgentState state() {
        return lastState;
    }
}
