package com.hackathon.backend.engine.tasks;

import com.hackathon.backend.commons.pipeline.Task;
import com.hackathon.backend.engine.entities.EngineMode;
import com.hackathon.backend.engine.entities.RecommendationContext;

import java.util.Set;

public abstract class RecommendationTaskBase extends Task<RecommendationContext> {

    protected Set<EngineMode> supportedModes() {
        return Set.of();
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        Set<EngineMode> modes = supportedModes();
        return !modes.isEmpty() && !modes.contains(ctx.getMode());
    }
}
