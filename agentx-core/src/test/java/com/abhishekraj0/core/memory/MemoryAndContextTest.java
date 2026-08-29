package com.abhishekraj0.core.memory;

import com.abhishekraj0.api.agent.AgentRequest;
import com.abhishekraj0.api.agent.AgentState;
import com.abhishekraj0.api.context.AgentContext;
import com.abhishekraj0.api.memory.Memory;
import com.abhishekraj0.api.memory.MemoryId;
import com.abhishekraj0.api.memory.MemoryMetadata;
import com.abhishekraj0.api.memory.MemoryQuery;
import com.abhishekraj0.api.model.ChatMessage;
import com.abhishekraj0.core.context.SlidingWindowContextManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MemoryAndContextTest {

    @Test
    public void testSlidingWindowContextManager() {
        SlidingWindowContextManager manager = new SlidingWindowContextManager(3);

        List<ChatMessage> history = List.of(
                ChatMessage.system("System Instruction"),
                ChatMessage.user("Message 1"),
                ChatMessage.assistant("Message 2"),
                ChatMessage.user("Message 3"),
                ChatMessage.assistant("Message 4")
        );

        AgentState state = new AgentState("exec-1", history, null, Map.of(), 0, 0, "RUNNING");
        AgentRequest request = new AgentRequest("test input");

        AgentContext context = manager.buildContext(request, state);

        // Should keep system message + last 2 messages (total 3 messages)
        List<ChatMessage> compressed = context.messages();
        assertEquals(3, compressed.size());
        assertEquals("System Instruction", compressed.get(0).content());
        assertEquals("Message 3", compressed.get(1).content());
        assertEquals("Message 4", compressed.get(2).content());
    }

    @Test
    public void testInMemoryVectorMemoryStoreSimilarity() {
        InMemoryVectorMemoryStore store = new InMemoryVectorMemoryStore();

        java.util.UUID id1 = java.util.UUID.randomUUID();
        java.util.UUID id2 = java.util.UUID.randomUUID();

        Memory m1 = new Memory(
                new MemoryId(id1),
                "Java programming language is class-based and object-oriented.",
                "semantic",
                new MemoryMetadata(Instant.now(), Map.of("category", "java"))
        );
        Memory m2 = new Memory(
                new MemoryId(id2),
                "Python is an interpreted high-level general-purpose programming language.",
                "semantic",
                new MemoryMetadata(Instant.now(), Map.of("category", "python"))
        );

        store.save(m1);
        store.save(m2);

        // Search for Java
        List<Memory> resultsJava = store.search(new MemoryQuery("class object Java", "semantic", 2, Map.of()));
        assertEquals(2, resultsJava.size());
        assertEquals(id1, resultsJava.get(0).id().id()); // Java memory should be first because of keyword overlap

        // Search with filter
        List<Memory> resultsFiltered = store.search(new MemoryQuery("programming", "semantic", 2, Map.of("category", "python")));
        assertEquals(1, resultsFiltered.size());
        assertEquals(id2, resultsFiltered.get(0).id().id());

        // Delete memory
        store.delete(new MemoryId(id1));
        List<Memory> resultsAfterDelete = store.search(new MemoryQuery("Java", null, 10, Map.of()));
        assertEquals(1, resultsAfterDelete.size());
        assertEquals(id2, resultsAfterDelete.get(0).id().id());
    }
}
