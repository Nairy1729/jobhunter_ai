package com.jobhunter.service.embedding;

import java.util.List;

public interface EmbeddingProvider {

    /**
     * Unique identifier for the provider (e.g. "gemini", "deterministic_local").
     */
    String getProviderId();

    /**
     * Model name used for embeddings (e.g. "text-embedding-004").
     */
    String getModelName();

    /**
     * Target vector dimension (standardized to 768).
     */
    int getDimensions();

    /**
     * Generates a 768-dimensional float embedding for a single text.
     */
    float[] generateEmbedding(String text);

    /**
     * Generates embeddings in batch.
     */
    List<float[]> generateEmbeddings(List<String> texts);

    /**
     * Checks if this provider is currently available and configured.
     */
    boolean isAvailable();
}
