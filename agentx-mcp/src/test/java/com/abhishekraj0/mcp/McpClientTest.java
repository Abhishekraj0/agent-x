package com.abhishekraj0.mcp;

import com.abhishekraj0.api.tool.*;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class McpClientTest {

    @Mock
    private McpSyncClient mcpSyncClient;

    @Mock
    private McpSchema.Tool mcpTool;

    @Mock
    private McpSchema.CallToolResult callResult;

    @Test
    @SuppressWarnings("unchecked")
    public void testMcpToolWrapperExecution() {
        // Mock Tool attributes
        when(mcpTool.name()).thenReturn("test-tool");
        when(mcpTool.description()).thenReturn("Test Description");

        // Mock input schema mapping
        Map<String, Object> mockSchema = Map.of("type", "object", "properties", Map.of("input", Map.of("type", "string")));
        doReturn(mockSchema).when(mcpTool).inputSchema();

        // Mock Tool Execution result
        McpSchema.TextContent textContent = new McpSchema.TextContent("mcp-tool-response");

        List<McpSchema.Content> contentList = List.of(textContent);
        when(callResult.content()).thenReturn(contentList);

        when(mcpSyncClient.callTool(any(McpSchema.CallToolRequest.class))).thenReturn(callResult);

        // Instantiate Wrapper
        McpToolWrapper wrapper = new McpToolWrapper(mcpTool, mcpSyncClient);

        assertEquals("test-tool", wrapper.id().name());
        assertEquals("Test Description", wrapper.description());
        assertNotNull(wrapper.inputSchema());
        assertEquals("object", wrapper.inputSchema().type());
        assertTrue(wrapper.inputSchema().properties().stream().anyMatch(p -> p.name().equals("input")));

        // Execute Tool
        ToolContext context = new ToolContext("session-1", Map.of("input", "hello"));
        ToolResult result = wrapper.execute(context);

        assertTrue(result.success());
        assertEquals("mcp-tool-response", result.output());
        assertNull(result.error());
    }
}
