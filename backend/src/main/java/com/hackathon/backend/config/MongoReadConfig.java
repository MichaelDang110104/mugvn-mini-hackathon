package com.hackathon.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class MongoReadConfig {

    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new LenientStringToIntegerConverter()));
    }

    @ReadingConverter
    static class LenientStringToIntegerConverter implements Converter<String, Integer> {
        private static final Pattern LEADING_INTEGER = Pattern.compile("^[\\s]*([+-]?\\d+)");

        @Override
        public Integer convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }

            Matcher matcher = LEADING_INTEGER.matcher(source);
            if (!matcher.find()) {
                return null;
            }

            try {
                return Integer.valueOf(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}
