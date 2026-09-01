# AgentX Durable Execution Architecture

## Overview

AgentX guarantees durable, fault-tolerant agent execution across process crashes, restarts, and distributed instances.

### Key Guarantees

1. **State Snapshot Persistence**: Snapshots saved before and after state transitions.
2. **Pending Idempotency Records**: Tool execution requests marked `PENDING` in persistent storage prior to tool invocation.
3. **Atomic Ownership Claims**: Concurrent resume attempts atomically acquire ownership via version optimistic locking.
4. **Secret Redaction**: Sensitive parameters in snapshots and idempotency stores automatically redacted.
