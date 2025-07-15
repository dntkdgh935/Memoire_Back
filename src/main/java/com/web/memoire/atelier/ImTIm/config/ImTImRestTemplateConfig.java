package com.web.memoire.atelier.ImTIm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


@Configuration
public class ImTImRestTemplateConfig {
    @Bean("imtimRestTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

