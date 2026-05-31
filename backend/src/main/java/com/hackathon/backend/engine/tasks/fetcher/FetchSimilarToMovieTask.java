package com.hackathon.backend.engine.tasks.fetcher;

import com.hackathon.backend.dto.VectorSearchResult;
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
        return super.shouldSkip(ctx) || !ctx.hasAnchorMovie();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        int limit = ctx.getLimit() > 0 ? ctx.getLimit() : 20;
        log.info("[FetchSimilarToMovieTask] userId={} mode={} movieId={} limit={}",
                ctx.getUserId(), ctx.getMode(), ctx.getMovieId(), limit);

        return CompletableFuture.supplyAsync(() -> {
            String movieId = ctx.getMovieId();
            EmbeddedMovie anchor;
            try {
                anchor = embeddedMovieRepository.findById(new ObjectId(movieId)).orElse(null);
            } catch (IllegalArgumentException e) {
                log.warn("[FetchSimilarToMovieTask] Invalid movieId [{}] for similar-to-movie search", movieId);
                return ctx;
            }

            if (anchor == null || anchor.getPlotEmbedding() == null || anchor.getPlotEmbedding().isEmpty()) {
                log.warn("[FetchSimilarToMovieTask] No embedding found for movieId={}, skipping", movieId);
                return ctx;
            }

            log.info("[FetchSimilarToMovieTask] anchor found: title='{}' embeddingSize={}",
                    anchor.getTitle(), anchor.getPlotEmbedding().size());

            List<VectorSearchResult> raw =
                    vectorSearchService.searchByEmbedding(anchor.getPlotEmbedding(), limit * 10);

            List<String> excluded = ctx.getExcludedMovieIds() != null ? ctx.getExcludedMovieIds() : List.of();

            // Post-search filter: exclude anchor movie and explicitly excluded ids
            List<VectorSearchResult> filtered = raw.stream()
                    .filter(r -> r.getMovie() != null && r.getMovie().getId() != null)
                    .filter(r -> !movieId.equals(r.getMovie().getId().toHexString()))
                    .filter(r -> !excluded.contains(r.getMovie().getId().toHexString()))
                    .toList();

            // Score drop-off: keep results within 75% of the top score
            if (filtered.isEmpty()) {
                log.info("[FetchSimilarToMovieTask] no candidates after filtering for movieId={}", movieId);
                return ctx;
            }

            double topScore = filtered.get(0).getVectorSearchScore();
            double threshold = topScore * 0.75;

            List<ScoredMovie> candidates = filtered.stream()
                    .filter(r -> r.getVectorSearchScore() >= threshold)
                    .limit((long) limit * 2)
                    .map(r -> ObjectUtils.toScoredMovie(r, "similar_to_movie"))
                    .toList();

            log.info("[FetchSimilarToMovieTask] fetched {} candidates similar to movieId={} ('{}') topScore={} threshold={}",
                    candidates.size(), movieId, anchor.getTitle(), topScore, threshold);

            ctx.addCandidates(candidates);
            return ctx;
        }, ioExecutor);
    }
}
