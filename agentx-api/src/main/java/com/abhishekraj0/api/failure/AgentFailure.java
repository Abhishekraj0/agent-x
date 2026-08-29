package com.abhishekraj0.api.failure;

import java.io.Serializable;
import java.time.Instant;

/**
 * Standardized runtime exception for failures across the AgentX system.
 */
public class AgentFailure extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 1L;

    private final FailureType type;
    private final String code;
    private final String message;
    private final boolean retryable;
    private final String executionId;
    private final Instant timestamp;

    public AgentFailure(FailureType type, String code, String message, boolean retryable, String executionId, Throwable cause) {
        super(message, cause);
        this.type = type;
        this.code = code;
        this.message = message;
        this.retryable = retryable;
        this.executionId = executionId;
        this.timestamp = Instant.now();
    }

    public FailureType getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getExecutionId() {
        return executionId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
