package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.OnboardingRequest;
import com.hackathon.backend.dto.OnboardingResponse;
import com.hackathon.backend.models.AppUser;
import com.hackathon.backend.models.RecommendationProfile;
import com.hackathon.backend.models.UserOnboardingAnswers;
import com.hackathon.backend.repositories.AppUserRepository;
import com.hackathon.backend.repositories.RecommendationProfileRepository;
import com.hackathon.backend.repositories.UserOnboardingAnswersRepository;
import com.hackathon.backend.services.OnboardingProfileService;
import com.hackathon.backend.services.SessionService;
import com.hackathon.backend.services.StarterRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final SessionService sessionService;
    private final AppUserRepository appUserRepository;
    private final UserOnboardingAnswersRepository answersRepository;
    private final RecommendationProfileRepository profileRepository;
    private final StarterRecommendationService starterRecommendationService;

    @PostMapping
    public OnboardingResponse submit(
            @RequestHeader(value = "X-Session-Id", required = false) String headerSessionId,
            @RequestBody OnboardingRequest request) {
        String sessionId = sessionService.resolveSessionId(headerSessionId, request.getSessionId());
        request.setSessionId(sessionId);

        AppUser appUser = appUserRepository.findBySessionId(sessionId)
                .orElseGet(() -> AppUser.builder()
                        .sessionId(sessionId)
                        .createdAt(Instant.now())
                        .lastSeenAt(Instant.now())
                        .build());
        appUser = appUserRepository.save(appUser);

        UserOnboardingAnswers answers = UserOnboardingAnswers.builder()
                .userId(appUser.getId())
                .sessionId(sessionId)
                .selectedGenres(request.getSelectedGenres())
                .selectedThemes(request.getSelectedThemes())
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

        RecommendationProfile profile = OnboardingProfileService.buildInitialProfile(appUser.getId(), request);
        profile.setSessionId(sessionId);
        RecommendationProfile savedProfile = profileRepository.save(profile);
        starterRecommendationService.refreshStarterCandidates(savedProfile, request.getFreeTextTasteSummary());

        return OnboardingResponse.builder()
                .sessionId(sessionId)
                .completed(true)
                .profileVersion(savedProfile.getProfileVersion())
                .build();
    }
}
