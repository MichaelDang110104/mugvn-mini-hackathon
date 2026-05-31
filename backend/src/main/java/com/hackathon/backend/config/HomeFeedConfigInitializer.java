package com.hackathon.backend.config;

import com.hackathon.backend.enums.SectionType;
import com.hackathon.backend.models.HomeFeedConfig;
import com.hackathon.backend.models.SectionConfig;
import com.hackathon.backend.repositories.HomeFeedConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomeFeedConfigInitializer implements CommandLineRunner {

    private final HomeFeedConfigRepository homeFeedConfigRepository;

    @Override
    public void run(String... args) {
        if (homeFeedConfigRepository.count() > 0) {
            log.info("[HomeFeedConfigInitializer] config already exists, skipping seed");
            return;
        }

        homeFeedConfigRepository.saveAll(List.of(
                HomeFeedConfig.builder()
                        .id("authenticated")
                        .audience("authenticated")
                        .active(true)
                        .sections(List.of(
                                SectionConfig.builder()
                                        .sectionId("trending")
                                        .titleTemplate("Trending Now")
                                        .type(SectionType.TRENDING)
                                        .limit(20).order(1).enabled(true).dynamic(false)
                                        .build(),
                                SectionConfig.builder()
                                        .sectionId("continue_watching")
                                        .titleTemplate("Continue Watching")
                                        .type(SectionType.RECENT_WATCH)
                                        .limit(10).order(2).enabled(true).dynamic(false)
                                        .build(),
                                SectionConfig.builder()
                                        .sectionId("recommended")
                                        .titleTemplate("Recommended For You")
                                        .type(SectionType.USER_RECOMMENDATION)
                                        .limit(20).order(3).enabled(true).dynamic(false)
                                        .build(),
                                SectionConfig.builder()
                                        .sectionId("because_watched")
                                        .titleTemplate("Because You Watched {anchor}")
                                        .type(SectionType.SIMILAR_TO_MOVIE)
                                        .limit(20).order(4).enabled(true).dynamic(true).dynamicLimit(2)
                                        .build(),
                                SectionConfig.builder()
                                        .sectionId("genre")
                                        .titleTemplate("{genre} Movies")
                                        .type(SectionType.GENRE)
                                        .limit(20).order(5).enabled(true).dynamic(true).dynamicLimit(2)
                                        .build()
                        ))
                        .build(),

                HomeFeedConfig.builder()
                        .id("anonymous")
                        .audience("anonymous")
                        .active(true)
                        .sections(List.of(
                                SectionConfig.builder()
                                        .sectionId("trending")
                                        .titleTemplate("Trending Now")
                                        .type(SectionType.TRENDING)
                                        .limit(20).order(1).enabled(true).dynamic(false)
                                        .build()
                        ))
                        .build()
        ));

        log.info("[HomeFeedConfigInitializer] seeded home feed config for authenticated + anonymous");
    }
}
