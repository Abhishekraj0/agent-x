package com.abhishekraj0.api.memory;

/**
 * Represents a single piece of information stored in memory.
 */
public record Memory(
        MemoryId id,
        String content,
        String type, // e.g. WORKING, CONVERSATION, SEMANTIC, EPISODIC
        MemoryMetadata metadata
) {}
