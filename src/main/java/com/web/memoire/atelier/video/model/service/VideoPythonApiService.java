package com.web.memoire.atelier.video.model.service;

import com.web.memoire.atelier.video.model.dto.TtsPreviewRequest;
import com.web.memoire.atelier.video.model.dto.VideoGenerationRequest;
import com.web.memoire.atelier.video.model.dto.VideoResultDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoPythonApiService {

    private final RestTemplate restTemplate;
    private final String pythonBaseUrl;

    public VideoPythonApiService(@Qualifier("videoRestTemplate") RestTemplate restTemplate,
                                 @Value("${python.api.base-url:}") String pythonBaseUrl) {
        this.restTemplate = restTemplate;
        this.pythonBaseUrl = pythonBaseUrl;
    }

    private boolean isDisabled() {
        return pythonBaseUrl == null || pythonBaseUrl.isBlank();
    }

    public String uploadImage(MultipartFile file) {
        if (isDisabled()) {
            return null;
        }
        return restTemplate.postForObject(pythonBaseUrl + "/upload-image", file, String.class);
    }

    public String previewTTS(TtsPreviewRequest request) {
        if (isDisabled()) {
            return null;
        }
        return restTemplate.postForObject(pythonBaseUrl + "/preview-tts", request, String.class);
    }

    public String generateTTS(TtsPreviewRequest request) {
        if (isDisabled()) {
            return null;
        }
        return restTemplate.postForObject(pythonBaseUrl + "/generate-tts", request, String.class);
    }

    public VideoResultDto generateVideo(VideoGenerationRequest request) {
        if (isDisabled()) {
            return null;
        }
        return restTemplate.postForObject(pythonBaseUrl + "/generate-video", request, VideoResultDto.class);
    }

}

