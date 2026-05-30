package com.hackathon.backend.dto;

import com.hackathon.backend.models.Movie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * API representation of {@link Movie}. Mirrors the entity field-for-field, but exposes the
 * Mongo {@code _id} as a hex string instead of the raw {@link org.bson.types.ObjectId},
 * which Jackson would otherwise serialize as a {@code {date,timestamp}} object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {

    private String id;

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

    private Integer numMflixComments;

    private Movie.Awards awards;
    private Movie.Imdb imdb;
    private Movie.Tomatoes tomatoes;

    public static MovieResponse from(Movie movie) {
        if (movie == null) {
            return null;
        }
        return MovieResponse.builder()
                .id(movie.getId() != null ? movie.getId().toHexString() : null)
                .title(movie.getTitle())
                .plot(movie.getPlot())
                .fullplot(movie.getFullplot())
                .genres(movie.getGenres())
                .cast(movie.getCast())
                .directors(movie.getDirectors())
                .writers(movie.getWriters())
                .languages(movie.getLanguages())
                .countries(movie.getCountries())
                .runtime(movie.getRuntime())
                .year(movie.getYear())
                .rated(movie.getRated())
                .type(movie.getType())
                .poster(movie.getPoster())
                .lastupdated(movie.getLastupdated())
                .released(movie.getReleased())
                .numMflixComments(movie.getNumMflixComments())
                .awards(movie.getAwards())
                .imdb(movie.getImdb())
                .tomatoes(movie.getTomatoes())
                .build();
    }
}
