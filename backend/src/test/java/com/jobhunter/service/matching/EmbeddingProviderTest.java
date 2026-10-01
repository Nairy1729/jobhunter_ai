package com.jobhunter.service.matching;

import com.jobhunter.service.embedding.DeterministicLocalEmbeddingProvider;
import com.jobhunter.service.embedding.EmbeddingService;
import com.jobhunter.service.embedding.GeminiEmbeddingProvider;
import com.jobhunter.service.embedding.VectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingProviderTest {

    private DeterministicLocalEmbeddingProvider localProvider;
    private VectorService vectorService;

    @BeforeEach
    void setUp() {
        localProvider = new DeterministicLocalEmbeddingProvider();
        vectorService = new VectorService(null, null, null, null, null);
    }

    @Test
    @DisplayName("Should generate 768-dimensional normalized embedding vectors")
    void testDimensionsAndNormalization() {
        float[] vector = localProvider.generateEmbedding("Senior Java Backend Engineer with Spring Boot and PostgreSQL");

        assertNotNull(vector);
        assertEquals(768, vector.length);

        // Verify L2 norm is ~ 1.0 (unit vector)
        double sumSq = 0.0;
        for (float v : vector) {
            sumSq += v * v;
        }
        assertEquals(1.0, Math.sqrt(sumSq), 0.01, "Embedding vector must be unit-normalized");
    }

    @Test
    @DisplayName("Should be 100% deterministic: identical input generates identical vector")
    void testDeterminism() {
        String text = "Enterprise microservices with Spring Cloud and Kafka";
        float[] vector1 = localProvider.generateEmbedding(text);
        float[] vector2 = localProvider.generateEmbedding(text);

        assertArrayEquals(vector1, vector2, 0.00001f);
    }

    @Test
    @DisplayName("Should produce high similarity for semantically close texts and low for distinct domains")
    void testSemanticSimilarity() {
        float[] javaBackend1 = localProvider.generateEmbedding("Java Spring Boot REST API PostgreSQL backend development");
        float[] javaBackend2 = localProvider.generateEmbedding("Spring Boot Java microservices with relational database SQL");
        float[] graphicDesign = localProvider.generateEmbedding("Graphic design Adobe Photoshop typography creative branding illustration");

        double simClose = vectorService.cosineSimilarity(javaBackend1, javaBackend2);
        double simFar = vectorService.cosineSimilarity(javaBackend1, graphicDesign);

        assertTrue(simClose > 0.25, "Semantically aligned technical descriptions must yield substantial cosine similarity (>0.25), got: " + simClose);
        assertTrue(simFar < 0.15, "Unrelated domains must yield low cosine similarity (<0.15), got: " + simFar);
        assertTrue(simClose > simFar, "Similar texts must have strictly higher similarity than unrelated texts");
    }

    @Test
    @DisplayName("EmbeddingService selects active provider with graceful fallback")
    void testEmbeddingServiceFallback() {
        GeminiEmbeddingProvider mockGemini = Mockito.mock(GeminiEmbeddingProvider.class);
        Mockito.when(mockGemini.isAvailable()).thenReturn(false);

        EmbeddingService service = new EmbeddingService(mockGemini, localProvider, "auto");
        assertEquals("deterministic_local", service.getActiveProvider().getProviderId());
        assertEquals("deterministic-semantic-hash-768", service.getActiveModelName());

        float[] vector = service.generateEmbedding("Test text");
        assertEquals(768, vector.length);
    }
}
