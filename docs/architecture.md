# AgentX — Target Architecture

AgentX utilizes a decoupled, interface-first architecture in Java 21+ built around core modules.

```
                  ┌────────────────────────────────────────┐
                  │              AgentRuntime              │
                  └───────────────────┬────────────────────┘
                                      │
                                      ▼
                  ┌────────────────────────────────────────┐
                  │               AgentLoop                │
                  └────────┬──────────┬──────────┬─────────┘
                           │          │          │
                           ▼          ▼          ▼
                      ┌────────┐ ┌────────┐ ┌─────────┐
                      │Planner │ │Memory  │ │Guardrail│
                      └────────┘ └────────┘ └─────────┘
```

## Architectural Design Principles

1. **Decoupled API vs. Core**: The `agentx-api` module defines the structural records, interfaces, and options. The `agentx-core` module implements these using standard JDK patterns.
2. **Composition**: The runtime execution relies on composable, replaceable engine blocks injected via builder options.
3. **Optional Spring Boot Support**: Dependency injection is handled programmatically in core; `agentx-spring` bridges tools and plugins to Spring application contexts automatically.
