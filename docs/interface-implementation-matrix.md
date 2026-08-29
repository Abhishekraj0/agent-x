# AgentX Interface-Implementation Matrix

This document provides a deep structural mapping of all public interfaces in the AgentX library.

| Interface | Concrete Implementation | Created By | Consumed By | Replaceable | Unit Test | Integration Test | E2E Test | Docs | Thread Safety |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `Agent` | `DefaultAgent` | `AgentX.Builder` | Users | YES | `AgentLoopTest` | Yes | Yes | YES | Thread-safe (stateless runner) |
| `AgentRuntime` | `DefaultAgentRuntime` | `DefaultAgent` | `DefaultAgent` | YES | `AgentLoopTest` | Yes | Yes | YES | Thread-safe |
| `AgentLoop` | `DefaultAgentLoop` | `DefaultAgent` | `DefaultAgentRuntime` | YES | `AgentLoopTest` | Yes | Yes | YES | Thread-safe (run-scoped context) |
| `LoopController` | `DefaultLoopController` | `DefaultAgentLoop` | `DefaultAgentLoop` | YES | `RetryAndBudgetTest` | Yes | Yes | YES | Thread-safe (instantiated per-run) |
| `Planner` | `SimplePlanner` | Builder / Spring | `DefaultAgentLoop` / `DefaultLoopController` | YES | `WorkflowEngineTest` | Yes | Yes | YES | Thread-safe (stateless) |
| `ActionSelector` | `DefaultActionSelector` | `DefaultAgentLoop` | `DefaultLoopController` | YES | `RetryAndBudgetTest` | Yes | Yes | YES | Thread-safe |
| `GoalEvaluator` | `DefaultGoalEvaluator` | `DefaultAgentLoop` | `DefaultLoopController` | YES | `RetryAndBudgetTest` | Yes | Yes | YES | Thread-safe |
| `TerminationStrategy` | `DefaultTerminationStrategy` | `DefaultAgentLoop` | `DefaultLoopController` | YES | `RetryAndBudgetTest` | Yes | Yes | YES | Thread-safe |
| `ObservationHandler` | `DefaultObservationHandler` | `DefaultAgentLoop` | `DefaultLoopController` | YES | `RetryAndBudgetTest` | Yes | Yes | YES | Thread-safe |
| `StateUpdater` | `DefaultStateUpdater` | `DefaultAgentLoop` | `DefaultLoopController` | YES | `RetryAndBudgetTest` | Yes | Yes | YES | Thread-safe |
| `ExecutionEngine` | `DefaultExecutionEngine` | `DefaultAgent` | `DefaultAgentLoop` | YES | `ToolSystemTest` | Yes | Yes | YES | Thread-safe |
| `ChatModel` | `MockChatModel`, adapters | Builder / Spring | `DefaultExecutionEngine` | YES | `MockChatModelTest` | Yes | Yes | YES | Thread-safe |
| `StreamingChatModel` | *None* | N/A | N/A | YES | None | No | No | YES | N/A |
| `AgentTool` | `FunctionTool`, `McpToolWrapper` | User | `DefaultToolRegistry` | YES | `ToolSystemTest` | Yes | Yes | YES | Thread-safe (if logic is stateless) |
| `ToolRegistry` | `DefaultToolRegistry` | Builder / Spring | `DefaultExecutionEngine` / `DefaultLoopController` | YES | `ToolSystemTest` | Yes | Yes | YES | Thread-safe (`ConcurrentHashMap`) |
| `ToolResolver` | `DefaultToolResolver` | `DefaultToolRegistry` | `DefaultToolRegistry` | YES | `ToolSystemTest` | Yes | Yes | YES | Thread-safe |
| `ToolProvider` | *None* | N/A | N/A | YES | None | No | No | YES | N/A |
| `MemoryStore` | `InMemoryVectorMemoryStore` | Builder / Spring | Agents / Users | YES | `MemoryAndContextTest` | Yes | Yes | YES | Thread-safe (`CopyOnWriteArrayList`) |
| `MemoryRetriever` | *None* | N/A | N/A | YES | None | No | No | YES | N/A |
| `ContextManager` | `SimpleContextManager` | Builder / Spring | `DefaultAgentLoop` / `DefaultLoopController` | YES | `MemoryAndContextTest` | Yes | Yes | YES | Thread-safe |
| `TokenBudgetManager` | `DefaultTokenBudgetManager` | `SimpleContextManager` | `SimpleContextManager` | YES | `RetryAndBudgetTest` | Yes | Yes | YES | Thread-safe |
| `RetryStrategy` | `SimpleRetryStrategy` | Builder / Spring | `DefaultAgentLoop` / `DefaultLoopController` | YES | `RetryAndBudgetTest` | Yes | Yes | YES | Thread-safe |
| `Guardrail` | `CompositeGuardrail`, `PromptInjectionGuardrail` | Builder / Spring | `DefaultExecutionEngine` / `DefaultLoopController` | YES | `SecurityAndGuardrailTest` | Yes | Yes | YES | Thread-safe |
| `PermissionManager` | `DefaultPermissionManager` | Builder / Spring | `DefaultExecutionEngine` / `DefaultLoopController` | YES | `SecurityAndGuardrailTest` | Yes | Yes | YES | Thread-safe |
| `ApprovalProvider` | `SimpleApprovalProvider` | Builder / Spring | `DefaultExecutionEngine` / `DefaultLoopController` | YES | `SecurityAndGuardrailTest` | Yes | Yes | YES | Thread-safe |
| `EventBus` | `SimpleEventBus` | Builder / Spring | Agents / Engines | YES | `ObservabilityTest` | Yes | Yes | YES | Thread-safe (`ConcurrentHashMap`) |
| `AgentObserver` | *None* | N/A | N/A | YES | None | No | No | YES | N/A |
| `AgentEvaluator` | *None* | N/A | N/A | YES | None | No | No | YES | N/A |
| `AgentCoordinator` | `DefaultAgentCoordinator` | Registry / Spring | Agents | YES | `MultiAgentTest` | Yes | Yes | YES | Thread-safe |
| `AgentRegistry` | `DefaultAgentRegistry` | Spring / User | `DefaultAgentCoordinator` | YES | `MultiAgentTest` | Yes | Yes | YES | Thread-safe (`ConcurrentHashMap`) |
| `Workflow` | `DefaultWorkflow` | User | User | YES | `WorkflowEngineTest` | Yes | Yes | YES | Thread-safe (run-scoped context) |
| `AgentTrigger` | *None* | N/A | N/A | YES | None | No | No | YES | N/A |
| `AgentPlugin` | *None* (dynamically loaded) | `ServiceLoader` | `PluginManager` | YES | `PluginSystemTest` | Yes | Yes | YES | Thread-safe |
| `McpClient` | `DefaultMcpClient` | User | User / Registry | YES | `McpClientTest` | Yes | Yes | YES | Thread-safe |
