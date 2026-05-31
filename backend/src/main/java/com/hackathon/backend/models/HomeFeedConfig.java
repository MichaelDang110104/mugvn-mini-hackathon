package com.hackathon.backend.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "home_feed_config")
public class HomeFeedConfig {
    @Id
    private String id;
    private String audience;
    private boolean active;
    private List<SectionConfig> sections;
}
