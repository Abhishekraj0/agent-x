package com.abhishekraj0.api.tool;

/**
 * Classifies categories of errors that can occur during tool execution.
 */
public enum ToolErrorType {
    VALIDATION,
    TIMEOUT,
    RATE_LIMIT,
    AUTHENTICATION,
    AUTHORIZATION,
    NOT_FOUND,
    TRANSIENT,
    PERMANENT,
    UNKNOWN
}
