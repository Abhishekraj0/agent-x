package com.abhishekraj0.core.model;

import com.abhishekraj0.api.loop.RetryDecision;
import com.abhishekraj0.api.model.*;
import com.abhishekraj0.api.security.CircuitBreaker;
import com.abhishekraj0.api.security.Permission;
import java.util.Optional;

/**
 * ChatModel implementation wrapping standard ChatModels to provide fault tolerance,
 * circuit breakers, retries, and fallbacks.
 */
public class ReliableChatModel implements ChatModel {

    private final ChatModel primaryModel;
    private final CircuitBreaker circuitBreaker;
    private final ModelRouter modelRouter;
    private final ModelRetryStrategy retryStrategy;
    private final ModelFallbackStrategy fallbackStrategy;

    public ReliableChatModel(
            ChatModel primaryModel,
            CircuitBreaker circuitBreaker,
            ModelRouter modelRouter,
            ModelRetryStrategy retryStrategy,
            ModelFallbackStrategy fallbackStrategy
    ) {
        this.primaryModel = primaryModel;
        this.circuitBreaker = circuitBreaker;
        this.modelRouter = modelRouter;
        this.retryStrategy = retryStrategy;
        this.fallbackStrategy = fallbackStrategy;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        if (circuitBreaker != null) {
            Permission perm = circuitBreaker.acquire();
            if (!perm.isAllowed()) {
                if (fallbackStrategy != null) {
                    Optional<ChatModel> fallback = fallbackStrategy.getFallback(primaryModel, new RuntimeException("Circuit open"));
                    if (fallback.isPresent()) {
                        return fallback.get().chat(request);
                    }
                }
                throw new RuntimeException("Circuit open and no fallback available");
            }
        }

        ChatModel activeModel = primaryModel;
        int attempt = 1;
        while (true) {
            try {
                ChatResponse response = activeModel.chat(request);
                if (circuitBreaker != null) {
                    circuitBreaker.recordSuccess();
                }
                return response;
            } catch (Exception e) {
                if (circuitBreaker != null) {
                    circuitBreaker.recordFailure(e);
                }

                if (retryStrategy != null) {
                    RetryDecision decision = retryStrategy.shouldRetry(e, attempt);
                    if (decision.shouldRetry()) {
                        attempt++;
                        if (decision.delay() != null && !decision.delay().isZero()) {
                            try {
                                Thread.sleep(decision.delay().toMillis());
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        continue;
                    }
                }

                if (fallbackStrategy != null) {
                    Optional<ChatModel> fallback = fallbackStrategy.getFallback(activeModel, e);
                    if (fallback.isPresent()) {
                        activeModel = fallback.get();
                        attempt = 1;
                        continue;
                    }
                }

                throw e;
            }
        }
    }

    @Override
    public ModelMetadata metadata() {
        return primaryModel.metadata();
    }
}
