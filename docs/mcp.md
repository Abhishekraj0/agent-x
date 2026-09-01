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

## 2. Specification & SDK Versions

- **MCP Protocol Specification**: `2024-11-05`
- **Official Java SDK**: `io.modelcontextprotocol.sdk:mcp` version `2.0.1`
- **Supported Transports**: `STDIO`, `Mock / In-Memory (Test)`
- **Scope**: Tools, Tool Discovery, Tool Execution, Tool Schemas, Multi-Server Namespacing.
- **Out-of-Scope Capabilities**: Resources, Prompts, Sampling, Elicitation, Tasks (reserved for future iterations).

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
