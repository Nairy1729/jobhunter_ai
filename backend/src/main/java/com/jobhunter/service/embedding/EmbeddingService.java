package com.jobhunter.service.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final GeminiEmbeddingProvider geminiProvider;
    private final DeterministicLocalEmbeddingProvider localProvider;
    private final String configuredProvider;

    public EmbeddingService(
            GeminiEmbeddingProvider geminiProvider,
            DeterministicLocalEmbeddingProvider localProvider,
            @Value("${ai.embedding.provider:auto}") String configuredProvider) {
        this.geminiProvider = geminiProvider;
        this.localProvider = localProvider;
        this.configuredProvider = configuredProvider != null ? configuredProvider.toLowerCase() : "auto";
    }

    public EmbeddingProvider getActiveProvider() {
        if ("gemini".equals(configuredProvider) && geminiProvider.isAvailable()) {
            return geminiProvider;
        }
        if ("auto".equals(configuredProvider) && geminiProvider.isAvailable()) {
            return geminiProvider;
        }
        return localProvider;
    }

    public float[] generateEmbedding(String text) {
        EmbeddingProvider provider = getActiveProvider();
        try {
            return provider.generateEmbedding(text);
        } catch (Exception e) {
            if (provider != localProvider) {
                log.warn("Embedding provider [{}] failed ({}), falling back to deterministic local embedding",
                        provider.getProviderId(), e.getMessage());
                return localProvider.generateEmbedding(text);
            }
            throw e;
        }
    }

    public List<float[]> generateEmbeddings(List<String> texts) {
        EmbeddingProvider provider = getActiveProvider();
        try {
            return provider.generateEmbeddings(texts);
        } catch (Exception e) {
            if (provider != localProvider) {
                log.warn("Embedding provider [{}] failed ({}), falling back to deterministic local embedding",
                        provider.getProviderId(), e.getMessage());
                return localProvider.generateEmbeddings(texts);
            }
            throw e;
        }
    }

    public String getActiveModelName() {
        return getActiveProvider().getModelName();
    }
}
