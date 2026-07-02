package com.codesec.gitlab;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(GitLabProperties.class)
public class GitLabConfig {

    @Bean
    public GitLabClient gitLabClient(GitLabProperties properties) {
        return new GitLabClient(properties);
    }

    @Bean
    public JsonMapper objectMapper() {
        return JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
    }
}
