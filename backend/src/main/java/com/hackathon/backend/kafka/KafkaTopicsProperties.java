package com.hackathon.backend.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaTopicsProperties {

    private String userEventsTopic;
    private String userEventProcessorGroup = "user-event-processor";
    private Integer userEventConsumerConcurrency = 1;
}
