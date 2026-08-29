package com.abhishekraj0.core.memory;

import com.abhishekraj0.api.memory.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * An in-memory vector database that stores memories and searches them
 * based on true cosine similarity of their vector embeddings.
 */
public class InMemoryVectorMemoryStore implements MemoryStore {

    private final Map<MemoryId, Memory> store = new ConcurrentHashMap<>();
    private final Map<MemoryId, Embedding> embeddings = new ConcurrentHashMap<>();
    private final EmbeddingModel embeddingModel;

    public InMemoryVectorMemoryStore() {
        this(new SimpleEmbeddingModel());
    }

    public InMemoryVectorMemoryStore(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel != null ? embeddingModel : new SimpleEmbeddingModel();
    }

    @Override
    public void save(Memory memory) {
        store.put(memory.id(), memory);
        if (memory.content() != null) {
            embeddings.put(memory.id(), embeddingModel.embed(memory.content()));
        }
    }

    @Override
    public void delete(MemoryId id) {
        store.remove(id);
        embeddings.remove(id);
    }

    @Override
    public void clear() {
        store.clear();
        embeddings.clear();
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

        // Generate query embedding
        Embedding queryEmbedding = embeddingModel.embed(query.queryText());

        return filtered.stream()
                .sorted((m1, m2) -> {
                    double sim1 = calculateCosineSimilarity(queryEmbedding, embeddings.get(m1.id()));
                    double sim2 = calculateCosineSimilarity(queryEmbedding, embeddings.get(m2.id()));
                    return Double.compare(sim2, sim1); // Descending order of similarity
                })
                .limit(query.maxResults())
                .collect(Collectors.toList());
    }

    private double calculateCosineSimilarity(Embedding e1, Embedding e2) {
        if (e1 == null || e2 == null) {
            return 0.0;
        }
        List<Double> v1 = e1.vector();
        List<Double> v2 = e2.vector();
        if (v1 == null || v2 == null || v1.size() != v2.size() || v1.isEmpty()) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            double a = v1.get(i);
            double b = v2.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
