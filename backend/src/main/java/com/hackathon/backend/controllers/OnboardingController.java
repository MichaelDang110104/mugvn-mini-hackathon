package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.OnboardingRequest;
import com.hackathon.backend.dto.OnboardingMovieOptionResponse;
import com.hackathon.backend.dto.OnboardingOptionsResponse;
import com.hackathon.backend.dto.OnboardingResponse;
import com.hackathon.backend.models.MflixUser;
import com.hackathon.backend.models.RecommendationProfile;
import com.hackathon.backend.models.UserOnboardingAnswers;
import com.hackathon.backend.repositories.MflixUserRepository;
import com.hackathon.backend.repositories.RecommendationProfileRepository;
import com.hackathon.backend.repositories.UserOnboardingAnswersRepository;
import com.hackathon.backend.services.OnboardingProfileService;
import com.hackathon.backend.services.OnboardingCatalogService;
import com.hackathon.backend.services.SessionService;
import com.hackathon.backend.services.StarterRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final SessionService sessionService;
    private final MflixUserRepository mflixUserRepository;
    private final UserOnboardingAnswersRepository answersRepository;
    private final RecommendationProfileRepository profileRepository;
    private final StarterRecommendationService starterRecommendationService;
    private final OnboardingCatalogService onboardingCatalogService;

    @GetMapping("/options")
    public OnboardingOptionsResponse getOptions() {
        return onboardingCatalogService.getOptions();
    }

    @GetMapping("/movies")
    public OnboardingMovieOptionResponse getMovieOptions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> genres,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return onboardingCatalogService.getMovieOptions(query, genres, limit);
    }

    @PostMapping
    public OnboardingResponse submit(
            @RequestHeader(value = "X-Session-Id", required = false) String headerSessionId,
            @RequestBody OnboardingRequest request,
            Authentication authentication) {
        String sessionId = sessionService.resolveSessionId(headerSessionId, request.getSessionId());
        request.setSessionId(sessionId);

        String email = extractAuthenticatedEmail(authentication);
        MflixUser user = mflixUserRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Authenticated user not found for email: " + email));

        UserOnboardingAnswers answers = UserOnboardingAnswers.builder()
                .userId(user.getId().toHexString())
                .sessionId(sessionId)
                .selectedGenres(request.getSelectedGenres())
                .selectedThemes(request.getSelectedThemes())
                .favoriteMovieIds(request.getFavoriteMovieIds())
                .favoriteTitles(request.getFavoriteTitles())
                .avoidedGenres(request.getAvoidedGenres())
                .avoidedThemes(request.getAvoidedThemes())
                .preferredLanguages(request.getPreferredLanguages())
                .preferredEra(request.getPreferredEra())
                .preferredPace(request.getPreferredPace())
                .freeTextTasteSummary(request.getFreeTextTasteSummary())
                .completedAt(Instant.now())
                .version(1L)
                .build();
        answersRepository.save(answers);

        RecommendationProfile profile = OnboardingProfileService.buildInitialProfile(user.getId().toHexString(), request);
        profile.setSessionId(sessionId);
        RecommendationProfile savedProfile = profileRepository.save(profile);
        starterRecommendationService.refreshStarterCandidates(savedProfile, request.getFreeTextTasteSummary());

        return OnboardingResponse.builder()
                .sessionId(sessionId)
                .completed(true)
                .profileVersion(savedProfile.getProfileVersion())
                .build();
    }

    private String extractAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new IllegalStateException("Onboarding requires an authenticated user");
        }

        return userDetails.getUsername();
    }
}
