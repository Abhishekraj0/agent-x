package com.abhishekraj0.api.tool;

import java.util.List;

/**
 * Request payload for resolving subset of tools for a specific agent execution context.
 */
public record ToolResolutionRequest(
        List<ToolId> requestedTools,
        ToolContext context
) {}
