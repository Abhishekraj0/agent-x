package com.abhishekraj0.api.memory;

/**
 * ThreadLocal context holder to propagate execution and session IDs to memory operations.
 */
public final class MemoryContextHolder {

    private static final ThreadLocal<String> currentExecutionId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentSessionId = new ThreadLocal<>();

    private MemoryContextHolder() {}

    public static void setExecutionId(String executionId) {
        currentExecutionId.set(executionId);
    }

    public static String getExecutionId() {
        return currentExecutionId.get();
    }

    public static void clearExecutionId() {
        currentExecutionId.remove();
    }

    public static void setSessionId(String sessionId) {
        currentSessionId.set(sessionId);
    }

    public static String getSessionId() {
        return currentSessionId.get();
    }

    public static void clearSessionId() {
        currentSessionId.remove();
    }

    public static void clear() {
        currentExecutionId.remove();
        currentSessionId.remove();
    }
}
