package com.agentx.core.agent;

import com.agentx.api.agent.*;
import com.agentx.api.loop.AgentLoop;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of AgentRuntime managing active agent executions.
 */
public class DefaultAgentRuntime implements AgentRuntime {

    private final AgentLoop agentLoop;
    private final Map<String, AgentExecution> executions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> cancellations = new ConcurrentHashMap<>();

    public DefaultAgentRuntime(AgentLoop agentLoop) {
        this.agentLoop = agentLoop;
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        String execId = request.executionId();
        cancellations.put(execId, false);
        AgentState initialState = AgentState.initial(execId);
        AgentExecution execution = new AgentExecution(execId, request, initialState, Instant.now(), null);
        executions.put(execId, execution);

        try {
            AgentResponse response = agentLoop.execute(initialState);
            AgentExecution finalExecution = new AgentExecution(
                    execId, request, response.state(), execution.startTime(), Instant.now()
            );
            executions.put(execId, finalExecution);
            return response;
        } catch (Exception e) {
            AgentState errorState = new AgentState(
                    execId, initialState.history(), initialState.plan(), initialState.variables(),
                    initialState.iterations(), initialState.toolCalls(), "FAILED"
            );
            AgentExecution failedExecution = new AgentExecution(
                    execId, request, errorState, execution.startTime(), Instant.now()
            );
            executions.put(execId, failedExecution);
            throw e;
        } finally {
            cancellations.remove(execId);
        }
    }

    @Override
    public void cancel(String executionId) {
        if (cancellations.containsKey(executionId)) {
            cancellations.put(executionId, true);
        }
    }

    @Override
    public AgentExecution getExecution(String executionId) {
        return executions.get(executionId);
    }

    public boolean isCancelled(String executionId) {
        return cancellations.getOrDefault(executionId, false);
    }
}
