package com.hackathon.backend.services;

import com.hackathon.backend.dto.OnboardingMovieOptionResponse;
import com.hackathon.backend.dto.OnboardingOptionsResponse;
import com.hackathon.backend.models.EmbeddedMovie;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingCatalogServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private OnboardingCatalogService service;

    @Test
    void getOptions_returnsSortedDistinctGenres() {
        when(mongoTemplate.findDistinct(any(Query.class), eq("genres"), eq("embedded_movies"), eq(String.class)))
                .thenReturn(List.of("Drama", "Action", "Drama"));

        OnboardingOptionsResponse response = service.getOptions();

        assertThat(response.getGenres()).containsExactly("Action", "Drama");
    }

    @Test
    void getMovieOptions_withQuery_usesTitleSearch() {
        EmbeddedMovie movie = EmbeddedMovie.builder()
                .id(new ObjectId("507f1f77bcf86cd799439011"))
                .title("Interstellar")
                .genres(List.of("Sci-Fi", "Drama"))
                .poster("poster.jpg")
                .year(2014)
                .build();
        when(mongoTemplate.find(any(Query.class), eq(EmbeddedMovie.class))).thenReturn(List.of(movie));

        OnboardingMovieOptionResponse response = service.getMovieOptions("inter", List.of("Sci-Fi"), 10);

        assertThat(response.getMovies()).hasSize(1);
        assertThat(response.getMovies().getFirst().getMovieId()).isEqualTo("507f1f77bcf86cd799439011");
        verify(mongoTemplate).find(any(Query.class), eq(EmbeddedMovie.class));
    }

    @Test
    void getMovieOptions_withoutQuery_samplesByGenres() {
        EmbeddedMovie movie = EmbeddedMovie.builder()
                .id(new ObjectId("507f1f77bcf86cd799439012"))
                .title("Arrival")
                .genres(List.of("Sci-Fi", "Drama"))
                .build();
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("embedded_movies"), eq(EmbeddedMovie.class)))
                .thenReturn(new org.springframework.data.mongodb.core.aggregation.AggregationResults<>(List.of(movie), new Document()));

        OnboardingMovieOptionResponse response = service.getMovieOptions("", List.of("Sci-Fi", "Drama", "Mystery"), 10);

        assertThat(response.getMovies()).hasSize(1);
        assertThat(response.getMovies().getFirst().getTitle()).isEqualTo("Arrival");
    }
}
