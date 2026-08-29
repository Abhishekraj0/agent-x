package com.abhishekraj0.api.agent;

import java.time.Duration;

/**
 * Structured configuration defining the timeout hierarchy for AgentX runtime.
 */
public record TimeoutHierarchy(
        Duration executionTimeout,
        Duration modelTimeout,
        Duration toolTimeout,
        Duration workflowTimeout,
        Duration delegationTimeout,
        Duration approvalTimeout
) {
    public static TimeoutHierarchy from(AgentOptions options) {
        Duration global = options.timeout() != null ? options.timeout() : Duration.ofMinutes(5);
        Duration model = getDuration(options, "modelTimeout", Duration.ofSeconds(30));
        Duration tool = getDuration(options, "toolTimeout", Duration.ofSeconds(10));
        Duration workflow = getDuration(options, "workflowTimeout", Duration.ofMinutes(2));
        Duration delegation = getDuration(options, "delegationTimeout", Duration.ofMinutes(1));
        Duration approval = getDuration(options, "approvalTimeout", Duration.ofMinutes(30));
        return new TimeoutHierarchy(global, model, tool, workflow, delegation, approval);
    }

    private static Duration getDuration(AgentOptions options, String key, Duration defaultValue) {
        if (options.additionalOptions() != null) {
            Object val = options.additionalOptions().get(key);
            if (val instanceof Duration d) {
                return d;
            }
            if (val instanceof Number n) {
                return Duration.ofMillis(n.longValue());
            }
        }
        return defaultValue;
    }
}
