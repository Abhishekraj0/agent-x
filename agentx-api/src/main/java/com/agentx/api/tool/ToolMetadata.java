package com.agentx.api.tool;

import java.time.Duration;

/**
 * Metadata specifying configuration constraints and security/approval policies for a tool.
 */
public record ToolMetadata(
        RiskLevel riskLevel,
        boolean requiresApproval,
        boolean readOnly,
        boolean idempotent,
        Duration timeout
) {
    public static ToolMetadata defaultMetadata() {
        return new ToolMetadata(RiskLevel.LOW, false, false, false, Duration.ofSeconds(30));
    }
}
