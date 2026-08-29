# Agent Runtime

The `DefaultAgentRuntime` handles concurrent executions, cancellation triggers, and transaction management for active runs.

## Core API
```java
public interface AgentRuntime {
    AgentResponse execute(AgentRequest request);
    void cancel(String executionId);
    AgentExecution getExecution(String executionId);
}
```

## Execution States
Executions transition through standard state values:
* `PENDING`
* `RUNNING`
* `COMPLETED`
* `FAILED`
* `CANCELLED`
