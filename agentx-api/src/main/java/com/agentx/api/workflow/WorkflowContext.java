package com.agentx.api.workflow;

import java.util.Map;

/**
 * Execution context carrying state and input data for running a workflow.
 */
public record WorkflowContext(
        String workflowExecutionId,
        Map<String, Object> input,
        Map<String, Object> variables
) {}
