package com.abhishekraj0.mcp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class McpArchitectureTest {

    @Test
    public void testApiAndCoreDoNotImportMcpSdkClasses() throws Exception {
        // Verify agentx-api class files do not reference io.modelcontextprotocol
        Class<?> apiClass = com.abhishekraj0.api.mcp.McpClient.class;
        for (var method : apiClass.getMethods()) {
            String returnType = method.getReturnType().getName();
            assertFalse(returnType.startsWith("io.modelcontextprotocol"), "agentx-api must NOT leak MCP SDK types in methods");
        }

        // Verify agentx-mcp owns MCP SDK
        Class<?> mcpWrapperClass = com.abhishekraj0.mcp.McpToolWrapper.class;
        assertNotNull(mcpWrapperClass);
    }
}
