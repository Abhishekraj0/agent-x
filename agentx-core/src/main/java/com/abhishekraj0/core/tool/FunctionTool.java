package com.abhishekraj0.core.tool;

import com.abhishekraj0.api.tool.*;
import java.util.function.Function;

/**
 * A standard function-based wrapper implementation of AgentTool.
 */
public class FunctionTool implements AgentTool {

    private final ToolId id;
    private final String description;
    private final ToolSchema schema;
    private final Function<ToolContext, ToolResult> executor;
    private final ToolMetadata metadata;

    public FunctionTool(ToolId id, String description, ToolSchema schema, Function<ToolContext, ToolResult> executor) {
        this(id, description, schema, executor, ToolMetadata.defaultMetadata());
    }

    public FunctionTool(ToolId id, String description, ToolSchema schema, Function<ToolContext, ToolResult> executor, ToolMetadata metadata) {
        this.id = id;
        this.description = description;
        this.schema = schema;
        this.executor = executor;
        this.metadata = metadata;
    }

    @Override
    public ToolId id() {
        return id;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public ToolSchema inputSchema() {
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        try {
            return executor.apply(context);
        } catch (Exception e) {
            return ToolResult.failure("EXECUTION_ERROR", e.getMessage(), e);
        }
    }

    @Override
    public ToolMetadata metadata() {
        return metadata;
    }
}
