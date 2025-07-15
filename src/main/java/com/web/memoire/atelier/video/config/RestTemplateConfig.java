package com.web.memoire.atelier.video.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean("videoRestTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
