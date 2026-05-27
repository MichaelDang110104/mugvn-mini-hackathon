package com.hackathon.backend.kafka;

import com.hackathon.backend.services.UserEventProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
        // Keep this test isolated from any real infra.
        "spring.mongodb.uri=mongodb://localhost:27017/unused",
        "spring.data.redis.host=localhost"
})
@EmbeddedKafka(partitions = 3, topics = {"event_log_test"})
class EmbeddedKafkaUserEventIntegrationTest {

    @TestConfiguration
    static class Overrides {
        @Bean
        UserEventProcessor userEventProcessor() {
            return mock(UserEventProcessor.class);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // Use the embedded broker for both producer + consumer.
        registry.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));
        registry.add("app.kafka.user-events-topic", () -> "event_log_test");
        registry.add("spring.kafka.consumer.group-id", () -> "user-event-processor-embedded-it");
        registry.add("app.kafka.user-event-processor-group", () -> "user-event-processor-embedded-it");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("app.kafka.user-event-consumer-concurrency", () -> "1");
    }

    @Autowired
    private KafkaTemplate<String, UserEventMessageV1> kafkaTemplate;

    @Autowired
    private UserEventProcessor userEventProcessor;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void publishesAndConsumesUserEvent_overEmbeddedKafka() {
        assertThat(embeddedKafkaBroker.getBrokersAsString()).isNotBlank();

        String eventId = "evt-" + UUID.randomUUID();
        UserEventMessageV1 msg = UserEventMessageV1.builder()
                .eventVersion(1)
                .eventId(eventId)
                .sessionId("session-1")
                .eventType("view")
                .build();

        kafkaTemplate.send("event_log_test", "session-1", msg).join();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var captor = forClass(UserEventMessageV1.class);
            verify(userEventProcessor, times(1)).process(captor.capture());
            assertThat(captor.getValue().getEventId()).isEqualTo(eventId);
            assertThat(captor.getValue().getSessionId()).isEqualTo("session-1");
            assertThat(captor.getValue().getEventType()).isEqualTo("view");
        });
    }
}
