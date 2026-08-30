package com.abhishekraj0.core.failure;

import com.abhishekraj0.api.failure.AgentFailure;
import com.abhishekraj0.api.failure.FailureContext;
import com.abhishekraj0.api.failure.FailureType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.security.AccessControlException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

public class FailureClassifierTest {

    private final DefaultFailureClassifier classifier = new DefaultFailureClassifier();
    private final FailureContext context = FailureContext.of("exec-123", "testStep", Map.of("meta", "value"));

    @Test
    public void testClassifyCancellation() {
        Throwable t = new InterruptedException("Sleep interrupted");
        AgentFailure failure = classifier.classify(t, context);

        assertEquals(FailureType.CANCELLATION, failure.getType());
        assertEquals("EXECUTION_CANCELLED", failure.getCode());
        assertEquals("Sleep interrupted", failure.getMessage());
        assertFalse(failure.isRetryable());
        assertEquals("exec-123", failure.getExecutionId());
        assertEquals("value", failure.getMetadata().get("meta"));
    }

    @Test
    public void testClassifyTimeout() {
        Throwable t1 = new SocketTimeoutException("Read timed out");
        AgentFailure f1 = classifier.classify(t1, context);
        assertEquals(FailureType.TIMEOUT, f1.getType());
        assertTrue(f1.isRetryable());

        Throwable t2 = new TimeoutException("Future timeout");
        AgentFailure f2 = classifier.classify(t2, context);
        assertEquals(FailureType.TIMEOUT, f2.getType());
        assertTrue(f2.isRetryable());
    }

    @Test
    public void testClassifyPermissionAndAuthorization() {
        Throwable t1 = new AccessControlException("Access denied");
        AgentFailure f1 = classifier.classify(t1, context);
        assertEquals(FailureType.SECURITY_FAILURE, f1.getType());
        assertEquals("UNAUTHORIZED", f1.getCode());
        assertFalse(f1.isRetryable());

        Throwable t2 = new SecurityException("Permission denied to read file");
        AgentFailure f2 = classifier.classify(t2, context);
        assertEquals(FailureType.SECURITY_FAILURE, f2.getType());
        assertEquals("PERMISSION_DENIED", f2.getCode());
        assertFalse(f2.isRetryable());
    }

    @Test
    public void testClassifyRateLimit() {
        Throwable t = new RuntimeException("HTTP 429 Too Many Requests");
        AgentFailure failure = classifier.classify(t, context);
        assertEquals(FailureType.MODEL_FAILURE, failure.getType());
        assertEquals("RATE_LIMIT_EXCEEDED", failure.getCode());
        assertTrue(failure.isRetryable());
    }

    @Test
    public void testClassifyNetworkFailure() {
        Throwable t = new ConnectException("Connection refused");
        AgentFailure failure = classifier.classify(t, context);
        assertEquals(FailureType.AGENT_FAILURE, failure.getType());
        assertEquals("NETWORK_FAILURE", failure.getCode());
        assertTrue(failure.isRetryable());
    }

    @Test
    public void testClassifyValidationAndInvalidState() {
        Throwable t = new IllegalArgumentException("Invalid parameter value");
        AgentFailure failure = classifier.classify(t, context);
        assertEquals(FailureType.INVALID_STATE, failure.getType());
        assertEquals("INVALID_STATE_OR_ARGUMENT", failure.getCode());
        assertFalse(failure.isRetryable());
    }

    @Test
    public void testClassifyFallbackStepNames() {
        FailureContext toolContext = FailureContext.of("exec-123", "ExecuteToolStep");
        Throwable t1 = new RuntimeException("Generic error");
        AgentFailure f1 = classifier.classify(t1, toolContext);
        assertEquals(FailureType.TOOL_FAILURE, f1.getType());
        assertEquals("TOOL_ERROR", f1.getCode());

        FailureContext modelContext = FailureContext.of("exec-123", "InvokeModelStep");
        AgentFailure f2 = classifier.classify(t1, modelContext);
        assertEquals(FailureType.MODEL_FAILURE, f2.getType());
        assertEquals("MODEL_ERROR", f2.getCode());
    }
}
