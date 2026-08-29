package com.abhishekraj0.api.agent;

import java.time.Duration;
import java.util.Map;

/**
 * Options for configuring agent execution.
 */
public record AgentOptions(
        int maxIterations,
        int maxToolCalls,
        Duration timeout,
        Double temperature,
        Map<String, Object> additionalOptions
) {
    /**
     * Creates default agent options.
     *
     * @return the default agent options
     */
    public static AgentOptions defaultOptions() {
        return new AgentOptions(10, 20, Duration.ofMinutes(5), 0.7, Map.of());
    }
}
