package com.abhishekraj0.core.tool;

import com.abhishekraj0.api.tool.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default in-memory implementation of the ToolRegistry.
 */
public class DefaultToolRegistry implements ToolRegistry {

    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();

    @Override
    public void register(AgentTool tool) {
        if (tool != null && tool.id() != null) {
            tools.put(tool.id().getFullName(), tool);
        }
    }

    @Override
    public void unregister(ToolId id) {
        if (id != null) {
            tools.remove(id.getFullName());
        }
    }

    @Override
    public Optional<AgentTool> get(ToolId id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tools.get(id.getFullName()));
    }

    @Override
    public Collection<AgentTool> all() {
        return Collections.unmodifiableCollection(tools.values());
    }

    @Override
    public Collection<AgentTool> find(ToolQuery query) {
        return tools.values().stream()
                .filter(tool -> {
                    if (query.searchTerm() != null && !query.searchTerm().isBlank()) {
                        String term = query.searchTerm().toLowerCase();
                        boolean nameMatch = tool.id().name().toLowerCase().contains(term);
                        boolean descMatch = tool.description() != null && tool.description().toLowerCase().contains(term);
                        if (!nameMatch && !descMatch) {
                            return false;
                        }
                    }
                    if (query.maxRiskLevel() != null) {
                        if (tool.metadata().riskLevel().ordinal() > query.maxRiskLevel().ordinal()) {
                            return false;
                        }
                    }
                    if (query.readOnly() != null) {
                        if (tool.metadata().readOnly() != query.readOnly()) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
}
