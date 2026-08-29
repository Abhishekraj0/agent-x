# Model Context Protocol (MCP)

AgentX integrates the Model Context Protocol to dynamically discover and use third-party tools.

## Flow Diagram
```
┌────────────┐         ┌───────────────┐         ┌───────────┐
│ MCP Server │ ──────> │ McpToolWriter │ ──────> │ AgentTool │
└────────────┘         └───────────────┘         └───────────┘
```

## Initialization
Connect to an MCP host:
```java
var client = new DefaultMcpClient(host, port);
client.connect();
Collection<AgentTool> mcpTools = client.tools();
```
