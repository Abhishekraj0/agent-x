package com.abhishekraj0.api.memory;

import java.util.List;

/**
 * Interface representing a component that can store, query, and delete memories.
 */
public interface MemoryStore {

    /**
     * Saves a memory.
     *
     * @param memory the memory to save
     */
    void save(Memory memory);

    /**
     * Searches memories based on the query constraints.
     *
     * @param query the query details
     * @return matching list of memories
     */
    List<Memory> search(MemoryQuery query);

    /**
     * Deletes a memory by ID.
     *
     * @param id the memory ID
     */
    void delete(MemoryId id);

    /**
     * Clears all stored memories.
     */
    void clear();
}
