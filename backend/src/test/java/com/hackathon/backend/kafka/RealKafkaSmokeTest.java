package com.hackathon.backend.kafka;

import com.hackathon.backend.services.UserEventProcessor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class RealKafkaSmokeTest {

    @TestConfiguration
    static class Mocks {
        @Bean
        UserEventProcessor userEventProcessor() {
            return mock(UserEventProcessor.class);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        String bootstrap = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        final String topic = resolveTopic();

        // If env isn't set, keep the test skipped (see @Test).
        if (bootstrap != null && !bootstrap.isBlank()) {
            registry.add("spring.kafka.bootstrap-servers", () -> bootstrap);
            registry.add("app.kafka.user-events-topic", () -> topic);
            registry.add("spring.kafka.consumer.group-id", () -> "user-event-processor-smoke");
            registry.add("app.kafka.user-event-processor-group", () -> "user-event-processor-smoke");
            registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
            registry.add("app.kafka.user-event-consumer-concurrency", () -> "1");
        }
    }

    private static String resolveTopic() {
        String topic = System.getenv("KAFKA_USER_EVENTS_TOPIC");
        if (topic == null || topic.isBlank()) {
            return "user-events.v1";
        }
        return topic;
    }

    @Autowired
    private KafkaTemplate<String, UserEventMessageV1> kafkaTemplate;

    @Autowired
    private UserEventProcessor userEventProcessor;

    @Test
    void consumesFromRealKafka_whenBootstrapServersProvided() {
        String bootstrap = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        Assumptions.assumeTrue(bootstrap != null && !bootstrap.isBlank(),
                "Set KAFKA_BOOTSTRAP_SERVERS to run this real-Kafka smoke test");

        String topic = resolveTopic();

        String eventId = "evt-" + UUID.randomUUID();
        UserEventMessageV1 msg = UserEventMessageV1.builder()
                .eventVersion(1)
                .eventId(eventId)
                .sessionId("session-1")
                .eventType("view")
                .build();

        kafkaTemplate.send(topic, "session-1", msg).join();

        // If the topic doesn't exist or the consumer can't reach the broker, the listener won't fire.
        // Bump timeout a bit to reduce false negatives on slow VPS brokers.

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                verify(userEventProcessor, atLeastOnce()).process(org.mockito.ArgumentMatchers.any())
        );
    }
}
