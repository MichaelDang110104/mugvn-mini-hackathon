package com.hackathon.backend.engine.tasks.loader;

import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import com.hackathon.backend.models.RecommendationProfile;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.RecommendationProfileRepository;
import com.hackathon.backend.repositories.UserEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class LoadRecommendationProfileTask extends RecommendationTaskBase {

    private final RecommendationProfileRepository recommendationProfileRepository;
    private final UserEventRepository userEventRepository;

    public LoadRecommendationProfileTask(
            RecommendationProfileRepository recommendationProfileRepository,
            UserEventRepository userEventRepository
    ) {
        this.recommendationProfileRepository = recommendationProfileRepository;
        this.userEventRepository = userEventRepository;
    }

    @Override
    public String name() {
        return "load_recommendation_profile";
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        return super.shouldSkip(ctx) || ctx.getUserId() == null || ctx.getUserId().isBlank();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        log.info("[LoadRecommendationProfileTask] userId={} mode={}", ctx.getUserId(), ctx.getMode());

        RecommendationProfile profile = recommendationProfileRepository.findByUserId(ctx.getUserId()).orElse(null);
        if (profile != null) {
            ctx.setProfile(profile);
            if (profile.getProfileEmbedding() != null && !profile.getProfileEmbedding().isEmpty()) {
                ctx.setUserProfileEmbedding(profile.getProfileEmbedding());
            }
            log.info("[LoadRecommendationProfileTask] done — profile found, topGenres={}, hasEmbedding={}",
                    profile.getTopGenres(), ctx.getUserProfileEmbedding() != null);
        } else {
            log.info("[LoadRecommendationProfileTask] done — no profile found for userId={}", ctx.getUserId());
        }

        ctx.setExcludedMovieIds(userEventRepository.findByUserIdOrderByTimestampDesc(ctx.getUserId()).stream()
                .map(UserEvent::getMovieId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));

        return CompletableFuture.completedFuture(ctx);
    }
}
