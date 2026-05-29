package com.hackathon.backend.services;

import com.hackathon.backend.dto.HomeFeedResponse;
import com.hackathon.backend.dto.HomeFeedResponse.HomeSection;
import com.hackathon.backend.dto.SectionSpec;
import com.hackathon.backend.engine.RecommendationEngine;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.enums.SectionType;
import com.hackathon.backend.models.HomeFeedConfig;
import com.hackathon.backend.models.Movie;
import com.hackathon.backend.models.SectionConfig;
import com.hackathon.backend.repositories.HomeFeedConfigRepository;
import com.hackathon.backend.repositories.MovieRepository;
import com.hackathon.backend.repositories.RecommendationProfileRepository;
import com.hackathon.backend.repositories.UserEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HomeFeedService {

    private final HomeFeedConfigRepository homeFeedConfigRepository;
    private final RecommendationEngine recommendationEngine;
    private final UserEventRepository userEventRepository;
    private final RecommendationProfileRepository profileRepository;
    private final MovieRepository movieRepository;
    private final Executor ioExecutor;

    public HomeFeedService(HomeFeedConfigRepository homeFeedConfigRepository,
                           RecommendationEngine recommendationEngine,
                           UserEventRepository userEventRepository,
                           RecommendationProfileRepository profileRepository,
                           MovieRepository movieRepository,
                           @Qualifier("ioExecutor") Executor ioExecutor) {
        this.homeFeedConfigRepository = homeFeedConfigRepository;
        this.recommendationEngine = recommendationEngine;
        this.userEventRepository = userEventRepository;
        this.profileRepository = profileRepository;
        this.movieRepository = movieRepository;
        this.ioExecutor = ioExecutor;
    }

    public CompletableFuture<HomeFeedResponse> buildFeed(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            String audience = userId != null ? "authenticated" : "anonymous";
            HomeFeedConfig config = homeFeedConfigRepository.findByAudienceAndActiveTrue(audience)
                    .orElseThrow(() -> new IllegalStateException("No active home feed config for audience: " + audience));

            return config.getSections().stream()
                    .filter(SectionConfig::isEnabled)
                    .sorted(Comparator.comparingInt(SectionConfig::getOrder))
                    .flatMap(sectionConfig -> resolveSection(sectionConfig, userId).stream())
                    .collect(Collectors.toList());
        }, ioExecutor).thenCompose(sectionSpecs -> {
            List<CompletableFuture<HomeSection>> futures = sectionSpecs.stream()
                    .map(sectionSpec -> recommendationEngine.execute(sectionSpec.getContext())
                            .orTimeout(5, TimeUnit.SECONDS)
                            .thenApply(movies -> HomeSection.builder()
                                    .sectionId(sectionSpec.getSectionId())
                                    .title(sectionSpec.getTitle())
                                    .type(sectionSpec.getType())
                                    .movies(movies)
                                    .build())
                            .exceptionally(exception -> {
                                if (exception.getCause() instanceof TimeoutException) {
                                    log.warn("[HomeFeedService] section={} timed out, skipping", sectionSpec.getSectionId());
                                } else {
                                    log.warn("[HomeFeedService] section={} failed: {}", sectionSpec.getSectionId(), exception.getMessage());
                                }
                                return null;
                            }))
                    .toList();

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(5, TimeUnit.SECONDS)
                    .exceptionally(exception -> null)
                    .thenApplyAsync(ignored -> {
                        List<HomeSection> sections = futures.stream()
                                .map(future -> future.getNow(null))
                                .filter(section -> section != null && section.getMovies() != null && !section.getMovies().isEmpty())
                                .collect(Collectors.toList());
                        return HomeFeedResponse.builder().sections(sections).build();
                    }, ioExecutor);
        });
    }

    private List<SectionSpec> resolveSection(SectionConfig sectionConfig, String userId) {
        if (!sectionConfig.isDynamic()) {
            return List.of(toSpec(sectionConfig, userId, null, null));
        }
        return switch (sectionConfig.getType()) {
            case SIMILAR_TO_MOVIE -> resolveBecauseWatched(sectionConfig, userId);
            case GENRE             -> resolveGenreSections(sectionConfig, userId);
            default               -> List.of();
        };
    }

    private List<SectionSpec> resolveBecauseWatched(SectionConfig sectionConfig, String userId) {
        if (userId == null) return List.of();
        int dynamicLimit = sectionConfig.getDynamicLimit() != null ? sectionConfig.getDynamicLimit() : 2;

        List<String> watchedMovieIds = userEventRepository
                .findByUserIdAndEventTypeOrderByTimestampDesc(userId, EventType.WATCH_START,
                        PageRequest.of(0, dynamicLimit * 3))
                .stream()
                .filter(event -> event.getMovieId() != null)
                .map(event -> event.getMovieId())
                .distinct()
                .limit(dynamicLimit)
                .collect(Collectors.toList());

        List<SectionSpec> sectionSpecs = new ArrayList<>();
        for (String movieId : watchedMovieIds) {
            String title;
            try {
                title = movieRepository.findById(new ObjectId(movieId))
                        .map(Movie::getTitle)
                        .orElse("a movie");
            } catch (Exception exception) {
                title = "a movie";
            }
            String sectionTitle = sectionConfig.getTitleTemplate().replace("{anchor}", title);
            sectionSpecs.add(toSpec(sectionConfig, userId, sectionTitle, movieId));
        }
        return sectionSpecs;
    }

    private List<SectionSpec> resolveGenreSections(SectionConfig sectionConfig, String userId) {
        if (userId == null) return List.of();
        int dynamicLimit = sectionConfig.getDynamicLimit() != null ? sectionConfig.getDynamicLimit() : 2;

        List<String> genres = profileRepository.findByUserId(userId)
                .map(profile -> profile.getTopGenres() != null ? profile.getTopGenres() : List.<String>of())
                .orElse(List.of())
                .stream()
                .limit(dynamicLimit)
                .collect(Collectors.toList());

        return genres.stream()
                .map(genre -> toSpec(sectionConfig, userId,
                        sectionConfig.getTitleTemplate().replace("{genre}", genre),
                        genre))
                .collect(Collectors.toList());
    }

    private SectionSpec toSpec(SectionConfig sectionConfig, String userId, String titleOverride, String extra) {
        String title = titleOverride != null ? titleOverride : sectionConfig.getTitleTemplate();
        int limit = sectionConfig.getLimit() > 0 ? sectionConfig.getLimit() : 20;

        RecommendationContext context = switch (sectionConfig.getType()) {
            case TRENDING            -> RecommendationContext.forTrending(userId, limit);
            case RECENT_WATCH        -> RecommendationContext.forRecentWatch(userId, limit);
            case USER_RECOMMENDATION -> RecommendationContext.forUserRecommend(userId, limit);
            case SIMILAR_TO_MOVIE    -> RecommendationContext.forSimilarToMovie(userId, extra, limit);
            case GENRE               -> (extra != null && !extra.isBlank())
                                         ? RecommendationContext.forGenre(userId, extra, limit)
                                         : RecommendationContext.forGenre(userId, limit);
        };

        String sectionId = sectionConfig.getSectionId() + (extra != null ? "_" + extra : "");
        return SectionSpec.builder()
                .sectionId(sectionId)
                .title(title)
                .type(sectionConfig.getType())
                .context(context)
                .build();
    }

    public CompletableFuture<List<Movie>> loadSection(String userId, String sectionId, int page, int limit) {
        String audience = userId != null ? "authenticated" : "anonymous";
        HomeFeedConfig config = homeFeedConfigRepository.findByAudienceAndActiveTrue(audience)
                .orElseThrow(() -> new IllegalStateException("No active home feed config for audience: " + audience));

        SectionConfig sectionConfig = config.getSections().stream()
                .filter(SectionConfig::isEnabled)
                .filter(section -> sectionId.equals(section.getSectionId()) || sectionId.startsWith(section.getSectionId() + "_"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown sectionId: " + sectionId));

        String extra = null;
        String prefix = sectionConfig.getSectionId() + "_";
        if (sectionId.startsWith(prefix)) {
            extra = sectionId.substring(prefix.length());
        }

        int fetchLimit = (page + 1) * limit;
        RecommendationContext context = buildContext(sectionConfig.getType(), userId, extra, fetchLimit);

        log.info("[HomeFeedService] loadSection sectionId={} userId={} page={} limit={}", sectionId, userId, page, limit);

        return recommendationEngine.execute(context).thenApply(movies -> {
            int fromIndex = page * limit;
            if (fromIndex >= movies.size()) return List.of();
            return movies.subList(fromIndex, Math.min(fromIndex + limit, movies.size()));
        });
    }

    private RecommendationContext buildContext(SectionType type, String userId, String extra, int limit) {
        return switch (type) {
            case TRENDING            -> RecommendationContext.forTrending(userId, limit);
            case RECENT_WATCH        -> RecommendationContext.forRecentWatch(userId, limit);
            case USER_RECOMMENDATION -> RecommendationContext.forUserRecommend(userId, limit);
            case SIMILAR_TO_MOVIE    -> RecommendationContext.forSimilarToMovie(userId, extra, limit);
            case GENRE               -> (extra != null && !extra.isBlank())
                                         ? RecommendationContext.forGenre(userId, extra, limit)
                                         : RecommendationContext.forGenre(userId, limit);
        };
    }
}
