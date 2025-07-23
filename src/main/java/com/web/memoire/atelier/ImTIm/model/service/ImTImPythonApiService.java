package com.web.memoire.atelier.ImTIm.model.service;

import com.web.memoire.atelier.ImTIm.exception.ImageGenerationException;
import com.web.memoire.atelier.ImTIm.exception.StylePromptException;
import com.web.memoire.atelier.ImTIm.exception.invalidImageFormatException;
import com.web.memoire.atelier.ImTIm.model.dto.ImTImGenerationRequest;
import com.web.memoire.atelier.ImTIm.model.dto.ImTImResultDto;
import com.web.memoire.atelier.ImTIm.model.dto.PromptRefinementResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
public class ImTImPythonApiService {

    private final RestTemplate restTemplate;
    private final String pythonBaseUrl;

    // 생성자에 @Qualifier를 명시적으로 붙이면 확실히 적용됩니다.
    @Autowired
    public ImTImPythonApiService(
            @Qualifier("imtimRestTemplate") RestTemplate restTemplate,
            @Value("${fastapi.base-url:}") String pythonBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.pythonBaseUrl = pythonBaseUrl;
    }

    private boolean isDisabled() {
        return pythonBaseUrl == null || pythonBaseUrl.isBlank();
    }

    public ImTImResultDto generateImage(ImTImGenerationRequest request) {
        log.info("Generating image for {}", request);
        if (isDisabled()) {
            log.warn("Python base URL is not configured, but proceeding without it.");
        }

        if (request.getStylePrompt() == null || request.getStylePrompt().isBlank()) {
            throw new StylePromptException("스타일 프롬프트를 입력해주세요.");
        }

        String filename = UUID.randomUUID().toString() + ".png";
        log.info("filename is {}", filename);

        String url = pythonBaseUrl + "/atelier/image-1/generate";
        log.info("▶️ Calling Python at URL: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    // 2. 폼 데이터 (form-urlencoded 형식)
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("image_raw", request.getStylePrompt());

    // 3. HttpEntity로 포장
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

    // 4. postForEntity 사용 (반환값은 ResponseEntity<Map>)
        ResponseEntity<Map> response = restTemplate.postForEntity(
                pythonBaseUrl + "/atelier/openai/generate-image-prompt",
                entity,
                Map.class
        );

        String refinedPrompt = (String) response.getBody().get("prompt");
        log.info("refinedPrompt is {}", refinedPrompt);


        try {
            Map<String, String> fastApiPayload = Map.of(
                    "prompt",     refinedPrompt,
                    "image_url",  request.getImageUrl()
            );
            ResponseEntity<Map<String, String>> respEntity = restTemplate.exchange(
                    pythonBaseUrl + "/atelier/image-1/generate",
                    HttpMethod.POST,
                    new HttpEntity<>(fastApiPayload),
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, String> fastResp = respEntity.getBody();
            if (fastResp == null || !fastResp.containsKey("generated_image_url")) {
                throw new ImageGenerationException("FastAPI 응답에 generated_image_url 필드가 없습니다");
            }
            String generatedUrl = fastResp.get("generated_image_url");

            byte[] imageBytes;
            if (generatedUrl.startsWith("http://") || generatedUrl.startsWith("https://")) {
                // 진짜 HTTP URL 이면 RestTemplate 으로 다운로드
                imageBytes = restTemplate.getForObject(generatedUrl, byte[].class);
            } else {
                // 로컬 파일 경로면 Java NIO 로 직접 읽기
                Path path = Paths.get(generatedUrl);
                if (!Files.exists(path)) {
                    throw new ImageGenerationException("다운로드할 파일이 없습니다: " + generatedUrl);
                }
                imageBytes = Files.readAllBytes(path);
            }

            // 파일 저장
            Path uploadDir = Paths.get("C:", "upload_files", "memory_img");
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(filename);
            Files.write(filePath, imageBytes);

            // DTO 반환
            ImTImResultDto resultDto = new ImTImResultDto();
            resultDto.setImageUrl(generatedUrl);
            resultDto.setFilename(filename);
            resultDto.setFilepath("/upload_files/memory_img");
            return resultDto;
        } catch (Exception ex) {
            throw new ImageGenerationException("이미지 생성에 실패했습니다: " + ex.getMessage(), ex);
        }
    }
}
