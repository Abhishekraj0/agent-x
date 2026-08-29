package com.abhishekraj0.core.tool;

import com.abhishekraj0.api.tool.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of the ToolResolver that looks up tools in a ToolRegistry.
 */
public class DefaultToolResolver implements ToolResolver {

    private final ToolRegistry registry;

    public DefaultToolResolver(ToolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<AgentTool> resolve(ToolResolutionRequest request) {
        if (request == null) {
            return List.of();
        }
        if (request.requestedTools() == null || request.requestedTools().isEmpty()) {
            return new ArrayList<>(registry.all());
        }
        return request.requestedTools().stream()
                .map(registry::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
