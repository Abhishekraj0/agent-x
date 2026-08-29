package com.abhishekraj0.api.agent;

/**
 * Extension of AgentRuntime or separate interface for resuming suspended executions.
 */
public interface ResumableAgentRuntime {

    AgentResponse resume(String executionId, ResumeInput input);
}
