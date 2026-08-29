package com.abhishekraj0.api.memory;

import java.util.UUID;

/**
 * Unique identifier wrapper for Memories.
 */
public record MemoryId(UUID id) {
    public static MemoryId random() {
        return new MemoryId(UUID.randomUUID());
    }
}
