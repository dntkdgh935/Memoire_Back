package com.web.memoire.atelier.text.model.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.memoire.atelier.text.exception.TextGenerationException;
import com.web.memoire.atelier.text.model.dto.ImagePromptRequest;
import com.web.memoire.atelier.text.model.dto.ImageResultDto;
import com.web.memoire.atelier.text.model.dto.TextGenerationRequest;
import com.web.memoire.atelier.text.model.dto.TextResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class PythonApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${python.text-url}")
    private String textApiUrl;

    @Value("${python.image-url}")
    private String imageApiUrl;

    /**
     * ChatGPT 호출용 메서드 (텍스트 생성)
     */
    public TextResultDto callGpt(TextGenerationRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TextGenerationRequest> entity = new HttpEntity<>(request, headers);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(request);
            System.out.println("🔗 FastAPI Text URL = " + textApiUrl);
            System.out.println("📨 Request JSON = " + jsonBody);

            ResponseEntity<TextResultDto> response = restTemplate.exchange(
                    textApiUrl,
                    HttpMethod.POST,
                    entity,
                    TextResultDto.class
            );

            System.out.println("✅ Text generation success, title=" + response.getBody().getTitle());
            return response.getBody();

        } catch (JsonProcessingException e) {
            throw new TextGenerationException("JSON serialization failed", e);
        } catch (HttpStatusCodeException e) {
            System.out.println("❌ FastAPI Text error code=" + e.getStatusCode());
            System.out.println("❌ Body: " + e.getResponseBodyAsString());
            throw new TextGenerationException("Text generation error", e);
        } catch (Exception e) {
            throw new TextGenerationException("Unexpected text generation error", e);
        }
    }

    /**
     * DALL·E 이미지 생성 호출
     */
    public ImageResultDto callDalle(ImagePromptRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ImagePromptRequest> entity = new HttpEntity<>(request, headers);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(request);
            System.out.println("🔗 FastAPI Image URL = " + imageApiUrl);
            System.out.println("📨 Request JSON = " + jsonBody);

            ResponseEntity<ImageResultDto> response = restTemplate.exchange(
                    imageApiUrl,
                    HttpMethod.POST,
                    entity,
                    ImageResultDto.class
            );

            ImageResultDto dto = response.getBody();
            System.out.println("✅ Image generation success, url=" + dto.getImageUrl());
            return dto;

        } catch (HttpStatusCodeException e) {
            System.out.println("❌ FastAPI Image error code=" + e.getStatusCode());
            System.out.println("❌ Body: " + e.getResponseBodyAsString());
            throw new TextGenerationException("Image generation error", e);
        } catch (Exception e) {
            throw new TextGenerationException("Unexpected image generation error", e);
        }
    }
}