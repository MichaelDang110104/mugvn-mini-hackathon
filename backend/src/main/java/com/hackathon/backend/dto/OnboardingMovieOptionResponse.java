package com.hackathon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingMovieOptionResponse {
    private List<MovieOption> movies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovieOption {
        private String movieId;
        private String title;
        private List<String> genres;
        private String posterUrl;
        private Integer year;
    }
}
