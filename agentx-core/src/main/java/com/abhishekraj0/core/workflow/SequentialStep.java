package com.abhishekraj0.core.workflow;

import com.abhishekraj0.api.workflow.WorkflowContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkflowStep that executes a list of child steps in sequence.
 */
public class SequentialStep implements WorkflowStep {

    private final String id;
    private final List<WorkflowStep> steps;

    public SequentialStep(String id, List<WorkflowStep> steps) {
        this.id = id;
        this.steps = steps != null ? steps : List.of();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Map<String, Object> execute(WorkflowContext context) {
        Map<String, Object> stepOutput = new HashMap<>();
        for (WorkflowStep step : steps) {
            Map<String, Object> subOutput = step.execute(context);
            if (subOutput != null) {
                stepOutput.putAll(subOutput);
                context.variables().putAll(subOutput);
            }
        }
        return stepOutput;
    }
}
