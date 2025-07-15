package com.web.memoire.atelier.ImTIm.model.service;

import com.web.memoire.atelier.ImTIm.model.dto.ImTImGenerationRequest;
import com.web.memoire.atelier.ImTIm.model.dto.ImTImResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;


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
        return callFastApiForImage(request);
    }

    public String uploadImage(MultipartFile file) {
        if (isDisabled()) {
            return null;
        }
        return restTemplate.postForObject(pythonBaseUrl + "/upload-image", file, String.class);
    }
}
