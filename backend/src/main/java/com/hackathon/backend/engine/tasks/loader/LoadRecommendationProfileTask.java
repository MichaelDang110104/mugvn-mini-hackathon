package com.hackathon.backend.engine.tasks.loader;

import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import com.hackathon.backend.models.RecommendationProfile;
import com.hackathon.backend.repositories.RecommendationProfileRepository;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class LoadRecommendationProfileTask extends RecommendationTaskBase {

    private final RecommendationProfileRepository recommendationProfileRepository;

    public LoadRecommendationProfileTask(RecommendationProfileRepository recommendationProfileRepository) {
        this.recommendationProfileRepository = recommendationProfileRepository;
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
        RecommendationProfile profile = recommendationProfileRepository.findById(ctx.getUserId()).orElse(null);
        if (profile != null) {
            ctx.setProfile(profile);
            if (profile.getProfileEmbedding() != null && !profile.getProfileEmbedding().isEmpty()) {
                ctx.setUserProfileEmbedding(profile.getProfileEmbedding());
            }
        }
        return CompletableFuture.completedFuture(ctx);
    }
}
