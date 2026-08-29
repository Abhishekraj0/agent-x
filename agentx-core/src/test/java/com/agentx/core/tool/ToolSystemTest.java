package com.agentx.core.tool;

import com.agentx.api.tool.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ToolSystemTest {

    @Test
    public void testRegisterAndExecuteTool() {
        DefaultToolRegistry registry = new DefaultToolRegistry();
        
        ToolId toolId = new ToolId("math", "add");
        AgentTool addTool = new FunctionTool(
                toolId,
                "Adds two numbers",
                ToolSchema.empty(),
                context -> {
                    Map<String, Object> args = context.arguments();
                    Number a = (Number) args.get("a");
                    Number b = (Number) args.get("b");
                    double sum = a.doubleValue() + b.doubleValue();
                    return ToolResult.success(String.valueOf(sum));
                }
        );

        registry.register(addTool);

        Optional<AgentTool> retrieved = registry.get(toolId);
        assertTrue(retrieved.isPresent());
        assertEquals("math:add", retrieved.get().id().getFullName());

        // Execute
        ToolContext context = new ToolContext("exec-123", Map.of("a", 5, "b", 10));
        ToolResult result = retrieved.get().execute(context);

        assertTrue(result.success());
        assertEquals("15.0", result.output());
        assertNull(result.error());
    }

    @Test
    public void testRegistryFindAndResolver() {
        DefaultToolRegistry registry = new DefaultToolRegistry();
        
        AgentTool tool1 = new FunctionTool(new ToolId("t1"), "First tool", ToolSchema.empty(), ctx -> ToolResult.success("1"));
        AgentTool tool2 = new FunctionTool(new ToolId("t2"), "Second tool", ToolSchema.empty(), ctx -> ToolResult.success("2"));
        
        registry.register(tool1);
        registry.register(tool2);

        // Find query
        Collection<AgentTool> found = registry.find(new ToolQuery("Second", null, null));
        assertEquals(1, found.size());
        assertEquals("t2", found.iterator().next().id().name());

        // Resolve
        DefaultToolResolver resolver = new DefaultToolResolver(registry);
        List<AgentTool> resolved = resolver.resolve(new ToolResolutionRequest(List.of(new ToolId("t1")), new ToolContext("1", Map.of())));
        assertEquals(1, resolved.size());
        assertEquals("t1", resolved.get(0).id().name());
    }
}
