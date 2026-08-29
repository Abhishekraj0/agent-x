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
import com.abhishekraj0.api.tool.ToolRegistry;
import com.abhishekraj0.core.AgentX;
import com.abhishekraj0.core.context.SimpleContextManager;
import com.abhishekraj0.core.loop.DefaultAgentLoop;
import com.abhishekraj0.core.loop.DefaultExecutionEngine;
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
    private final com.abhishekraj0.api.event.EventBus eventBus;

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
        reset();
    }

    @Override
    public AgentResponse run(AgentRequest request) {
        ExecutionEngine engine = new DefaultExecutionEngine(model, tools, guardrails, permissionManager, approvalProvider, eventBus);
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
