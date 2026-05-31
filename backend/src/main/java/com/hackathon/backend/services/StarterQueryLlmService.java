package com.hackathon.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.backend.config.StarterQueryLlmProperties;
import com.hackathon.backend.dto.StarterQueryPackage;
import com.hackathon.backend.models.RecommendationProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StarterQueryLlmService {

    private static final long LLM_QUERY_VERSION = 2L;

    private final StarterQueryLlmProperties props;
    private final StarterQueryService deterministic;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<ChatModel> chatModelProvider;
    @Cacheable(value = "starterQuery", key = "T(java.util.Objects).toString(#profile != null ? #profile.userId : '') + ':' + T(java.util.Objects).toString(#profile != null ? #profile.profileVersion : 0) + ':' + T(java.lang.Integer).toHexString((#freeText == null ? '' : #freeText).hashCode())")
    public StarterQueryPackage buildForProfile(RecommendationProfile profile, String freeText) {
        if (profile == null) {
            return StarterQueryPackage.builder()
                    .starterQueryText("")
                    .queryKeywords(List.of())
                    .querySummary("")
                    .llmModel("deterministic-fallback")
                    .queryVersion(1L)
                    .build();
        }

        if (!props.enabled()) {
            return StarterQueryService.buildDeterministicQuery(profile, freeText);
        }

        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return StarterQueryService.buildDeterministicQuery(profile, freeText);
        }

        try {
            StarterQueryPromptInput input = StarterQueryPromptInput.from(profile, freeText);

            String system = "You are a movie search query generator. " +
                    "Given onboarding preferences, produce a concise query text that will be embedded for semantic retrieval. " +
                    "Return JSON only, with fields: queryText (string, 1-2 sentences) and keywords (string array, optional).";

            String user = "Input JSON:\n{{inputJson}}\n\n" +
                    "Rules:\n" +
                    "- Output MUST be valid JSON and nothing else.\n" +
                    "- queryText must be short (max 2 sentences).\n" +
                    "- If avoidGenres/avoidThemes exist, include a short 'avoid ...' constraint in queryText.\n";

            String inputJson = objectMapper.writeValueAsString(input);

            Prompt prompt = new Prompt(List.of(
                    new SystemPromptTemplate(system).createMessage(),
                    new PromptTemplate(user).createMessage(Map.of("inputJson", inputJson))
            ), OpenAiChatOptions.builder()
                    .model(props.model())
                    .temperature(props.temperature())
                    .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                            .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT)
                            .build())
                    .build());

            String content = chatModel.call(prompt).getResult().getOutput().getText();

            StarterQueryLlmOutput parsed = parseOutput(content);
            if (parsed == null || parsed.queryText == null || parsed.queryText.isBlank()) {
                log.debug("LLM starter query invalid/blank; falling back to deterministic query");
                return StarterQueryService.buildDeterministicQuery(profile, freeText);
            }

            String queryText = clamp(parsed.queryText.trim(), 600);
            List<String> keywords = parsed.keywords != null ? parsed.keywords : List.of();

            return StarterQueryPackage.builder()
                    .starterQueryText(queryText)
                    .queryKeywords(keywords)
                    .querySummary("Starter taste profile (llm): " + queryText)
                    .llmModel(props.model())
                    .queryVersion(LLM_QUERY_VERSION)
                    .build();

        } catch (Exception e) {
            log.warn("LLM starter query build failed; falling back to deterministic query: {}", e.getMessage());
            return StarterQueryService.buildDeterministicQuery(profile, freeText);
        }
    }

    private StarterQueryLlmOutput parseOutput(String content) {
        if (content == null || content.isBlank()) return null;

        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }

        int jsonStart = trimmed.indexOf('{');
        int jsonEnd = trimmed.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            trimmed = trimmed.substring(jsonStart, jsonEnd + 1);
        }

        try {
            JsonNode node = objectMapper.readTree(trimmed);
            String queryText = node.hasNonNull("queryText") ? node.get("queryText").asText() : null;

            List<String> keywords = new ArrayList<>();
            if (node.has("keywords") && node.get("keywords").isArray()) {
                for (JsonNode kw : node.get("keywords")) {
                    if (kw != null && kw.isTextual()) {
                        String v = kw.asText();
                        if (v != null && !v.isBlank()) {
                            keywords.add(clamp(v.trim(), 60));
                        }
                    }
                }
            }

            if (keywords.size() > 20) {
                keywords = keywords.subList(0, 20);
            }

            return new StarterQueryLlmOutput(queryText, keywords);
        } catch (Exception e) {
            return null;
        }
    }

    private String clamp(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }

    record StarterQueryPromptInput(
            List<String> selectedGenres,
            List<String> selectedThemes,
            List<String> favoriteTitles,
            List<String> preferredLanguages,
            String preferredEra,
            String preferredPace,
            List<String> avoidGenres,
            List<String> avoidThemes,
            String freeTextTasteSummary
    ) {
        static StarterQueryPromptInput from(RecommendationProfile profile, String freeText) {
            return new StarterQueryPromptInput(
                    safe(profile.getTopGenres()),
                    safe(profile.getTopThemes()),
                    safe(profile.getFavoriteTitles()),
                    safe(profile.getPreferredLanguages()),
                    profile.getPreferredEra(),
                    profile.getPreferredPace(),
                    safe(profile.getAvoidedGenres()),
                    safe(profile.getAvoidedThemes()),
                    freeText
            );
        }

        private static List<String> safe(List<String> v) {
            return v == null ? List.of() : v;
        }
    }

    record StarterQueryLlmOutput(String queryText, List<String> keywords) {
    }
}
