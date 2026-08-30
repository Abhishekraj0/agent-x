package com.abhishekraj0.api.context;

import com.abhishekraj0.api.agent.CancellationToken;
import java.io.Serializable;
import java.util.Map;

/**
 * Context for a specific execution run, linking it to a cancellation token.
 */
public record ExecutionContext(
    String executionId,
    CancellationToken cancellationToken,
    Map<String, Object> metadata
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public ExecutionContext(String executionId, CancellationToken cancellationToken) {
        this(executionId, cancellationToken, Map.of());
    }
}
