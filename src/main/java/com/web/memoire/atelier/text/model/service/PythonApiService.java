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

    public TextResultDto callGpt(TextGenerationRequest request) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TextGenerationRequest> entity = new HttpEntity<>(request, headers);

        try {
            // 요청 JSON 출력
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(request);
            System.out.println("🔗 FastAPI 호출 주소 = " + textApiUrl);
            System.out.println("📨 보낼 JSON = " + jsonBody);

            // 실제 호출
            ResponseEntity<TextResultDto> response = restTemplate.exchange(
                    textApiUrl,
                    HttpMethod.POST,
                    entity,
                    TextResultDto.class
            );

            System.out.println("✅ GPT 결과 수신 완료, 제목: " + response.getBody().getTitle());
            return response.getBody();

        } catch (JsonProcessingException e) {
            System.out.println("❌ JSON 직렬화 오류");
            e.printStackTrace();
            throw new TextGenerationException("요청 JSON 직렬화 실패", e);

        } catch (HttpStatusCodeException e) {
            System.out.println("❌ FastAPI 응답 오류 - 상태 코드: " + e.getStatusCode());
            System.out.println("❌ 응답 바디: " + e.getResponseBodyAsString());
            throw new TextGenerationException("텍스트 생성 중 오류 발생", e);

        } catch (Exception e) {
            System.out.println("❌ 예기치 못한 오류 발생");
            e.printStackTrace();
            throw new TextGenerationException("예기치 못한 오류", e);
        }
    }

    public ImageResultDto callDalle(ImagePromptRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ImagePromptRequest> entity = new HttpEntity<>(request, headers);

        try {
            System.out.println("📦 보낼 JSON: " + new ObjectMapper().writeValueAsString(request));
            System.out.println("🔗 FastAPI 호출 주소 = " + imageApiUrl);  // 절대 "http://" + imageApiUrl 하지 마세요
            return restTemplate.exchange(
                    imageApiUrl,
                    HttpMethod.POST,
                    entity,
                    ImageResultDto.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            System.out.println("❌ DALL·E 응답 오류 - 상태 코드: " + e.getStatusCode());
            System.out.println("❌ 응답 바디: " + e.getResponseBodyAsString());
            throw new RuntimeException("이미지 생성 중 오류 발생", e);
        } catch (Exception e) {
            System.out.println("❌ 예기치 못한 이미지 생성 오류");
            e.printStackTrace();
            throw new RuntimeException("예기치 못한 이미지 생성 오류", e);
        }
    }

}