package com.jobhunter.service.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Component("geminiEmbeddingProvider")
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingProvider.class);
    private final String modelName;
    private final int dimensions;

    private final String apiKey;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiEmbeddingProvider(
            @Value("${ai.gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${ai.gemini.model-embedding:gemini-embedding-001}") String modelName,
            @Value("${ai.gemini.embedding-dimensions:3072}") int dimensions,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.modelName = modelName != null && !modelName.isBlank() ? modelName.trim() : "gemini-embedding-001";
        this.dimensions = dimensions > 0 ? dimensions : 3072;
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderId() {
        return "gemini";
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (!isAvailable()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }
        if (text == null || text.isBlank()) {
            return new float[dimensions];
        }

        try {
            Map<String, Object> request = Map.of(
                    "model", "models/" + modelName,
                    "content", Map.of("parts", List.of(Map.of("text", text.length() > 8000 ? text.substring(0, 8000) : text)))
            );

            String responseBody = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/" + modelName + ":embedContent")
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode valuesNode = root.path("embedding").path("values");
            if (valuesNode.isArray() && valuesNode.size() > 0) {
                float[] vector = new float[valuesNode.size()];
                for (int i = 0; i < valuesNode.size(); i++) {
                    vector[i] = (float) valuesNode.get(i).asDouble();
                }
                return vector;
            }
            throw new IllegalStateException("Invalid embedding response from Gemini API: " + responseBody);
        } catch (Exception e) {
            log.error("Failed to generate embedding via Gemini API: {}", e.getMessage());
            throw new RuntimeException("Gemini embedding failure: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        List<float[]> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(generateEmbedding(text));
        }
        return results;
    }
}
