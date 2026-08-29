package com.agentx.api.workflow;

/**
 * Interface representing a deterministic workflow definition.
 */
public interface Workflow {

    /**
     * Returns the design definition of the workflow.
     *
     * @return the workflow definition
     */
    WorkflowDefinition definition();

    /**
     * Executes the workflow with the provided context.
     *
     * @param context the execution context
     * @return the execution result
     */
    WorkflowResult execute(WorkflowContext context);
}
