package com.abhishekraj0.api.agent;

import java.util.Map;

/**
 * Represents a task assigned to an agent, e.g., in multi-agent environments.
 */
public record AgentTask(
        String taskId,
        String description,
        String assigneeAgentName,
        Map<String, Object> parameters
) {}
