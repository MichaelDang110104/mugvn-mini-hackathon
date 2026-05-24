package com.hackathon.backend.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    @Cacheable(value = "queries", key = "#text")
    public List<Double> embed(String text) {
        try {
            float[] embedding = embeddingModel.embed(text);
            return toDoubleList(embedding);
        } catch (Exception e) {
            log.error("Embedding generation failed for text [{}]: {}", truncate(text, 80), e.getMessage(), e);
            return List.of();
        }
    }

    public List<List<Double>> embedBatch(List<String> texts) {
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

    public String buildStarterProfileText(List<String> genres, List<String> themes, List<String> titles,
            List<String> languages, String era, String pace, String freeText) {
        StringBuilder sb = new StringBuilder();
        if (genres != null && !genres.isEmpty()) {
            sb.append("Preferred genres: ").append(String.join(", ", genres));
        }
        if (themes != null && !themes.isEmpty()) {
            if (!sb.isEmpty())
                sb.append(". ");
            sb.append("Themes: ").append(String.join(", ", themes));
        }
        if (titles != null && !titles.isEmpty()) {
            if (!sb.isEmpty())
                sb.append(". ");
            sb.append("Favorite movies: ").append(String.join(", ", titles));
        }
        if (languages != null && !languages.isEmpty()) {
            if (!sb.isEmpty())
                sb.append(". ");
            sb.append("Languages: ").append(String.join(", ", languages));
        }
        if (era != null && !era.isBlank()) {
            if (!sb.isEmpty())
                sb.append(". ");
            sb.append("Era: ").append(era);
        }
        if (pace != null && !pace.isBlank()) {
            if (!sb.isEmpty())
                sb.append(". ");
            sb.append("Pace: ").append(pace);
        }
        if (freeText != null && !freeText.isBlank()) {
            if (!sb.isEmpty())
                sb.append(". ");
            sb.append(freeText);
        }
        return sb.toString();
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
