package com.agentx.api.workflow;

import java.util.Map;

/**
 * Result representing the outcome and outputs of a executed Workflow.
 */
public record WorkflowResult(
        String workflowExecutionId,
        boolean success,
        Map<String, Object> output,
        Throwable error
) {}
