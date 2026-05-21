package com.hackathon.backend.engine.tasks;

import com.hackathon.backend.commons.pipeline.Task;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class MergeCandidateGroupsTask extends Task<RecommendationContext> {

    @Override
    public String name() {
        return "merge_candidate_groups";
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        List<ScoredMovie> merged = new ArrayList<>();
        merged.addAll(ctx.getCandidateGroup("query_vector"));
        merged.addAll(ctx.getCandidateGroup("user_profile_vector"));
        ctx.setCandidates(merged);
        return CompletableFuture.completedFuture(ctx);
    }
}
