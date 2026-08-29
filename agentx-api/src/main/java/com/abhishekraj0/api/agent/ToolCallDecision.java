package com.abhishekraj0.api.agent;

import com.abhishekraj0.api.model.ToolCall;
import java.util.List;

/**
 * Decision to execute one or more tools.
 */
public record ToolCallDecision(
        String decisionId,
        String reason,
        List<ToolCall> toolCalls
) implements AgentDecision {}
