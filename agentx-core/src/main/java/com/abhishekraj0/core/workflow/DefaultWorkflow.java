package com.abhishekraj0.core.workflow;

import com.abhishekraj0.api.workflow.Workflow;
import com.abhishekraj0.api.workflow.WorkflowContext;
import com.abhishekraj0.api.workflow.WorkflowDefinition;
import com.abhishekraj0.api.workflow.WorkflowResult;
import java.util.Map;

/**
 * Default implementation of Workflow.
 */
public class DefaultWorkflow implements Workflow {

    private final WorkflowDefinition definition;
    private final WorkflowStep rootStep;

    public DefaultWorkflow(WorkflowDefinition definition, WorkflowStep rootStep) {
        this.definition = definition;
        this.rootStep = rootStep;
    }

    @Override
    public WorkflowDefinition definition() {
        return definition;
    }

    @Override
    public WorkflowResult execute(WorkflowContext context) {
        try {
            Map<String, Object> output = rootStep.execute(context);
            return new WorkflowResult(context.workflowExecutionId(), true, output, null);
        } catch (Throwable t) {
            return new WorkflowResult(context.workflowExecutionId(), false, Map.of(), t);
        }
    }
}
