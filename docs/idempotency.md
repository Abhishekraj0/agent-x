# AgentX Tool Idempotency & Safety Policy Architecture

## 1. Core Definitions

AgentX distinguishes between three distinct tool execution safety concepts:

- **Idempotent (`idempotent`)**: Repeated execution with identical parameters produces the same intended state.
- **Retryable (`retryable`)**: The operation may be attempted again after a known failure (e.g. temporary network error).
- **Safe After Unknown Result (`safeAfterUnknownResult`)**: The operation may be retried even when a previous attempt was interrupted mid-flight and its outcome is unknown (e.g., read operations, status queries, or operations with external provider idempotency keys).

> [!IMPORTANT]
> **No Universal EXACTLY-ONCE Claim**: AgentX provides durable idempotency coordination and duplicate-prevention within the framework. Exactly-once external side effects require cooperation from the target external system (e.g., passing idempotency keys to external APIs).

---

## 2. Unknown Result Recovery Policies

When process crashes or network disconnects occur during tool execution:

1. **`FAIL_SAFE` (Default)**: Immediately halts re-execution of non-idempotent/unsafe tools and raises `AgentFailure` (`UNKNOWN_TOOL_RESULT`).
2. **`REQUIRE_APPROVAL`**: Suspends agent loop and transitions state to `WAITING_APPROVAL` with an `AskUserDecision`.
3. **`RETRY`**: Re-executes tool **ONLY** if `safeAfterUnknownResult == true` or explicit operator authorization is provided.

---

## 3. Human Operator Resolution API (`UnknownResultResolution`)

Operators can resolve `WAITING_APPROVAL` states without forcing tool re-execution:

- `CONFIRMED_SUCCESS(output, reason)`: Marks execution as completed with verified output without re-invoking the tool.
- `CONFIRMED_FAILURE(reason)`: Marks execution as failed without re-invoking the tool.
- `RETRY_AUTHORIZED(reason)`: Authorizes safe re-execution of the tool.
