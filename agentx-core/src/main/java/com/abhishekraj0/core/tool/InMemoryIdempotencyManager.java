package com.abhishekraj0.core.tool;

import com.abhishekraj0.api.security.SecretRedactor;
import com.abhishekraj0.api.tool.IdempotencyDecision;
import com.abhishekraj0.api.tool.IdempotencyManager;
import com.abhishekraj0.api.tool.ToolExecutionRequest;
import com.abhishekraj0.api.tool.ToolExecutionResult;
import com.abhishekraj0.core.security.DefaultSecretRedactor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe memory-based IdempotencyManager with pending state tracking and secret redaction.
 */
public class InMemoryIdempotencyManager implements IdempotencyManager {

    private final Map<String, ToolExecutionResult> cache = new ConcurrentHashMap<>();
    private final SecretRedactor secretRedactor;

    public InMemoryIdempotencyManager() {
        this(new DefaultSecretRedactor());
    }

    public InMemoryIdempotencyManager(SecretRedactor secretRedactor) {
        this.secretRedactor = secretRedactor != null ? secretRedactor : text -> text;
    }

    @Override
    public IdempotencyDecision check(ToolExecutionRequest request) {
        if (request == null || request.idempotencyKey() == null) {
            return IdempotencyDecision.executeNew();
        }
        String cleanKey = secretRedactor.redact(request.idempotencyKey());
        ToolExecutionResult cached = cache.get(cleanKey);
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
            String cleanKey = secretRedactor.redact(request.idempotencyKey());
            cache.putIfAbsent(cleanKey, new ToolExecutionResult(
                    request.executionId(),
                    request.toolCallId(),
                    request.toolId(),
                    cleanKey,
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
            String cleanKey = secretRedactor.redact(result.idempotencyKey());
            String cleanOutput = secretRedactor.redact(result.output());
            String cleanError = secretRedactor.redact(result.errorMessage());

            ToolExecutionResult cleanResult = new ToolExecutionResult(
                    result.executionId(),
                    result.toolCallId(),
                    result.toolId(),
                    cleanKey,
                    result.success(),
                    cleanOutput,
                    cleanError,
                    result.completedAt(),
                    result.status()
            );
            cache.put(cleanKey, cleanResult);
        }
    }
}
