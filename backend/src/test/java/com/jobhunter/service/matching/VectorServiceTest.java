package com.jobhunter.service.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.service.embedding.VectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VectorServiceTest {

    private VectorService vectorService;

    @BeforeEach
    void setUp() {
        vectorService = new VectorService(null, null, null, null, new ObjectMapper());
    }

    @Test
    @DisplayName("Cosine similarity of identical vectors is 1.0")
    void testIdenticalVectors() {
        float[] v = new float[]{0.6f, 0.8f, 0.0f};
        double sim = vectorService.cosineSimilarity(v, v);
        assertEquals(1.0, sim, 0.0001);
    }

    @Test
    @DisplayName("Cosine similarity of orthogonal vectors is 0.0")
    void testOrthogonalVectors() {
        float[] v1 = new float[]{1.0f, 0.0f, 0.0f};
        float[] v2 = new float[]{0.0f, 1.0f, 0.0f};
        double sim = vectorService.cosineSimilarity(v1, v2);
        assertEquals(0.0, sim, 0.0001);
    }

    @Test
    @DisplayName("Vector serialization and deserialization retains float precision")
    void testSerializationRoundTrip() {
        float[] original = new float[]{0.123f, -0.456f, 0.789f};
        String json = vectorService.serializeVector(original);
        assertNotNull(json);

        float[] restored = vectorService.deserializeVector(json);
        assertEquals(original.length, restored.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], restored[i], 0.0001f);
        }
    }

    @Test
    @DisplayName("Vector status description accurately reflects setup requirement")
    void testStatusDescription() {
        String desc = vectorService.getStatusDescription();
        assertNotNull(desc);
        assertTrue(desc.contains("JSONB") || desc.contains("PostgreSQL"));
    }
}
