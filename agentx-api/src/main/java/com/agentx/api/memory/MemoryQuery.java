package com.agentx.api.memory;

import java.util.Map;

/**
 * Encapsulates parameters used to query or search the memory system.
 */
public record MemoryQuery(
        String queryText,
        String type,
        int maxResults,
        Map<String, Object> filter
) {
    public static MemoryQuery forText(String text, int maxResults) {
        return new MemoryQuery(text, null, maxResults, Map.of());
    }
}
