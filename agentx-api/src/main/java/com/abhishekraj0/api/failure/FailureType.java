package com.abhishekraj0.api.failure;

/**
 * Standardized categories of runtime errors in the AgentX framework.
 */
public enum FailureType {
    AGENT_FAILURE,
    MODEL_FAILURE,
    TOOL_FAILURE,
    MEMORY_FAILURE,
    MCP_FAILURE,
    WORKFLOW_FAILURE,
    SECURITY_FAILURE,
    APPROVAL_FAILURE,
    TIMEOUT,
    CANCELLATION,
    BUDGET_EXCEEDED,
    INVALID_STATE,
    PLUGIN_FAILURE
}
