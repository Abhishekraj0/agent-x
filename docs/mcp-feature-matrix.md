# AgentX MCP Feature Matrix

Below is the capabilities and verification matrix for Model Context Protocol (MCP) in AgentX.

| Capability      | Supported | Tested | E2E | Notes |
| --------------- | --------- | ------ | --- | ----- |
| **Tools**           | YES | YES | YES | Full support for MCP Tool discovery & execution |
| **Tool discovery**  | YES | YES | YES | Dynamic listing & automatic ToolRegistry registration |
| **Tool execution**  | YES | YES | YES | Converts inputs/outputs to AgentX ToolResult |
| **Tool schema**     | YES | YES | YES | Converts JSON schema to AgentX ToolSchema |
| **Multi-server**    | YES | YES | YES | Server namespacing prevents collision (e.g. `serverA.search`) |
| **STDIO Transport** | YES | YES | YES | Official Java SDK StdioClientTransport |
| **In-Memory/Mock**  | YES | YES | YES | Test fixture AgentXMcpTestServer |
| **SSE Transport**   | NO  | NO  | NO  | Reserved for future release |
| **Streamable HTTP** | NO  | NO  | NO  | Reserved for future release |
| **Resources**       | NO  | NO  | NO  | Out of scope for AgentX tool execution scope |
| **Prompts**         | NO  | NO  | NO  | Out of scope for AgentX tool execution scope |
| **Sampling**        | NO  | NO  | NO  | Out of scope for AgentX tool execution scope |
| **Elicitation**     | NO  | NO  | NO  | Out of scope for AgentX tool execution scope |
| **Tasks**           | NO  | NO  | NO  | Out of scope for AgentX tool execution scope |
