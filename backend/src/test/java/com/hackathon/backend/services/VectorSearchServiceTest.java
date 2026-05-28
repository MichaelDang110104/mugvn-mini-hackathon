package com.hackathon.backend.services;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.BinaryVector;
import org.bson.BsonBinary;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private EmbeddingService embeddingService;

    @Test
    void searchByEmbedding_sendsQueryVectorAsBsonFloat32Binary() {
        MongoDatabase database = mock(MongoDatabase.class);
        MongoCollection<Document> collection = mock(MongoCollection.class);
        AggregateIterable<Document> aggregateIterable = mock(AggregateIterable.class);
        MongoCursor<Document> cursor = mock(MongoCursor.class);
        ArgumentCaptor<List<Document>> pipelineCaptor = ArgumentCaptor.forClass(List.class);

        when(mongoTemplate.getCollectionName(com.hackathon.backend.models.EmbeddedMovie.class))
                .thenReturn("embedded_movies");
        when(mongoTemplate.getDb()).thenReturn(database);
        when(database.getCollection("embedded_movies")).thenReturn(collection);
        when(collection.aggregate(pipelineCaptor.capture())).thenReturn(aggregateIterable);
        when(aggregateIterable.iterator()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);

        VectorSearchService service = new VectorSearchService(mongoTemplate, embeddingService);

        service.searchByEmbedding(List.of(0.25, -0.5, 1.0), 3);

        Document vectorSearch = pipelineCaptor.getValue().getFirst().get("$vectorSearch", Document.class);
        Object queryVector = vectorSearch.get("queryVector");

        assertThat(queryVector).isInstanceOf(BsonBinary.class);
    }

    @Test
    void searchByEmbedding_acceptsMongoFloat32BinaryVectorValues() {
        MongoDatabase database = mock(MongoDatabase.class);
        MongoCollection<Document> collection = mock(MongoCollection.class);
        AggregateIterable<Document> aggregateIterable = mock(AggregateIterable.class);
        MongoCursor<Document> cursor = mock(MongoCursor.class);
        ArgumentCaptor<List<Document>> pipelineCaptor = ArgumentCaptor.forClass(List.class);

        when(mongoTemplate.getCollectionName(com.hackathon.backend.models.EmbeddedMovie.class))
                .thenReturn("embedded_movies");
        when(mongoTemplate.getDb()).thenReturn(database);
        when(database.getCollection("embedded_movies")).thenReturn(collection);
        when(collection.aggregate(pipelineCaptor.capture())).thenReturn(aggregateIterable);
        when(aggregateIterable.iterator()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);

        VectorSearchService service = new VectorSearchService(mongoTemplate, embeddingService);

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<Double> mongoVector = (List) List.of(BinaryVector.floatVector(new float[]{0.25f, -0.5f, 1.0f}));
        service.searchByEmbedding(mongoVector, 3);

        Document vectorSearch = pipelineCaptor.getValue().getFirst().get("$vectorSearch", Document.class);
        Object queryVector = vectorSearch.get("queryVector");

        assertThat(queryVector).isInstanceOf(BsonBinary.class);
    }
}
