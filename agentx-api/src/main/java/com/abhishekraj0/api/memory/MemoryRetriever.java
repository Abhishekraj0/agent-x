package com.abhishekraj0.api.memory;

import java.util.List;

/**
 * Interface responsible for retrieving memories relevant to a query.
 */
public interface MemoryRetriever {

    /**
     * Retrieves memories based on the query constraints.
     *
     * @param query the query details
     * @return the list of relevant memories
     */
    List<Memory> retrieve(MemoryQuery query);
}
