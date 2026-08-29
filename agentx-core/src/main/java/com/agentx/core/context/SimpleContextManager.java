package com.agentx.core.context;

import com.agentx.api.agent.AgentRequest;
import com.agentx.api.agent.AgentState;
import com.agentx.api.context.AgentContext;
import com.agentx.api.context.ContextManager;
import java.util.ArrayList;
import java.util.Map;

/**
 * A basic ContextManager implementation that aggregates conversation history without trimming.
 */
public class SimpleContextManager implements ContextManager {

    @Override
    public AgentContext buildContext(AgentRequest request, AgentState state) {
        return new AgentContext(
                new ArrayList<>(state.history()),
                state.variables(),
                "You are an autonomous AI agent.",
                Map.of()
        );
    }

    @Override
    public AgentContext compress(AgentContext context) {
        return context;
    }
}
