package com.agentx.api.tool;

import java.util.List;

/**
 * Request payload for resolving subset of tools for a specific agent execution context.
 */
public record ToolResolutionRequest(
        List<ToolId> requestedTools,
        ToolContext context
) {}
