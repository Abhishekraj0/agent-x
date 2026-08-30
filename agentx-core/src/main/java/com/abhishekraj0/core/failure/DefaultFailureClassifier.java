package com.abhishekraj0.core.failure;

import com.abhishekraj0.api.failure.AgentFailure;
import com.abhishekraj0.api.failure.FailureClassifier;
import com.abhishekraj0.api.failure.FailureContext;
import com.abhishekraj0.api.failure.FailureType;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.util.concurrent.TimeoutException;

/**
 * Default implementation of FailureClassifier to map throwables to structured AgentFailure.
 */
public class DefaultFailureClassifier implements FailureClassifier {

    @Override
    public AgentFailure classify(Throwable throwable, FailureContext context) {
        if (throwable == null) {
            return new AgentFailure(
                FailureType.AGENT_FAILURE,
                "UNKNOWN_ERROR",
                "Null throwable provided to classifier",
                false,
                context.executionId(),
                null,
                context.metadata()
            );
        }

        if (throwable instanceof AgentFailure) {
            return (AgentFailure) throwable;
        }

        String msg = throwable.getMessage();
        if (msg == null) {
            msg = "";
        }
        String lowerMsg = msg.toLowerCase();

        // 1. Cancellation
        if (throwable instanceof InterruptedException || 
            lowerMsg.contains("cancellation") || 
            lowerMsg.contains("cancelled") || 
            lowerMsg.contains("cancel")) {
            return new AgentFailure(
                FailureType.CANCELLATION,
                "EXECUTION_CANCELLED",
                throwable.getMessage() != null ? throwable.getMessage() : "Execution was cancelled",
                false,
                context.executionId(),
                throwable,
                context.metadata()
            );
        }

        // 2. Timeout
        if (throwable instanceof SocketTimeoutException || 
            throwable instanceof TimeoutException || 
            lowerMsg.contains("timeout") || 
            lowerMsg.contains("timed out")) {
            return new AgentFailure(
                FailureType.TIMEOUT,
                "TIMEOUT_ERROR",
                throwable.getMessage() != null ? throwable.getMessage() : "Operation timed out",
                true,
                context.executionId(),
                throwable,
                context.metadata()
            );
        }

        // 3. Permission failure / Authorization failure
        if (throwable instanceof SecurityException || 
            throwable instanceof AccessControlException || 
            lowerMsg.contains("permission") || 
            lowerMsg.contains("unauthorized") || 
            lowerMsg.contains("authorization") || 
            lowerMsg.contains("forbidden") ||
            lowerMsg.contains("access denied")) {
            return new AgentFailure(
                FailureType.SECURITY_FAILURE,
                lowerMsg.contains("permission") ? "PERMISSION_DENIED" : "UNAUTHORIZED",
                throwable.getMessage() != null ? throwable.getMessage() : "Access denied",
                false,
                context.executionId(),
                throwable,
                context.metadata()
            );
        }

        // 4. Rate Limit
        if (lowerMsg.contains("rate limit") || 
            lowerMsg.contains("429") || 
            lowerMsg.contains("too many requests")) {
            return new AgentFailure(
                FailureType.MODEL_FAILURE,
                "RATE_LIMIT_EXCEEDED",
                throwable.getMessage() != null ? throwable.getMessage() : "Rate limit exceeded",
                true,
                context.executionId(),
                throwable,
                context.metadata()
            );
        }

        // 5. Network failure
        if (throwable instanceof ConnectException || 
            throwable instanceof UnknownHostException || 
            throwable instanceof IOException ||
            lowerMsg.contains("network") || 
            lowerMsg.contains("connection refused") || 
            lowerMsg.contains("connect timed out")) {
            return new AgentFailure(
                FailureType.AGENT_FAILURE,
                "NETWORK_FAILURE",
                throwable.getMessage() != null ? throwable.getMessage() : "Network failure",
                true,
                context.executionId(),
                throwable,
                context.metadata()
            );
        }

        // 6. Validation error / Invalid State
        if (throwable instanceof IllegalArgumentException || 
            throwable instanceof IllegalStateException || 
            lowerMsg.contains("validation") || 
            lowerMsg.contains("invalid state") ||
            lowerMsg.contains("invalid argument")) {
            return new AgentFailure(
                FailureType.INVALID_STATE,
                "INVALID_STATE_OR_ARGUMENT",
                throwable.getMessage() != null ? throwable.getMessage() : "Invalid state or argument",
                false,
                context.executionId(),
                throwable,
                context.metadata()
            );
        }

        // 7. StepName fallback
        String step = context.stepName() != null ? context.stepName().toLowerCase() : "";
        if (step.contains("tool") || lowerMsg.contains("tool")) {
            return new AgentFailure(
                FailureType.TOOL_FAILURE,
                "TOOL_ERROR",
                throwable.getMessage() != null ? throwable.getMessage() : "Tool execution failed",
                true,
                context.executionId(),
                throwable,
                context.metadata()
            );
        }

        if (step.contains("model") || step.contains("chat") || lowerMsg.contains("model") || lowerMsg.contains("llm")) {
            return new AgentFailure(
                FailureType.MODEL_FAILURE,
                "MODEL_ERROR",
                throwable.getMessage() != null ? throwable.getMessage() : "Model execution failed",
                true,
                context.executionId(),
                throwable,
                context.metadata()
            );
        }

        return new AgentFailure(
            FailureType.AGENT_FAILURE,
            "AGENT_INTERNAL_ERROR",
            throwable.getMessage() != null ? throwable.getMessage() : "Internal agent failure",
            false,
            context.executionId(),
            throwable,
            context.metadata()
        );
    }
}
