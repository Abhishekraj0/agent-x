package com.abhishekraj0.core.memory;

import com.abhishekraj0.api.memory.Embedding;
import com.abhishekraj0.api.memory.EmbeddingModel;
import java.util.*;

/**
 * A simple token hashing-based EmbeddingModel producing dense double vectors
 * via random projection. Fully thread-safe.
 */
public class SimpleEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 128;

    @Override
    public Embedding embed(String text) {
        if (text == null) {
            text = "";
        }
        double[] vector = new double[DIMENSIONS];
        String[] tokens = text.toLowerCase().split("\\W+");
        for (String token : tokens) {
            if (token.isBlank()) continue;
            // Deterministic random generator based on token hash
            Random rand = new Random(token.hashCode());
            for (int i = 0; i < DIMENSIONS; i++) {
                vector[i] += rand.nextGaussian();
            }
        }
        // Normalize the vector (L2 norm)
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        List<Double> list = new ArrayList<>(DIMENSIONS);
        for (double v : vector) {
            list.add(norm > 0 ? v / norm : 0.0);
        }
        return new Embedding(list);
    }
}
