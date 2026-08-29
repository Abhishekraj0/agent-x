package com.abhishekraj0.api.memory;

import java.time.Instant;
import java.util.Map;

/**
 * Metadata associated with an individual stored Memory.
 */
public record MemoryMetadata(
        Instant createdAt,
        Map<String, Object> additionalMetadata
) {
    public static MemoryMetadata now() {
        return new MemoryMetadata(Instant.now(), Map.of());
    }
}
