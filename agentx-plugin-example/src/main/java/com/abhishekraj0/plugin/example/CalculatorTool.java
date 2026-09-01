package com.abhishekraj0.plugin.example;

import com.abhishekraj0.api.tool.AgentTool;
import com.abhishekraj0.api.tool.ToolContext;
import com.abhishekraj0.api.tool.ToolId;
import com.abhishekraj0.api.tool.ToolProperty;
import com.abhishekraj0.api.tool.ToolResult;
import com.abhishekraj0.api.tool.ToolSchema;
import java.util.List;
import java.util.Map;

/**
 * Example plugin tool implementing addition calculation.
 */
public class CalculatorTool implements AgentTool {

    private final ToolId id = new ToolId("calculator", "add");
    private final ToolSchema schema = new ToolSchema("object", List.of(
            new ToolProperty("a", "number", "First number", true),
            new ToolProperty("b", "number", "Second number", true)
    ));

    @Override
    public ToolId id() {
        return id;
    }

    @Override
    public String description() {
        return "Adds two numbers";
    }

    @Override
    public ToolSchema inputSchema() {
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        Map<String, Object> args = context.arguments();
        double a = Double.parseDouble(String.valueOf(args.getOrDefault("a", 0)));
        double b = Double.parseDouble(String.valueOf(args.getOrDefault("b", 0)));
        double result = a + b;
        return ToolResult.success(String.valueOf(result));
    }
}
