package com.abhishekraj0.api.planner;

import com.abhishekraj0.api.agent.AgentRequest;
import com.abhishekraj0.api.context.AgentContext;

/**
 * Interface responsible for creating a plan of action for the agent.
 */
public interface Planner {

    /**
     * Creates a Plan based on the request and current context.
     *
     * @param request the agent request
     * @param context the agent context
     * @return the generated plan
     */
    Plan createPlan(AgentRequest request, AgentContext context);
}
