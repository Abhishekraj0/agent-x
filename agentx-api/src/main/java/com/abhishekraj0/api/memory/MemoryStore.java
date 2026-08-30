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
     * Saves a memory with an explicit scope and scope ID.
     */
    default void save(Memory memory, MemoryScope scope, String scopeId) {
        java.util.Map<String, Object> additional = new java.util.HashMap<>(
            memory.metadata() != null && memory.metadata().additionalMetadata() != null 
            ? memory.metadata().additionalMetadata() 
            : java.util.Map.of()
        );
        additional.put("scope", scope.name());
        if (scopeId != null) {
            additional.put("scopeId", scopeId);
        }
        MemoryMetadata newMeta = new MemoryMetadata(
            memory.metadata() != null ? memory.metadata().createdAt() : java.time.Instant.now(),
            java.util.Collections.unmodifiableMap(additional)
        );
        Memory scopedMemory = new Memory(memory.id(), memory.content(), memory.type(), newMeta);
        save(scopedMemory);
    }

    /**
     * Searches memories based on the query constraints.
     *
     * @param query the query details
     * @return matching list of memories
     */
    List<Memory> search(MemoryQuery query);

    /**
     * Searches memories with an explicit scope and scope ID.
     */
    default List<Memory> search(MemoryQuery query, MemoryScope scope, String scopeId) {
        java.util.Map<String, Object> newFilter = new java.util.HashMap<>(
            query.filter() != null ? query.filter() : java.util.Map.of()
        );
        newFilter.put("scope", scope.name());
        if (scopeId != null) {
            newFilter.put("scopeId", scopeId);
        }
        MemoryQuery scopedQuery = new MemoryQuery(
            query.queryText(), 
            query.type(), 
            query.maxResults(), 
            java.util.Collections.unmodifiableMap(newFilter)
        );
        return search(scopedQuery);
    }

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
