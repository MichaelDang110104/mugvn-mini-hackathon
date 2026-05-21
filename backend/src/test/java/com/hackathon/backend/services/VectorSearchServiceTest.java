package com.hackathon.backend.services;

import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.models.EmbeddedMovie;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private MongoDatabase mongoDatabase;

    @Mock
    private MongoCollection<Document> mongoCollection;

    @Mock
    private AggregateIterable<Document> aggregateIterable;

    @Mock
    private MongoConverter mongoConverter;

    @InjectMocks
    private VectorSearchService service;

    @Test
    void searchByQueryText_embedsQueryAndMapsMongoResults() {
        Document resultDocument = new Document("_id", new ObjectId("507f1f77bcf86cd799439011"))
                .append("vectorSearchScore", 0.95D);

        EmbeddedMovie movie = EmbeddedMovie.builder()
                .id(new ObjectId("507f1f77bcf86cd799439011"))
                .title("The Matrix")
                .genres(List.of("Action"))
                .build();

        when(embeddingService.embed("sci-fi hacker")).thenReturn(List.of(0.1, 0.2, 0.3));
        when(mongoTemplate.getCollectionName(EmbeddedMovie.class)).thenReturn("embedded_movies");
        when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
        when(mongoDatabase.getCollection("embedded_movies")).thenReturn(mongoCollection);
        when(mongoCollection.aggregate(anyList())).thenReturn(aggregateIterable);
        when(aggregateIterable.iterator()).thenReturn(singletonIterator(resultDocument));
        when(mongoTemplate.getConverter()).thenReturn(mongoConverter);
        when(mongoConverter.read(eq(EmbeddedMovie.class), eq(resultDocument))).thenReturn(movie);

        List<VectorSearchResult> results = service.searchByQueryText("sci-fi hacker", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMovie().getTitle()).isEqualTo("The Matrix");
        assertThat(results.get(0).getVectorSearchScore()).isEqualTo(0.95);
    }

    @Test
    void searchByEmbedding_withEmptyVector_returnsEmptyWithoutHittingMongo() {
        List<VectorSearchResult> results = service.searchByEmbedding(List.of(), 5);

        assertThat(results).isEmpty();
        verify(mongoTemplate, never()).getDb();
    }

    @Test
    void findSimilarMovies_excludesTargetMovieId() {
        Document selfDocument = new Document("_id", new ObjectId("507f1f77bcf86cd799439011"))
                .append("vectorSearchScore", 1.0D);
        Document otherDocument = new Document("_id", new ObjectId("507f1f77bcf86cd799439012"))
                .append("vectorSearchScore", 0.9D);

        EmbeddedMovie selfMovie = EmbeddedMovie.builder()
                .id(new ObjectId("507f1f77bcf86cd799439011"))
                .title("The Matrix")
                .build();
        EmbeddedMovie otherMovie = EmbeddedMovie.builder()
                .id(new ObjectId("507f1f77bcf86cd799439012"))
                .title("The Matrix Reloaded")
                .build();

        when(embeddingService.embed(anyString())).thenReturn(List.of(0.1, 0.2, 0.3));
        when(mongoTemplate.getCollectionName(EmbeddedMovie.class)).thenReturn("embedded_movies");
        when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
        when(mongoDatabase.getCollection("embedded_movies")).thenReturn(mongoCollection);
        when(mongoCollection.aggregate(anyList())).thenReturn(aggregateIterable);
        when(aggregateIterable.iterator()).thenReturn(List.of(selfDocument, otherDocument).iterator());
        when(mongoTemplate.getConverter()).thenReturn(mongoConverter);
        when(mongoConverter.read(eq(EmbeddedMovie.class), eq(selfDocument))).thenReturn(selfMovie);
        when(mongoConverter.read(eq(EmbeddedMovie.class), eq(otherDocument))).thenReturn(otherMovie);

        List<VectorSearchResult> results = service.findSimilarMovies(
                "A hacker learns the truth.", "507f1f77bcf86cd799439011", 1);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMovie().getTitle()).isEqualTo("The Matrix Reloaded");
    }

    private Iterator<Document> singletonIterator(Document document) {
        return List.of(document).iterator();
    }
}
