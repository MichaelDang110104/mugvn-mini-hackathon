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
public class SearchResponse {

    private List<SearchItem> items;
    private String mode;
    private boolean fallbackUsed;
    private String query;
    private String hint;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchItem {
        private MovieSummary movie;
        private double score;
        private List<Reason> reasons;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovieSummary {
        private String id;
        private String title;
        private String posterUrl;
        private List<String> genres;
        private Double ratingAvg;
        private Availability availability;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Availability {
        private boolean isAvailable;
        private String region;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reason {
        private String code;
        private String label;
    }
}
