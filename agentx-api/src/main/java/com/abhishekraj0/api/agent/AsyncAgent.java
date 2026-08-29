package com.abhishekraj0.api.agent;

import java.util.concurrent.CompletionStage;

/**
 * Interface representing an Agent that can process requests asynchronously.
 */
public interface AsyncAgent {

    /**
     * Executes the agent with the specified structured request asynchronously.
     *
     * @param request the agent request
     * @return a completion stage containing the agent response
     */
    CompletionStage<AgentResponse> run(AgentRequest request);
}
