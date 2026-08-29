package com.agentx.core.memory;

import com.agentx.api.memory.Memory;
import com.agentx.api.memory.MemoryId;
import com.agentx.api.memory.MemoryQuery;
import com.agentx.api.memory.MemoryStore;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * An in-memory mock vector database that stores memories and searches them
 * based on Jaccard token similarity (as a proxy for vector/semantic similarity).
 */
public class InMemoryVectorMemoryStore implements MemoryStore {

    private final Map<MemoryId, Memory> store = new ConcurrentHashMap<>();

    @Override
    public void save(Memory memory) {
        store.put(memory.id(), memory);
    }

    @Override
    public void delete(MemoryId id) {
        store.remove(id);
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public List<Memory> search(MemoryQuery query) {
        List<Memory> filtered = store.values().stream()
                .filter(m -> query.type() == null || query.type().equalsIgnoreCase(m.type()))
                .filter(m -> {
                    if (query.filter() == null || query.filter().isEmpty()) {
                        return true;
                    }
                    if (m.metadata() == null || m.metadata().additionalMetadata() == null) {
                        return false;
                    }
                    return query.filter().entrySet().stream()
                            .allMatch(e -> Objects.equals(e.getValue(), m.metadata().additionalMetadata().get(e.getKey())));
                })
                .collect(Collectors.toList());

        if (query.queryText() == null || query.queryText().isBlank()) {
            return filtered.stream().limit(query.maxResults()).collect(Collectors.toList());
        }

        Set<String> queryTokens = tokenize(query.queryText());
        return filtered.stream()
                .sorted((m1, m2) -> {
                    double sim1 = calculateSimilarity(queryTokens, tokenize(m1.content()));
                    double sim2 = calculateSimilarity(queryTokens, tokenize(m2.content()));
                    return Double.compare(sim2, sim1);
                })
                .limit(query.maxResults())
                .collect(Collectors.toList());
    }

    private Set<String> tokenize(String text) {
        if (text == null) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    private double calculateSimilarity(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);
        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);
        return (double) intersection.size() / union.size();
    }
}
