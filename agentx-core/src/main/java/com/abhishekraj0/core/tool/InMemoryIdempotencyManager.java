package com.abhishekraj0.core.tool;

import com.abhishekraj0.api.tool.IdempotencyDecision;
import com.abhishekraj0.api.tool.IdempotencyManager;
import com.abhishekraj0.api.tool.ToolExecutionRequest;
import com.abhishekraj0.api.tool.ToolExecutionResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe memory-based IdempotencyManager.
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
            return IdempotencyDecision.useCached(cached.output(), cached.success(), cached.errorMessage());
        }
        return IdempotencyDecision.executeNew();
    }

    @Override
    public void record(ToolExecutionResult result) {
        if (result != null && result.idempotencyKey() != null) {
            cache.put(result.idempotencyKey(), result);
        }
    }
}
