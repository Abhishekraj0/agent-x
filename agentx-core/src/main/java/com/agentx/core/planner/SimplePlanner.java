package com.agentx.core.planner;

import com.agentx.api.agent.AgentRequest;
import com.agentx.api.context.AgentContext;
import com.agentx.api.planner.Plan;
import com.agentx.api.planner.PlanStep;
import com.agentx.api.planner.Planner;
import java.util.List;
import java.util.UUID;

/**
 * Simple implementation of Planner that breaks down the request into default steps.
 */
public class SimplePlanner implements Planner {

    @Override
    public Plan createPlan(AgentRequest request, AgentContext context) {
        String planId = UUID.randomUUID().toString();
        List<PlanStep> steps = List.of(
                new PlanStep(UUID.randomUUID().toString(), "Analyze user query: " + request.input(), "PENDING", null),
                new PlanStep(UUID.randomUUID().toString(), "Execute actions to fulfill request", "PENDING", null)
        );
        return new Plan(planId, request.input(), steps);
    }
}
