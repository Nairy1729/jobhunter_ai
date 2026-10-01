package com.jobhunter.service.embedding;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component("deterministicLocalEmbeddingProvider")
public class DeterministicLocalEmbeddingProvider implements EmbeddingProvider {

    private static final String MODEL_NAME = "deterministic-semantic-hash-768";
    private static final int DIMENSIONS = 768;

    @Override
    public String getProviderId() {
        return "deterministic_local";
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    @Override
    public int getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[DIMENSIONS];
        }

        float[] vector = new float[DIMENSIONS];
        String clean = text.toLowerCase().replaceAll("[^a-z0-9\\s_\\-\\.]", " ");
        String[] tokens = clean.split("\\s+");

        // Term frequency map
        Map<String, Float> tf = new HashMap<>();
        for (String token : tokens) {
            if (token.isBlank()) continue;
            tf.put(token, tf.getOrDefault(token, 0f) + 1.0f);

            // Subword 3-grams & 4-grams for technical suffixes/roots
            if (token.length() >= 3) {
                for (int i = 0; i <= token.length() - 3; i++) {
                    String sub = "sub:" + token.substring(i, i + 3);
                    tf.put(sub, tf.getOrDefault(sub, 0f) + 0.3f);
                }
            }
        }

        // Bigrams
        for (int i = 0; i < tokens.length - 1; i++) {
            if (!tokens[i].isBlank() && !tokens[i + 1].isBlank()) {
                String bi = "bi:" + tokens[i] + "_" + tokens[i + 1];
                tf.put(bi, tf.getOrDefault(bi, 0f) + 1.2f);
            }
        }

        // Hash into 768 dimensions with positive TF weighting
        for (Map.Entry<String, Float> entry : tf.entrySet()) {
            float weight = (float) Math.log(1.0 + entry.getValue());
            applyFeatureHash(vector, entry.getKey(), weight);
        }

        // L2 Normalization (unit vector)
        double sumSq = 0.0;
        for (float val : vector) {
            sumSq += val * val;
        }

        if (sumSq > 0.0) {
            float norm = (float) Math.sqrt(sumSq);
            for (int i = 0; i < DIMENSIONS; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        List<float[]> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(generateEmbedding(text));
        }
        return results;
    }

    private void applyFeatureHash(float[] vector, String feature, float weight) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(feature.getBytes(StandardCharsets.UTF_8));

            int rawIndex = ((digest[0] & 0xFF) << 8) | (digest[1] & 0xFF);
            int index = Math.abs(rawIndex) % DIMENSIONS;

            vector[index] += weight;
        } catch (Exception ignored) {}
    }
}
