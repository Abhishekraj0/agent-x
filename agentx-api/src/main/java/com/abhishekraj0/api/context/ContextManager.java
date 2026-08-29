package com.abhishekraj0.api.context;

import com.abhishekraj0.api.agent.AgentRequest;
import com.abhishekraj0.api.agent.AgentState;

/**
 * Interface for building and compressing execution contexts.
 */
public interface ContextManager {

    /**
     * Builds the AgentContext from the current request and state.
     *
     * @param request the agent request
     * @param state   the current agent state
     * @return the built context
     */
    AgentContext buildContext(AgentRequest request, AgentState state);

    /**
     * Compresses the context (e.g. by summarizing or trimming history) to fit within token budgets.
     *
     * @param context the context to compress
     * @return the compressed context
     */
    AgentContext compress(AgentContext context);
}
