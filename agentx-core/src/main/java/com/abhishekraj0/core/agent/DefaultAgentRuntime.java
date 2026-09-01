package com.abhishekraj0.core.agent;

import com.abhishekraj0.api.agent.*;
import com.abhishekraj0.api.failure.AgentFailure;
import com.abhishekraj0.api.failure.FailureType;
import com.abhishekraj0.api.loop.AgentLoop;
import com.abhishekraj0.api.model.ChatMessage;
import com.abhishekraj0.api.memory.MemoryContextHolder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resumable implementation of AgentRuntime managing active agent executions with durable pause/resume.
 */
public class DefaultAgentRuntime implements AgentRuntime, ResumableAgentRuntime {

    private final AgentLoop agentLoop;
    private final AgentExecutionStore executionStore;
    private final CheckpointManager checkpointManager;
    private final Map<String, AgentExecution> executions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> cancellations = new ConcurrentHashMap<>();

    public DefaultAgentRuntime(AgentLoop agentLoop) {
        this(agentLoop, null, null);
    }

    public DefaultAgentRuntime(AgentLoop agentLoop, AgentExecutionStore executionStore, CheckpointManager checkpointManager) {
        this.agentLoop = agentLoop;
        this.executionStore = executionStore;
        this.checkpointManager = checkpointManager;
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        String execId = request.executionId();
        DefaultCancellationToken ct = new DefaultCancellationToken();
        DefaultCancellationToken.register(execId, ct);
        if (Boolean.TRUE.equals(cancellations.get(execId))) {
            ct.cancel();
        } else {
            cancellations.put(execId, false);
        }
        AgentState initialState = AgentState.initial(execId);
        AgentExecution execution = new AgentExecution(execId, request, initialState, Instant.now(), null);
        executions.put(execId, execution);

        try {
            MemoryContextHolder.setExecutionId(execId);
            AgentResponse response = agentLoop.execute(initialState);
            AgentExecution finalExecution = new AgentExecution(
                    execId, request, response.state(), execution.startTime(), Instant.now()
            );
            executions.put(execId, finalExecution);
            return response;
        } catch (Exception e) {
            boolean cancelled = isCancelled(execId) || (e instanceof AgentFailure af && af.getType() == FailureType.CANCELLATION);
            String status = cancelled ? "CANCELLED" : "FAILED";
            AgentState errorState = new AgentState(
                    execId, initialState.history(), initialState.plan(), initialState.variables(),
                    initialState.iterations(), initialState.toolCalls(), status
            );
            AgentExecution failedExecution = new AgentExecution(
                    execId, request, errorState, execution.startTime(), Instant.now()
            );
            executions.put(execId, failedExecution);
            throw e;
        } finally {
            MemoryContextHolder.clearExecutionId();
            cancellations.remove(execId);
            DefaultCancellationToken.deregister(execId);
        }
    }

    @Override
    public AgentResponse resume(String executionId, ResumeInput input) {
        if (executionStore == null) {
            throw new AgentFailure(FailureType.INVALID_STATE, "NO_STORE", "No execution store configured for durable resume", false, executionId, null);
        }
        Optional<AgentExecutionSnapshot> snapshotOpt = executionStore.find(executionId);
        if (snapshotOpt.isEmpty()) {
            throw new AgentFailure(FailureType.INVALID_STATE, "SNAPSHOT_NOT_FOUND", "Snapshot not found for execution ID: " + executionId, false, executionId, null);
        }
        AgentExecutionSnapshot snapshot = snapshotOpt.get();
        AgentState state = snapshot.state();

        if (!"WAITING_APPROVAL".equals(state.status()) && !"WAITING_FOR_USER".equals(state.status())) {
            String code = ("RUNNING".equals(snapshot.loopState()) || "RUNNING".equals(state.status())) ? "EXECUTION_ALREADY_RUNNING" : "INVALID_STATUS";
            throw new AgentFailure(FailureType.INVALID_STATE, code, "Cannot resume execution in status: " + state.status(), false, executionId, null);
        }

        Map<String, Object> updatedVariables = new HashMap<>(state.variables() != null ? state.variables() : Map.of());
        List<ChatMessage> updatedHistory = new ArrayList<>(state.history());

        if ("WAITING_APPROVAL".equals(state.status())) {
            if (input.approvalResult() == null) {
                throw new AgentFailure(FailureType.INVALID_STATE, "MISSING_APPROVAL", "ApprovalResult is required to resume from WAITING_APPROVAL", false, executionId, null);
            }
            String toolCallId = (String) state.variables().get("pending_tool_call_id");
            if (toolCallId == null) {
                throw new AgentFailure(FailureType.INVALID_STATE, "NO_PENDING_TOOL", "No pending tool call ID found in state variables", false, executionId, null);
            }
            updatedVariables.put("approval_result_" + toolCallId, input.approvalResult());
        } else if ("WAITING_FOR_USER".equals(state.status())) {
            if (input.userInput() == null) {
                throw new AgentFailure(FailureType.INVALID_STATE, "MISSING_USER_INPUT", "UserInput is required to resume from WAITING_FOR_USER", false, executionId, null);
            }
            updatedHistory.add(ChatMessage.user(input.userInput()));
        }

        if (input.additionalVariables() != null) {
            updatedVariables.putAll(input.additionalVariables());
        }

        AgentState restoredState = new AgentState(
                state.executionId(),
                updatedHistory,
                state.plan(),
                updatedVariables,
                state.iterations(),
                state.toolCalls(),
                "INITIALIZED"
        );

        AgentExecutionSnapshot runningSnapshot = new AgentExecutionSnapshot(
                snapshot.executionId(),
                snapshot.agentId(),
                snapshot.goal(),
                restoredState,
                "RUNNING",
                snapshot.plan(),
                snapshot.iteration(),
                snapshot.toolCallCount(),
                snapshot.observations(),
                snapshot.memoryReferences(),
                snapshot.pendingDecision(),
                snapshot.approvalState(),
                snapshot.budgets(),
                snapshot.timestamp(),
                snapshot.metadata(),
                snapshot.version()
        );
        try {
            executionStore.save(runningSnapshot);
        } catch (java.util.ConcurrentModificationException e) {
            throw new AgentFailure(FailureType.INVALID_STATE, "EXECUTION_ALREADY_RUNNING",
                    "Execution " + executionId + " is already running or has been modified by another runtime instance",
                    false, executionId, e);
        }

        DefaultCancellationToken ct = new DefaultCancellationToken();
        DefaultCancellationToken.register(executionId, ct);
        if (Boolean.TRUE.equals(cancellations.get(executionId))) {
            ct.cancel();
        } else {
            cancellations.put(executionId, false);
        }
        AgentExecution execution = new AgentExecution(executionId, new AgentRequest(snapshot.goal()), restoredState, snapshot.timestamp(), null);
        executions.put(executionId, execution);

        try {
            MemoryContextHolder.setExecutionId(executionId);
            AgentResponse response = agentLoop.execute(restoredState);
            AgentExecution finalExecution = new AgentExecution(
                    executionId, new AgentRequest(snapshot.goal()), response.state(), execution.startTime(), Instant.now()
            );
            executions.put(executionId, finalExecution);
            return response;
        } catch (Exception e) {
            boolean cancelled = isCancelled(executionId) || (e instanceof AgentFailure af && af.getType() == FailureType.CANCELLATION);
            String status = cancelled ? "CANCELLED" : "FAILED";
            AgentState errorState = new AgentState(
                    executionId, restoredState.history(), restoredState.plan(), restoredState.variables(),
                    restoredState.iterations(), restoredState.toolCalls(), status
            );
            AgentExecution failedExecution = new AgentExecution(
                    executionId, new AgentRequest(snapshot.goal()), errorState, execution.startTime(), Instant.now()
            );
            executions.put(executionId, failedExecution);
            throw e;
        } finally {
            MemoryContextHolder.clearExecutionId();
            cancellations.remove(executionId);
            DefaultCancellationToken.deregister(executionId);
        }
    }

    @Override
    public void cancel(String executionId) {
        cancellations.put(executionId, true);
        CancellationToken token = DefaultCancellationToken.get(executionId);
        if (token instanceof DefaultCancellationToken dct) {
            dct.cancel();
        }
    }

    @Override
    public AgentExecution getExecution(String executionId) {
        return executions.get(executionId);
    }

    public boolean isCancelled(String executionId) {
        CancellationToken token = DefaultCancellationToken.get(executionId);
        return (token != null && token.isCancelled()) || cancellations.getOrDefault(executionId, false);
    }
}
