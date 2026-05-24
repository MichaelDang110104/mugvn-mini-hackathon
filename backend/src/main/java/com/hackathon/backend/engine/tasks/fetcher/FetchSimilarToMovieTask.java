package com.hackathon.backend.engine.tasks.fetcher;

import com.hackathon.backend.engine.entities.EngineMode;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import com.hackathon.backend.engine.utils.ObjectUtils;
import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.repositories.EmbeddedMovieRepository;
import com.hackathon.backend.services.VectorSearchService;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class FetchSimilarToMovieTask extends RecommendationTaskBase {

    private final EmbeddedMovieRepository embeddedMovieRepository;
    private final VectorSearchService vectorSearchService;
    private final Executor ioExecutor;

    public FetchSimilarToMovieTask(EmbeddedMovieRepository embeddedMovieRepository,
                                   VectorSearchService vectorSearchService,
                                   @Qualifier("ioExecutor") Executor ioExecutor) {
        this.embeddedMovieRepository = embeddedMovieRepository;
        this.vectorSearchService = vectorSearchService;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return "similar_to_movie";
    }

    @Override
    protected Set<EngineMode> supportedModes() {
        return Set.of(EngineMode.SIMILAR_TO_MOVIE);
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        return super.shouldSkip(ctx) || ctx.getMovieId() == null || ctx.getMovieId().isBlank();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        int limit = ctx.getLimit() > 0 ? ctx.getLimit() : 20;
        log.info("[FetchSimilarToMovieTask] userId={} mode={} movieId={} limit={}",
                ctx.getUserId(), ctx.getMode(), ctx.getMovieId(), limit);

        return CompletableFuture.supplyAsync(() -> {
            EmbeddedMovie anchor;
            try {
                anchor = embeddedMovieRepository.findById(new ObjectId(ctx.getMovieId())).orElse(null);
            } catch (IllegalArgumentException e) {
                log.warn("[FetchSimilarToMovieTask] Invalid movieId [{}] for similar-to-movie search", ctx.getMovieId());
                return ctx;
            }

            if (anchor == null || anchor.getPlotEmbedding() == null || anchor.getPlotEmbedding().isEmpty()) {
                log.warn("[FetchSimilarToMovieTask] No embedding found for movieId={}, skipping", ctx.getMovieId());
                return ctx;
            }

            log.info("[FetchSimilarToMovieTask] anchor found: title='{}' embeddingSize={}",
                    anchor.getTitle(), anchor.getPlotEmbedding().size());

            List<ScoredMovie> candidates = vectorSearchService
                    .searchByEmbedding(anchor.getPlotEmbedding(), limit + 1)
                    .stream()
                    .filter(r -> !ctx.getMovieId().equals(
                            r.getMovie().getId() != null ? r.getMovie().getId().toHexString() : null))
                    .limit(limit)
                    .map(r -> ObjectUtils.toScoredMovie(r, "similar_to_movie"))
                    .toList();

            log.info("[FetchSimilarToMovieTask] fetched {} candidates similar to movieId={} ('{}')",
                    candidates.size(), ctx.getMovieId(), anchor.getTitle());

            ctx.addCandidates(candidates);
            return ctx;
        }, ioExecutor);
    }
}
