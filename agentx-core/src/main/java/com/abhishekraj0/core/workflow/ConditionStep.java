package com.abhishekraj0.core.workflow;

import com.abhishekraj0.api.workflow.WorkflowContext;
import java.util.Map;
import java.util.function.Predicate;

/**
 * WorkflowStep that evaluates a condition and branch execution accordingly.
 */
public class ConditionStep implements WorkflowStep {

    private final String id;
    private final Predicate<WorkflowContext> condition;
    private final WorkflowStep thenStep;
    private final WorkflowStep elseStep;

    public ConditionStep(String id, Predicate<WorkflowContext> condition, WorkflowStep thenStep, WorkflowStep elseStep) {
        this.id = id;
        this.condition = condition;
        this.thenStep = thenStep;
        this.elseStep = elseStep;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Map<String, Object> execute(WorkflowContext context) {
        if (condition.test(context)) {
            return thenStep != null ? thenStep.execute(context) : Map.of();
        } else {
            return elseStep != null ? elseStep.execute(context) : Map.of();
        }
    }
}
