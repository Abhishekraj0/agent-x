package com.abhishekraj0.api.agent;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Representation of an Agent checkpoint at specific execution boundaries.
 */
public record AgentCheckpoint(
        String checkpointId,
        String executionId,
        AgentState state,
        Instant timestamp,
        Map<String, Object> metadata
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
