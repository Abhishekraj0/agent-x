package com.abhishekraj0.api.failure;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

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
    private final Map<String, String> metadata;

    public AgentFailure(FailureType type, String code, String message, boolean retryable, String executionId, Throwable cause) {
        this(type, code, message, retryable, executionId, cause, java.util.Map.of());
    }

    public AgentFailure(FailureType type, String code, String message, boolean retryable, String executionId, Throwable cause, java.util.Map<String, String> metadata) {
        super(message, cause);
        this.type = type;
        this.code = code;
        this.message = message;
        this.retryable = retryable;
        this.executionId = executionId;
        this.timestamp = Instant.now();
        this.metadata = metadata != null ? java.util.Map.copyOf(metadata) : java.util.Map.of();
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

    public java.util.Map<String, String> getMetadata() {
        return metadata;
    }
}
