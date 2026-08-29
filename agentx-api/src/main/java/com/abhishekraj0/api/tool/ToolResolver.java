package com.abhishekraj0.api.tool;

import java.util.List;

/**
 * Interface responsible for resolving specific tools for a given context or request.
 */
public interface ToolResolver {

    /**
     * Resolves a list of tools for execution context.
     *
     * @param request the resolution request details
     * @return list of resolved tools
     */
    List<AgentTool> resolve(ToolResolutionRequest request);
}
