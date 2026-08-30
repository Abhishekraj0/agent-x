# AgentX Phase 5 Interface Matrix

This matrix documents all major public interfaces of the AgentX framework.

| Interface | Purpose | Implementation | Runtime Used | Thread Safe | Persistable | Tested | API Category |
| --------- | ------- | -------------- | ------------ | ----------- | ----------- | ------ | ------------ |
| `Agent` | Defines agent settings/actions | `DefaultAgent` | Yes | Yes | No (Config) | Yes | CORE API |
| `AgentRuntime` | Standard execution boundary | `DefaultAgentRuntime` | Yes | Yes | No | Yes | CORE API |
| `ResumableAgentRuntime` | Adds pause/resume API | `DefaultAgentRuntime` | Yes | Yes | No | Yes | CORE API |
| `CancellationToken` | Cooperative cancellation control | `DefaultCancellationToken` | Yes | Yes | No (Volatile) | Yes | CORE API |
| `ToolRegistry` | Registers and discovers tools | `DefaultToolRegistry` | Yes | Yes | No | Yes | CORE API |
| `AgentTool` | Extensible action capability | Various (Functional) | Yes | Yes | No | Yes | CORE API / SPI |
| `AgentExecutionStore` | Stores execution state | `InMemoryAgentExecutionStore` | Yes | Yes | Yes | Yes | SPI |
| `CheckpointManager` | Triggers checkpoints at boundaries | `DefaultCheckpointManager` | Yes | Yes | No | Yes | SPI |
| `IdempotencyManager` | Protects from duplicate tool executions | `InMemoryIdempotencyManager` | Yes | Yes | Yes | Yes | SPI |
| `MemoryStore` | Core memory persistence API | `InMemoryMemoryStore` | Yes | Yes | Yes | Yes | SPI |
| `EmbeddingModel` | Generates vector embeddings | None (Mocked) | Yes | Yes | No | Yes | SPI |
| `MemoryRetriever` | Searches and filters memory context | `DefaultMemoryRetriever` | Yes | Yes | No | Yes | SPI |
| `ChatModel` | Chat interaction with LLMs | `OpenAIChatModel`, `MockChatModel` | Yes | Yes | No | Yes | SPI |
| `StreamingChatModel` | Supports token streaming | None | No | - | - | No | SPI |
| `Guardrail` | Context and prompt injection security | `PromptInjectionGuardrail` | Yes | Yes | No | Yes | SPI |
| `PermissionManager` | Validates access control for tools | `DefaultPermissionManager` | Yes | Yes | No | Yes | SPI |
| `ApprovalProvider` | Blocking human-in-the-loop validation | Mock / Custom UI | Yes | Yes | No | Yes | SPI |
| `AgentPlugin` | Extensible configuration & capability hook | Various | Yes | Yes | No | Yes | SPI |
| `CostCalculator` | Evaluates token-to-currency costs | `DefaultCostCalculator` | Yes | Yes | No | Yes | SPI |
| `LoopController` | Coordinates execution loop | `DefaultLoopController` | Yes | Yes | No | Yes | INTERNAL |
| `AgentLoop` | Core execution thread boundary | `DefaultAgentLoop` | Yes | Yes | No | Yes | INTERNAL |
| `StateUpdater` | Updates variables and history in state | `DefaultStateUpdater` | Yes | Yes | No | Yes | INTERNAL |
| `FailureClassifier` | Categorizes and wraps errors | `DefaultFailureClassifier` | Yes | Yes | No | Yes | INTERNAL |

---

## 💡 Architectural Categorization Decoupling
* **CORE API:** Intended strictly for third-party application developers to instantiate agents, register standard tools, and run/cancel tasks.
* **SPI (Service Provider Interface):** Intended for integration/plugin developers to write databases (Postgres adapters), model connectors (Anthropic, Gemini), and custom security filters (custom Guardrails).
* **INTERNAL:** Private framework mechanics (like `LoopController` or `AgentLoop`) that should not be visible to users or developers, allowing core redesign without breaking backward compatibility.
