package com.agentx.api.workflow;

import java.time.Instant;
import java.util.Map;

/**
 * Context describing an external event that could trigger a task.
 */
public record TriggerContext(
        String triggerId,
        String eventType,
        Instant timestamp,
        Map<String, Object> payload
) {}
