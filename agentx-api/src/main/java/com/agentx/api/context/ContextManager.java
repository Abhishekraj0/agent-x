package com.agentx.api.context;

import com.agentx.api.agent.AgentRequest;
import com.agentx.api.agent.AgentState;

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
