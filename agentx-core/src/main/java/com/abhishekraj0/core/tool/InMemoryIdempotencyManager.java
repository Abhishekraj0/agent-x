package com.abhishekraj0.core.tool;

import com.abhishekraj0.api.tool.IdempotencyDecision;
import com.abhishekraj0.api.tool.IdempotencyManager;
import com.abhishekraj0.api.tool.ToolExecutionRequest;
import com.abhishekraj0.api.tool.ToolExecutionResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe memory-based IdempotencyManager with pending state tracking.
 */
public class InMemoryIdempotencyManager implements IdempotencyManager {

    private final Map<String, ToolExecutionResult> cache = new ConcurrentHashMap<>();

    @Override
    public IdempotencyDecision check(ToolExecutionRequest request) {
        if (request == null || request.idempotencyKey() == null) {
            return IdempotencyDecision.executeNew();
        }
        ToolExecutionResult cached = cache.get(request.idempotencyKey());
        if (cached != null) {
            if ("PENDING".equals(cached.status())) {
                return IdempotencyDecision.unknownResult("Tool execution outcome is unknown due to prior interruption or unconfirmed completion");
            }
            return IdempotencyDecision.useCached(cached.output(), cached.success(), cached.errorMessage());
        }
        return IdempotencyDecision.executeNew();
    }

    @Override
    public void recordPending(ToolExecutionRequest request) {
        if (request != null && request.idempotencyKey() != null) {
            cache.putIfAbsent(request.idempotencyKey(), new ToolExecutionResult(
                    request.executionId(),
                    request.toolCallId(),
                    request.toolId(),
                    request.idempotencyKey(),
                    false,
                    null,
                    "Tool execution in progress",
                    request.startedAt(),
                    "PENDING"
            ));
        }
    }

    @Override
    public void record(ToolExecutionResult result) {
        if (result != null && result.idempotencyKey() != null) {
            cache.put(result.idempotencyKey(), result);
        }
    }
}
