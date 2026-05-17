package com.hackathon.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "movies")
public class Movie {

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
    private String lastupdated;

    private Date released;

    @Field("num_mflix_comments")
    private Integer numMflixComments;

    private Awards awards;
    private Imdb imdb;
    private Tomatoes tomatoes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Awards {
        private Integer wins;
        private Integer nominations;
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Imdb {
        private Double rating;
        private Integer votes;
        private Integer id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tomatoes {
        private TomatoesReview viewer;
        private TomatoesReview critic;
        private Date dvd;
        private Date lastUpdated;
        private Integer rotten;
        private Integer fresh;
        private String production;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TomatoesReview {
        private Double rating;
        private Integer numReviews;
        private Integer meter;
    }
}
