package com.hackathon.backend.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    public List<Double> embed(String text) {
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            log.warn("Embedding model unavailable, returning empty embedding");
            return List.of();
        }
        try {
            float[] embedding = embeddingModel.embed(text);
            return toDoubleList(embedding);
        } catch (Exception e) {
            log.error("Embedding generation failed for text [{}]: {}", truncate(text, 80), e.getMessage(), e);
            return List.of();
        }
    }

    public List<List<Double>> embedBatch(List<String> texts) {
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            log.warn("Embedding model unavailable, returning empty embedding batch");
            return List.of();
        }
        try {
            List<float[]> embeddings = embeddingModel.embed(texts);
            return embeddings.stream()
                    .map(this::toDoubleList)
                    .toList();
        } catch (Exception e) {
            log.error("Batch embedding generation failed for {} texts: {}", texts.size(), e.getMessage(), e);
            return List.of();
        }
    }

    public String buildMovieEmbeddingText(String title, List<String> genres,
            List<String> tags, String overview) {
        StringBuilder sb = new StringBuilder();
        if (title != null)
            sb.append(title).append(". ");
        if (genres != null && !genres.isEmpty()) {
            sb.append("Genres: ").append(String.join(", ", genres)).append(". ");
        }
        if (tags != null && !tags.isEmpty()) {
            sb.append("Tags: ").append(String.join(", ", tags)).append(". ");
        }
        if (overview != null)
            sb.append(overview);
        return sb.toString().trim();
    }

    private List<Double> toDoubleList(float[] floats) {
        List<Double> doubles = new ArrayList<>(floats.length);
        for (float f : floats) {
            doubles.add((double) f);
        }
        return doubles;
    }

    private String truncate(String text, int maxLen) {
        if (text == null)
            return "null";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
