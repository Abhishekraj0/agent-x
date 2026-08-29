package com.abhishekraj0.api.tool;

/**
 * Query criteria for searching tools in a registry.
 */
public record ToolQuery(
        String searchTerm,
        RiskLevel maxRiskLevel,
        Boolean readOnly
) {
    public static ToolQuery all() {
        return new ToolQuery(null, null, null);
    }
}
