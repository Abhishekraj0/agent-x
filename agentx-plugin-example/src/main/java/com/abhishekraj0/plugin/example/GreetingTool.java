package com.abhishekraj0.plugin.example;

import com.abhishekraj0.api.tool.AgentTool;
import com.abhishekraj0.api.tool.ToolContext;
import com.abhishekraj0.api.tool.ToolId;
import com.abhishekraj0.api.tool.ToolProperty;
import com.abhishekraj0.api.tool.ToolResult;
import com.abhishekraj0.api.tool.ToolSchema;
import java.util.List;

/**
 * Example plugin tool generating a formatted greeting.
 */
public class GreetingTool implements AgentTool {

    private final ToolId id = new ToolId("greeting", "sayHello");
    private final ToolSchema schema = new ToolSchema("object", List.of(
            new ToolProperty("name", "string", "Name to greet", true)
    ));

    @Override
    public ToolId id() {
        return id;
    }

    @Override
    public String description() {
        return "Generates a personalized greeting";
    }

    @Override
    public ToolSchema inputSchema() {
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        String name = String.valueOf(context.arguments().getOrDefault("name", "World"));
        return ToolResult.success("Hello, " + name + "!");
    }
}
