package com.web.memoire.atelier.ImTIm.model.service;

import com.web.memoire.atelier.ImTIm.exception.ImageGenerationException;
import com.web.memoire.atelier.ImTIm.exception.StylePromptException;
import com.web.memoire.atelier.ImTIm.exception.invalidImageFormatException;
import com.web.memoire.atelier.ImTIm.model.dto.ImTImGenerationRequest;
import com.web.memoire.atelier.ImTIm.model.dto.ImTImResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class ImTImPythonApiService {

    private final RestTemplate restTemplate;
    private final String pythonBaseUrl;

    // 생성자에 @Qualifier를 명시적으로 붙이면 확실히 적용됩니다.
    @Autowired
    public ImTImPythonApiService(
            @Qualifier("imtimRestTemplate") RestTemplate restTemplate,
            @Value("${python.api.base-url:}") String pythonBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.pythonBaseUrl = pythonBaseUrl;
    }

    public ImTImResultDto callFastApiForImage(ImTImGenerationRequest req) {
        return restTemplate.postForObject(
                pythonBaseUrl + "/im2im/generate-image",
                req,
                ImTImResultDto.class
        );
    }

    private boolean isDisabled() {
        return pythonBaseUrl == null || pythonBaseUrl.isBlank();
    }

    public ImTImResultDto generateImage(ImTImGenerationRequest request) {
        if (isDisabled()) {
            return null;
        }
        HttpHeaders headers = restTemplate.headForHeaders(request.getImageUrl());
        MediaType contentType = headers.getContentType();

        if (contentType == null) {
            throw new invalidImageFormatException(
                    "이미지 URL의 Content-Type을 판별할 수 없습니다: " + request.getImageUrl()
            );
        }

        String mime = contentType.toString();  // 이제는 안전하게 toString() 호출 가능
        if (!mime.startsWith("image/")) {
            throw new invalidImageFormatException(
                    "지원되지 않는 이미지 형식입니다: " + mime
            );
        }
        if (request.getStylePrompt() == null || request.getStylePrompt().isBlank()) {
            throw new StylePromptException("스타일 프롬프트를 입력해주세요.");
        }

        try {
            return callFastApiForImage(request);
        } catch (Exception ex) {
            throw new ImageGenerationException("이미지 생성에 실패했습니다: " + ex.getMessage(), ex);
        }
    }
}
