package com.hackathon.backend.engine.tasks.fetcher;

import com.hackathon.backend.engine.entities.EngineMode;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import com.hackathon.backend.engine.utils.ObjectUtils;
import com.hackathon.backend.models.EmbeddedMovie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class FetchByKeywordSearchTask extends RecommendationTaskBase {

    private final MongoTemplate mongoTemplate;
    private final Executor ioExecutor;

    public FetchByKeywordSearchTask(MongoTemplate mongoTemplate,
                                    @Qualifier("ioExecutor") Executor ioExecutor) {
        this.mongoTemplate = mongoTemplate;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return "keyword_search";
    }

    @Override
    protected Set<EngineMode> supportedModes() {
        return Set.of(EngineMode.SEARCH);
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        return super.shouldSkip(ctx) || ctx.hasQuery();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        int limit = ctx.getLimit() > 0 ? ctx.getLimit() : 10;
        log.info("[FetchByKeywordSearchTask] userId={} mode={} query='{}' limit={}",
                ctx.getUserId(), ctx.getMode(), ctx.getSearchQuery(), limit);

        return CompletableFuture.supplyAsync(() -> {
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage()
                    .caseSensitive(false)
                    .matching(ctx.getSearchQuery());

            List<EmbeddedMovie> results = mongoTemplate.find(
                    TextQuery.queryText(textCriteria).sortByScore().limit(limit),
                    EmbeddedMovie.class);

            List<ScoredMovie> candidates = new ArrayList<>(results.size());
            for (int i = 0; i < results.size(); i++) {
                candidates.add(ObjectUtils.toScoredMovie(results.get(i), "keyword_search", 1.0 / (i + 1)));
            }

            log.info("[FetchByKeywordSearchTask] done — {} candidates", candidates.size());
            ctx.addCandidates(candidates);
            return ctx;
        }, ioExecutor);
    }
}
