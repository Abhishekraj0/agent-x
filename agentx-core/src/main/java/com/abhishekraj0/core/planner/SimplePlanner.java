package com.abhishekraj0.core.planner;

import com.abhishekraj0.api.agent.AgentRequest;
import com.abhishekraj0.api.context.AgentContext;
import com.abhishekraj0.api.planner.Plan;
import com.abhishekraj0.api.planner.PlanStep;
import com.abhishekraj0.api.planner.Planner;
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
