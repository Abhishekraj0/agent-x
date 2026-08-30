package com.abhishekraj0.core.memory;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.memory.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class MemoryScopeIsolationTest {

    @Test
    public void testExecutionIsolationDefault() {
        InMemoryVectorMemoryStore store = new InMemoryVectorMemoryStore();

        String execIdA = "exec-A-" + UUID.randomUUID();
        String execIdB = "exec-B-" + UUID.randomUUID();

        // 1. In Execution A, store memory
        MemoryContextHolder.setExecutionId(execIdA);
        try {
            Memory memory = new Memory(
                    new MemoryId(UUID.randomUUID()),
                    "Customer = Alice",
                    "SEMANTIC",
                    new MemoryMetadata(Instant.now(), Map.of())
            );
            store.save(memory);

            // Verify execution A can read it
            List<Memory> resultsA = store.search(MemoryQuery.forText("Alice", 10));
            assertEquals(1, resultsA.size());
            assertEquals("Customer = Alice", resultsA.get(0).content());
        } finally {
            MemoryContextHolder.clearExecutionId();
        }

        // 2. In Execution B, search memory
        MemoryContextHolder.setExecutionId(execIdB);
        try {
            List<Memory> resultsB = store.search(MemoryQuery.forText("Alice", 10));
            assertTrue(resultsB.isEmpty(), "Execution B should not access Execution A memory under default isolation");
        } finally {
            MemoryContextHolder.clearExecutionId();
        }
    }

    @Test
    public void testExplicitSessionScopeSharing() {
        InMemoryVectorMemoryStore store = new InMemoryVectorMemoryStore();

        String execIdA = "exec-A-" + UUID.randomUUID();
        String execIdB = "exec-B-" + UUID.randomUUID();
        String sharedSessionId = "session-shared-" + UUID.randomUUID();

        // 1. Execution A stores memory with SESSION scope
        MemoryContextHolder.setExecutionId(execIdA);
        try {
            Memory memory = new Memory(
                    new MemoryId(UUID.randomUUID()),
                    "Customer = Alice",
                    "SEMANTIC",
                    new MemoryMetadata(Instant.now(), Map.of())
            );
            // Explicitly store with SESSION scope
            store.save(memory, MemoryScope.SESSION, sharedSessionId);
        } finally {
            MemoryContextHolder.clearExecutionId();
        }

        // 2. Execution B searches memory with SESSION scope
        MemoryContextHolder.setExecutionId(execIdB);
        try {
            // Under default EXECUTION scope, B shouldn't find it
            List<Memory> resultsDefault = store.search(MemoryQuery.forText("Alice", 10));
            assertTrue(resultsDefault.isEmpty());

            // But querying explicitly with session scope finds it
            List<Memory> resultsSession = store.search(MemoryQuery.forText("Alice", 10), MemoryScope.SESSION, sharedSessionId);
            assertEquals(1, resultsSession.size(), "Execution B should find shared session memory");
            assertEquals("Customer = Alice", resultsSession.get(0).content());
        } finally {
            MemoryContextHolder.clearExecutionId();
        }
    }
}
