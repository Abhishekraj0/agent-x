# Model Context Protocol (MCP) Integration in AgentX

AgentX provides first-class support for integrating external tools via the **Model Context Protocol (MCP)** specification using the official Java MCP SDK.

---

## 1. Architecture & Boundaries

AgentX maintains clean architectural boundaries:

```
+-------------------------------------------------------------+
|                      AgentX Runtime                         |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|              agentx-api: McpClient Interface                |
|           (ToolProvider abstraction, NO SDK leaks)          |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|             agentx-mcp: DefaultMcpClient / Wrapper          |
|                 (Owns io.modelcontextprotocol)              |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|               Official Java MCP SDK (2.0.1)                 |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                     MCP Tool Server                         |
+-------------------------------------------------------------+
```

---

## 2. Protocol Eras & Version Negotiation

AgentX distinguishes between MCP protocol eras:

### A. Legacy Era (2024-10-07 through 2024-11-05) — Certified GREEN
- **Active Negotiated Protocol**: `2024-11-05`
- **Lifecycle**: Stateful session initialization (`initialize` request -> `notifications/initialized` notification).
- **Transport**: STDIO pipes and custom `McpClientTransport` instances.
- **SDK Implementation**: Native support in `io.modelcontextprotocol.sdk:mcp:2.0.1`.

### B. Modern Era (2026-07-28) — Certified YELLOW (SDK 2.0.1 Limitation)
- **Characteristics**: Stateless header-based processing, `server/discover` method, no `initialize` handshake, no sticky session requirement.
- **Audit Findings**: The Java SDK `2.0.1` client adapter hardcodes legacy `initialize` handshake and does not expose native `server/discover` wire methods.
- **Verification**: Verified via `Mcp2026ModernStdioInteropTest.java` against independent subprocess `Mcp2026ModernServerProcess.java`.
- **API Realism**: `supportedProtocolVersions()` explicitly advertises `["2024-11-05"]` to prevent false capability claims.

---

## 3. Key Capabilities

### A. Multi-Server Namespacing & Collision Prevention
When connecting multiple MCP servers to an agent, tools are automatically namespaced to avoid collision:
```java
McpClient serverA = new DefaultMcpClient("serverA", "npx", List.of("-y", "@mcp/server-a"));
McpClient serverB = new DefaultMcpClient("serverB", "npx", List.of("-y", "@mcp/server-b"));

registry.registerAll(serverA); // Tools: serverA.search, serverA.get
registry.registerAll(serverB); // Tools: serverB.search, serverB.create
```

### B. Secret Redaction
Sensitive tool arguments and tool output contents (e.g. `apiKey`, `password`, `Authorization: Bearer <token>`) are automatically sanitized using regex secret redaction before being passed to logs, events, memory, or checkpoints.

### C. Security Boundaries
Outputs returned from MCP servers are treated as **untrusted tool observation data**. Prompt injection strings inside MCP tool results are safely wrapped in tool execution observation messages and cannot escalate authorization or bypass `PermissionManager` guardrails.

### D. Idempotency & Failure Recovery
MCP tools participate fully in the AgentX durable execution model. Non-idempotent high-risk MCP tools interrupted by a system crash transition to `FAIL_SAFE` unknown result status and require explicit operator approval before re-execution.

---

## 4. Code Example

```java
// Create MCP client connecting to an external MCP server via STDIO
McpClient mcpClient = new DefaultMcpClient("finance", "npx", List.of("-y", "@mcp/finance-server"));
mcpClient.connect();

// Register MCP tools with AgentX registry
ToolRegistry registry = new DefaultToolRegistry();
registry.registerAll(mcpClient);

// Construct AgentX agent
Agent agent = AgentX.builder()
        .model(chatModel)
        .tools(registry)
        .executionStore(executionStore)
        .build();

// Execute agent request utilizing MCP tools
AgentResponse response = agent.execute(new AgentRequest("Calculate total of today's failed payments"));
```
