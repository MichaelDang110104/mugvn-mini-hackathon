package com.hackathon.backend;

import com.hackathon.backend.config.StarterQueryLlmProperties;
import com.hackathon.backend.kafka.KafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({StarterQueryLlmProperties.class, KafkaTopicsProperties.class})
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
