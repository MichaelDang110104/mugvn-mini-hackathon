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
public class OnboardingRequest {
    private String sessionId;
    private List<String> selectedGenres;
    private List<String> selectedThemes;
    private List<FavoriteMovieSelection> favoriteMovies;
    private List<String> avoidedGenres;
    private List<String> avoidedThemes;
    private List<String> preferredLanguages;
    private String preferredEra;
    private String preferredPace;
    private String freeTextTasteSummary;

    public List<String> getFavoriteTitles() {
        if (favoriteMovies == null) {
            return List.of();
        }

        return favoriteMovies.stream()
                .map(FavoriteMovieSelection::getTitle)
                .filter(title -> title != null && !title.isBlank())
                .toList();
    }

    public List<String> getFavoriteMovieIds() {
        if (favoriteMovies == null) {
            return List.of();
        }

        return favoriteMovies.stream()
                .map(FavoriteMovieSelection::getMovieId)
                .filter(movieId -> movieId != null && !movieId.isBlank())
                .toList();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteMovieSelection {
        private String movieId;
        private String title;
    }
}
