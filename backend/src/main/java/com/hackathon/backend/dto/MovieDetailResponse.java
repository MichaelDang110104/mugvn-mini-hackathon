package com.hackathon.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovieDetailResponse {

    private MovieDetail movie;
    private List<SearchResponse.SearchItem> similarMovies;
    private String mode;
    private boolean fallbackUsed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovieDetail {
        private String id;
        private String title;
        private String overview;
        private List<String> genres;
        private String posterUrl;
        private Double ratingAvg;
        private Availability availability;

        // Extended fields for detail page
        private List<String> cast;
        private List<String> directors;
        private List<String> writers;
        private List<String> languages;
        private List<String> countries;
        private Integer runtime;
        private Integer year;
        private String rated;
        private String fullplot;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Availability {
        private boolean isAvailable;
        private String region;
    }
}
