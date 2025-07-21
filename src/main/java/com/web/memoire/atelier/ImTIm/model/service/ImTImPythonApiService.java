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

        Map<String,String> refineReq = Map.of(
                "tts_raw",        "",
                "video_person_raw","",
                "video_noperson_raw","",
                "image_raw",      request.getStylePrompt()
        );
        ResponseEntity<PromptRefinementResponse> refResp = restTemplate.postForEntity(
                pythonBaseUrl + "/atelier/openai/refine-prompts",
                refineReq,
                PromptRefinementResponse.class
        );
        log.info("Refine response: {}", refResp.getBody());
        if (!refResp.getStatusCode().is2xxSuccessful() || refResp.getBody() == null) {
            throw new ImageGenerationException("프롬프트 정제 실패");
        }
        String refinedPrompt = refResp.getBody().getImagePrompt();

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

            byte[] imageBytes = restTemplate.getForObject(generatedUrl, byte[].class);
            if (imageBytes == null || imageBytes.length == 0) {
                throw new ImageGenerationException("다운로드된 이미지가 없습니다");
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
