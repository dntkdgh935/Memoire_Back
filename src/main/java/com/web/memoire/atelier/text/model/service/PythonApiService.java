package com.web.memoire.atelier.text.model.service;

import com.web.memoire.atelier.text.model.dto.TextGenerationRequest;
import com.web.memoire.atelier.text.model.dto.TextResultDto;
import com.web.memoire.atelier.text.model.dto.ImagePromptRequest;
import com.web.memoire.atelier.text.model.dto.ImageResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class PythonApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${python.text-url}")   // 예: http://localhost:8000/gpt
    private String textApiUrl;

    @Value("${python.image-url}")  // 예: http://localhost:8000/dalle
    private String imageApiUrl;

    public TextResultDto callGpt(TextGenerationRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TextGenerationRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<TextResultDto> response = restTemplate.exchange(
                textApiUrl,
                HttpMethod.POST,
                entity,
                TextResultDto.class
        );

        return response.getBody();
    }

    public ImageResultDto callDalle(ImagePromptRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ImagePromptRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ImageResultDto> response = restTemplate.exchange(
                imageApiUrl,
                HttpMethod.POST,
                entity,
                ImageResultDto.class
        );

        return response.getBody();
    }
}