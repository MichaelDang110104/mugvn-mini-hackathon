package com.hackathon.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void createIndexes() {
        createEmbeddedMoviesTextIndex();
        log.info("MongoDB indexes initialized");
    }

    private void createEmbeddedMoviesTextIndex() {
        try {
            TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
                    .onField("title", 10F)
                    .onField("plot", 5F)
                    .onField("fullplot", 3F)
                    .onField("genres", 2F)
                    .onField("cast", 2F)
                    .build();

            mongoTemplate.indexOps("embedded_movies").ensureIndex(textIndex);
            log.info("Text index ensured on embedded_movies collection");
        } catch (Exception e) {
            log.warn("Could not create text index on embedded_movies: {}", e.getMessage());
        }
    }
}
