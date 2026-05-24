package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.OnboardingRequest;
import com.hackathon.backend.dto.OnboardingResponse;
import com.hackathon.backend.dto.OnboardingRequest.FavoriteMovieSelection;
import com.hackathon.backend.dto.OnboardingMovieOptionResponse;
import com.hackathon.backend.dto.OnboardingOptionsResponse;
import com.hackathon.backend.models.MflixUser;
import com.hackathon.backend.models.RecommendationProfile;
import com.hackathon.backend.models.UserOnboardingAnswers;
import com.hackathon.backend.repositories.MflixUserRepository;
import com.hackathon.backend.repositories.RecommendationProfileRepository;
import com.hackathon.backend.repositories.UserOnboardingAnswersRepository;
import com.hackathon.backend.services.OnboardingCatalogService;
import com.hackathon.backend.services.SessionService;
import com.hackathon.backend.services.StarterRecommendationService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private MflixUserRepository mflixUserRepository;

    @Mock
    private UserOnboardingAnswersRepository answersRepository;

    @Mock
    private RecommendationProfileRepository profileRepository;

    @Mock
    private StarterRecommendationService starterRecommendationService;

    @Mock
    private OnboardingCatalogService onboardingCatalogService;

    @InjectMocks
    private OnboardingController controller;

    @Mock
    private Authentication authentication;

    @Test
    void submitOnboarding_persistsAnswersAndReturnsResolvedSession() {
        when(sessionService.resolveSessionId(null, "session-1")).thenReturn("session-1");
        UserDetails userDetails = User.withUsername("user@example.com").password("pw").authorities(List.of()).build();
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(mflixUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(MflixUser.builder()
                .id(new ObjectId("507f1f77bcf86cd799439011"))
                .email("user@example.com")
                .build()));
        when(profileRepository.save(any(RecommendationProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OnboardingRequest request = OnboardingRequest.builder()
                .sessionId("session-1")
                .selectedGenres(List.of("Sci-Fi", "Drama", "Thriller"))
                .selectedThemes(List.of("emotional", "space", "mind-bending"))
                .favoriteMovies(List.of(
                        FavoriteMovieSelection.builder().movieId("movie-1").title("Interstellar").build(),
                        FavoriteMovieSelection.builder().movieId("movie-2").title("Arrival").build()))
                .build();

        OnboardingResponse response = controller.submit(null, request, authentication);

        assertThat(response.getSessionId()).isEqualTo("session-1");
        assertThat(response.isCompleted()).isTrue();

        ArgumentCaptor<UserOnboardingAnswers> answersCaptor = ArgumentCaptor.forClass(UserOnboardingAnswers.class);
        verify(answersRepository).save(answersCaptor.capture());
        assertThat(answersCaptor.getValue().getSelectedGenres()).containsExactly("Sci-Fi", "Drama", "Thriller");
        assertThat(answersCaptor.getValue().getFavoriteTitles()).containsExactly("Interstellar", "Arrival");
        assertThat(answersCaptor.getValue().getFavoriteMovieIds()).containsExactly("movie-1", "movie-2");

        ArgumentCaptor<RecommendationProfile> profileCaptor = ArgumentCaptor.forClass(RecommendationProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getProfileSource()).isEqualTo("onboarding");
        assertThat(profileCaptor.getValue().getUserId()).isEqualTo("507f1f77bcf86cd799439011");

        verify(starterRecommendationService).refreshStarterCandidates(any(RecommendationProfile.class), any());
    }

    @Test
    void submitOnboarding_usesAuthenticatedMflixUser() {
        when(sessionService.resolveSessionId(null, "session-1")).thenReturn("session-1");
        UserDetails userDetails = User.withUsername("user@example.com").password("pw").authorities(List.of()).build();
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(mflixUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(MflixUser.builder()
                .id(new ObjectId("507f1f77bcf86cd799439011"))
                .email("user@example.com")
                .build()));
        when(profileRepository.save(any(RecommendationProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OnboardingRequest request = OnboardingRequest.builder()
                .sessionId("session-1")
                .selectedGenres(List.of("Sci-Fi", "Drama", "Thriller"))
                .selectedThemes(List.of("emotional", "space", "mind-bending"))
                .favoriteMovies(List.of(FavoriteMovieSelection.builder().movieId("movie-1").title("Interstellar").build()))
                .build();

        OnboardingResponse response = controller.submit(null, request, authentication);

        assertThat(response.getSessionId()).isEqualTo("session-1");
        verify(mflixUserRepository).findByEmail("user@example.com");
    }

    @Test
    void getOptions_returnsGenresFromCatalogService() {
        OnboardingOptionsResponse expected = OnboardingOptionsResponse.builder()
                .genres(List.of("Action", "Drama"))
                .build();
        when(onboardingCatalogService.getOptions()).thenReturn(expected);

        OnboardingOptionsResponse response = controller.getOptions();

        assertThat(response.getGenres()).containsExactly("Action", "Drama");
    }

    @Test
    void getMovieOptions_returnsCatalogResults() {
        OnboardingMovieOptionResponse expected = OnboardingMovieOptionResponse.builder()
                .movies(List.of(OnboardingMovieOptionResponse.MovieOption.builder()
                        .movieId("movie-1")
                        .title("Interstellar")
                        .genres(List.of("Sci-Fi"))
                        .build()))
                .build();
        when(onboardingCatalogService.getMovieOptions("space", List.of("Sci-Fi", "Drama"), 10)).thenReturn(expected);

        OnboardingMovieOptionResponse response = controller.getMovieOptions("space", List.of("Sci-Fi", "Drama"), 10);

        assertThat(response.getMovies()).hasSize(1);
        assertThat(response.getMovies().getFirst().getTitle()).isEqualTo("Interstellar");
    }
}
