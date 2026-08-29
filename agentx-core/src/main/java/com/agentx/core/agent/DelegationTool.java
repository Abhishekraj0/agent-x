package com.agentx.core.agent;

import com.agentx.api.agent.AgentCoordinator;
import com.agentx.api.agent.AgentResponse;
import com.agentx.api.agent.AgentTask;
import com.agentx.api.tool.AgentTool;
import com.agentx.api.tool.ToolContext;
import com.agentx.api.tool.ToolId;
import com.agentx.api.tool.ToolMetadata;
import com.agentx.api.tool.ToolProperty;
import com.agentx.api.tool.ToolResult;
import com.agentx.api.tool.ToolSchema;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An AgentTool that allows one agent to delegate tasks to another agent via the AgentCoordinator.
 */
public class DelegationTool implements AgentTool {

    private final AgentCoordinator coordinator;

    public DelegationTool(AgentCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public ToolId id() {
        return new ToolId("delegate_task");
    }

    @Override
    public String description() {
        return "Delegates a task to another specialized agent and returns their response.";
    }

    @Override
    public ToolSchema inputSchema() {
        return new ToolSchema("object", List.of(
                new ToolProperty("assignee", "string", "The name of the target agent to handle the task", true),
                new ToolProperty("taskDescription", "string", "Detailed description of the task for the target agent", true)
        ));
    }

    @Override
    public ToolResult execute(ToolContext context) {
        String assignee = (String) context.arguments().get("assignee");
        String taskDescription = (String) context.arguments().get("taskDescription");

        if (assignee == null || taskDescription == null) {
            return ToolResult.failure("MISSING_ARGUMENTS", "Both assignee and taskDescription are required.", null);
        }

        AgentTask task = new AgentTask(UUID.randomUUID().toString(), taskDescription, assignee, Map.of());
        AgentResponse response = coordinator.delegate(task);

        if ("FAILED".equals(response.state().status())) {
            return ToolResult.failure("DELEGATION_FAILED", response.output(), null);
        }

        return ToolResult.success(response.output());
    }
}
