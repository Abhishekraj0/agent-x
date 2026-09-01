# AgentX Tool Development Guide

## Configuring Tool Metadata & Safety Properties

When defining custom tools in AgentX, specify appropriate safety properties in `ToolMetadata`:

```java
ToolMetadata metadata = new ToolMetadata(
    RiskLevel.HIGH,          // Risk level
    false,                   // Requires approval before execution
    false,                   // Read-only tool
    false,                   // Idempotent
    false,                   // Safe after unknown result (e.g. crash)
    Duration.ofSeconds(30)   // Tool timeout
);
```

### Safety Rules

1. **Read Operations** (`readOnly = true`): Set `idempotent = true` and `safeAfterUnknownResult = true`.
2. **Financial / External Side Effects**: Set `idempotent = false` and `safeAfterUnknownResult = false` unless the external API provider enforces idempotency via request keys.
