package com.agentx.api.workflow;

import java.util.List;
import java.util.Map;

/**
 * Definition metadata outlining the steps and parameters of a Workflow.
 */
public record WorkflowDefinition(
        String id,
        String name,
        List<String> steps,
        Map<String, Object> metadata
) {}
