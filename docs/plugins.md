# AgentX Plugin Ecosystem & Developer Guide

## 1. Overview

AgentX provides a zero-core-modification **Plugin Ecosystem** allowing third-party Java developers to extend AgentX capabilities without modifying `agentx-core`, `agentx-api`, or runtime internals.

Plugins are discovered automatically using Java's standard `ServiceLoader` SPI mechanism.

---

## 2. Public SPI Architecture

All third-party plugins depend **strictly on `agentx-api`**.

```
+-------------------------------------------------------------+
|                      AgentX Runtime                         |
|  (PluginManager, DefaultToolRegistry, DefaultPluginContext) |
+-------------------------------------------------------------+
                              |
                     discovers via SPI
                              v
+-------------------------------------------------------------+
|                   com.abhishekraj0.api                      |
| (AgentPlugin, PluginContext, PluginMetadata, ToolRegistry)  |
+-------------------------------------------------------------+
                              ^
                       implements API
                              |
+-------------------------------------------------------------+
|              Third-Party Plugin JAR (External)              |
|        (e.g., agentx-plugin-example-1.0.0-SNAPSHOT.jar)    |
+-------------------------------------------------------------+
```

### Core SPI Interfaces (`agentx-api`)

* `AgentPlugin`: Contract implemented by external plugins.
  ```java
  public interface AgentPlugin {
      PluginMetadata metadata();
      void initialize(PluginContext context);
      void shutdown();
  }
  ```
* `PluginMetadata`: Record defining plugin identity and metadata.
  ```java
  public record PluginMetadata(
      String id,
      String name,
      String version,
      String description,
      String author
  )
  ```
* `PluginContext`: Context provided during initialization giving access to registries (`tools()`, `agents()`, `memory()`, `events()`, `configuration()`).

---

## 3. Plugin Lifecycle & State Machine

Every plugin transitions through deterministic lifecycle states managed by `PluginManager`:

```
DISCOVERED -> VALIDATED -> INITIALIZED -> ACTIVE -> SHUTDOWN
                                   |
                                   +---------> FAILED
```

1. **DISCOVERED**: Plugin class found via `ServiceLoader`.
2. **VALIDATED**: Metadata checked (`id` and `version` non-null, non-blank; duplicate ID checked).
3. **INITIALIZED**: `plugin.initialize(scopedContext)` executed.
4. **ACTIVE**: Plugin tools registered and ready for execution.
5. **SHUTDOWN**: `plugin.shutdown()` called on runtime termination.
6. **FAILED**: Initialization error or unhandled failure recorded.

---

## 4. Failure Isolation & Collision Policy

* **Metadata Validation**: Rejects null/blank IDs or versions (`PluginValidationException`).
* **Duplicate Plugin ID**: Rejects registration if a plugin with the same ID is already loaded (`DuplicatePluginException`).
* **Tool Collision**: Wrapped `PluginToolRegistry` checks for existing tools with identical fully-qualified `ToolId` (namespace + name). If a collision occurs, registration throws `PluginToolCollisionException`.
* **Initialization Fail-Fast**: If a plugin throws during `initialize()`, the state becomes `FAILED`, `PluginFailedEvent` is emitted, and startup fails deterministically.
* **Shutdown Resilience**: Exceptions during `shutdown()` are recorded (`PluginFailedEvent`), but remaining active plugins continue shutting down cleanly.

---

## 5. Security & Trust Model

* **In-Process Trust Model**: Java plugins loaded via `ServiceLoader` run in-process and inherit the JVM's execution context. They are **trusted code**.
* **Security & Reliability Policy Enforcement**: Tools registered by plugins participate fully in normal AgentX execution paths:
  * Permission checks & risk level filters (`ToolRiskLevel`)
  * Human approval requirement checks
  * Cancellation tokens (`CancellationToken`)
  * Idempotency verification & state store checks
  * Secret redaction on inputs/outputs
  * Telemetry & EventBus observability

> **Note**: For process-level sandbox isolation, use MCP (Model Context Protocol) remote servers rather than JVM SPI plugins.

---

## 6. Clean-Room External Developer Guide

### Step 1: Add `agentx-api` Dependency

In your external Maven project `pom.xml`:

```xml
<dependency>
    <groupId>com.abhishekraj0</groupId>
    <artifactId>agentx-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Step 2: Implement `AgentPlugin` & `AgentTool`

```java
package com.myorg.plugin;

import com.abhishekraj0.api.plugin.*;
import com.abhishekraj0.api.tool.*;
import java.util.List;
import java.util.Map;

public class MathPlugin implements AgentPlugin {

    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata("math-plugin", "Math Plugin", "1.0.0", "Provides math tools", "Dev");
    }

    @Override
    public void initialize(PluginContext context) {
        context.tools().register(new AgentTool() {
            @Override public ToolId id() { return new ToolId("math", "add"); }
            @Override public String description() { return "Adds two numbers"; }
            @Override public ToolSchema inputSchema() { return new ToolSchema("object", List.of()); }
            @Override public ToolResult execute(ToolContext ctx) {
                double a = Double.parseDouble(String.valueOf(ctx.arguments().getOrDefault("a", 0)));
                double b = Double.parseDouble(String.valueOf(ctx.arguments().getOrDefault("b", 0)));
                return ToolResult.success(String.valueOf(a + b));
            }
        });
    }

    @Override
    public void shutdown() {}
}
```

### Step 3: Register SPI Descriptor

Create file `src/main/resources/META-INF/services/com.abhishekraj0.api.plugin.AgentPlugin`:

```text
com.myorg.plugin.MathPlugin
```

### Step 4: Package & Deploy

Run `mvn clean package`. Place the generated JAR on the AgentX application classpath. `PluginManager` will automatically discover and initialize your plugin.
