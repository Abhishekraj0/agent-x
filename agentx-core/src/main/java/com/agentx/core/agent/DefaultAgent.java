package com.agentx.core.agent;

import com.agentx.api.agent.*;
import com.agentx.api.context.ContextManager;
import com.agentx.api.loop.AgentLoop;
import com.agentx.api.loop.ExecutionEngine;
import com.agentx.api.loop.RetryStrategy;
import com.agentx.api.memory.MemoryStore;
import com.agentx.api.model.ChatModel;
import com.agentx.api.planner.Planner;
import com.agentx.api.security.ApprovalProvider;
import com.agentx.api.security.Guardrail;
import com.agentx.api.security.PermissionManager;
import com.agentx.api.tool.ToolRegistry;
import com.agentx.core.AgentX;
import com.agentx.core.context.SimpleContextManager;
import com.agentx.core.loop.DefaultAgentLoop;
import com.agentx.core.loop.DefaultExecutionEngine;
import java.util.List;

/**
 * Default implementation of Agent executing the runtime and looping sequences.
 */
public class DefaultAgent implements Agent {

    private final ChatModel model;
    private final Planner planner;
    private final MemoryStore memory;
    private final ToolRegistry tools;
    private final List<Guardrail> guardrails;
    private final PermissionManager permissionManager;
    private final ApprovalProvider approvalProvider;
    private final RetryStrategy retryStrategy;
    private final ContextManager contextManager;

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
        reset();
    }

    @Override
    public AgentResponse run(AgentRequest request) {
        ExecutionEngine engine = new DefaultExecutionEngine(model, tools, guardrails, permissionManager, approvalProvider);
        AgentLoop loop = new DefaultAgentLoop(request, engine, contextManager, planner, retryStrategy);
        AgentRuntime runtime = new DefaultAgentRuntime(loop);

        AgentResponse response = runtime.execute(request);
        this.lastState = response.state();
        return response;
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
