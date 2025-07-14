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

    @Value("${python.text-url}")
    private String textApiUrl;

    // 수정된 URL: FastAPI에 매핑된 정확한 엔드포인트
    @Value("${python.image-url}")
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

        // 👇 여기 경로가 정확해야 합니다.
        ResponseEntity<ImageResultDto> response = restTemplate.exchange(
                imageApiUrl, // application.yml 또는 properties에서 정확히 설정해야 함
                HttpMethod.POST,
                entity,
                ImageResultDto.class
        );

        return response.getBody();
    }
}