package com.hackathon.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Float32BinaryVector;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "embedded_movies")
public class EmbeddedMovie {

    @Id
    private ObjectId id;

    private String title;
    private String plot;
    private String fullplot;

    private List<String> genres;
    private List<String> cast;
    private List<String> directors;
    private List<String> writers;
    private List<String> languages;
    private List<String> countries;

    private Integer runtime;
    private Integer year;
    private String rated;
    private String type;
    private String poster;
    private String playbackUrl;
    private String lastupdated;

    private Date released;

    @Field("num_mflix_comments")
    private Integer numMflixComments;

    private Movie.Awards awards;
    private Movie.Imdb imdb;
    private Movie.Tomatoes tomatoes;

    /**
     * 1536d embeddings from the plot field using OpenAI text-embedding-ada-002.
     * Stored as binData in MongoDB, mapped as List&lt;Double&gt; by the driver.
     */
    @Field("plot_embedding")
    private List<Double> plotEmbedding;

    /**
     * 2048d embeddings from the plot field using Voyage AI voyage-3-large.
     * Stored as binData in MongoDB, mapped as List&lt;Double&gt; by the driver.
     */
    @Field("plot_embedding_voyage_3_large")
    private List<Double> plotEmbeddingVoyage3Large;

    public List<Double> getPlotEmbedding() {
        return normalizeEmbedding(plotEmbedding);
    }

    public List<Double> getPlotEmbeddingVoyage3Large() {
        return normalizeEmbedding(plotEmbeddingVoyage3Large);
    }

    private List<Double> normalizeEmbedding(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            return embedding;
        }

        Object firstValue = embedding.getFirst();
        if (!(firstValue instanceof Float32BinaryVector binaryVector)) {
            return embedding;
        }

        float[] data = binaryVector.getData();
        List<Double> normalized = new ArrayList<>(data.length);
        for (float value : data) {
            normalized.add((double) value);
        }
        return normalized;
    }
}
