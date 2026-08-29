package com.abhishekraj0.core.context;

import com.abhishekraj0.api.agent.AgentRequest;
import com.abhishekraj0.api.agent.AgentState;
import com.abhishekraj0.api.context.AgentContext;
import com.abhishekraj0.api.context.ContextManager;
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
