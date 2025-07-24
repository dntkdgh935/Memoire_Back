package com.web.memoire.atelier.video.model.service;

import com.web.memoire.atelier.video.exception.TtsSyncException;
import com.web.memoire.atelier.video.model.dto.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class VideoPythonApiService {

    private final RestTemplate restTemplate;
    private final String pythonBaseUrl;

    public VideoPythonApiService(@Qualifier("videoRestTemplate") RestTemplate restTemplate,
                                 @Value("${fastapi.base-url:}") String pythonBaseUrl) {
        this.restTemplate = restTemplate;
        this.pythonBaseUrl = pythonBaseUrl;
    }

    private boolean isDisabled() {
        return pythonBaseUrl == null || pythonBaseUrl.isBlank();
    }

    public String generateTTS(TtsPreviewRequest request) {
        if (isDisabled()) {
            return null;
        }
        if(request.getScript() == null || request.getScript().isBlank()) {
            throw new TtsSyncException("TTS 텍스트를 입력해주세요.");
        }
        log.info("request : {}", request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            HttpEntity<TtsPreviewRequest> entity = new HttpEntity<>(request, headers);
            return restTemplate.postForObject(
                    pythonBaseUrl + "/atelier/tts/generate",
                    entity,
                    String.class
            );
        } catch (Exception ex){
            throw new TtsSyncException("음성 생성 중 오류 발생: "+ ex.getMessage());
        }
    }

    public VideoResultDto generateVideo(VideoGenerationRequest req) throws IOException {
        if (isDisabled()) {
            return null;
        }
        String rawVideoUrl;

        if (Boolean.TRUE.equals(req.getLipSyncEnabled())) {
            String imageAssetId = uploadImageAsset(req.getImageUrl());
            String audioAssetId = uploadAudioAsset(req.getTtsUrl());
            rawVideoUrl = restTemplate.postForObject(
                    pythonBaseUrl + "/atelier/runway/generate-lip-sync-video",
                    Map.of("image_asset_id", imageAssetId, "audio_asset_id", audioAssetId),
                    String.class
            );
        } else {
            String imageUrl = req.getImageUrl();
            String fileName = UUID.randomUUID().toString() + ".png";
            Path uploadDir = Paths.get("C:", "upload_files/memory_img");
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(fileName);

            try (InputStream in = new URL(imageUrl).openStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            String springImagePath = filePath.toString(); // "C:/upload_files/xxx.png"


            HttpHeaders jsonHeaders = new HttpHeaders();
            jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("image_url", springImagePath);
            body.put("prompt",    req.getVideoPrompt());
            body.put("tts_url",   req.getLipSyncEnabled() ? req.getTtsUrl() : null);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, jsonHeaders);

            ResponseEntity<Map> videoRes = restTemplate.postForEntity(
                    pythonBaseUrl + "/atelier/video/generate-video",
                    entity,
                    Map.class
            );

            rawVideoUrl = (String) videoRes.getBody().get("video_url");
            log.info("rawVideoUrl is {}", rawVideoUrl);
        }
        return VideoResultDto.builder()
                .videoUrl(rawVideoUrl)
                .build();
    }

    public VideoResultDto completeVideo(FfmpegGenerationRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<FfmpegGenerationRequest> mediaEntity = new HttpEntity<>(req, headers);
        FfmpegGenerationResponse resp = restTemplate.postForObject(
                pythonBaseUrl + "/atelier/ffmpeg/generate",
                mediaEntity,
                FfmpegGenerationResponse.class
        );
        String finalUrl = resp.getProcessedVideoUrl();
        log.info("finalVideoUrl = {}", finalUrl);

        return VideoResultDto.builder()
                .videoUrl(finalUrl)
                .build();
    }

    private String uploadImageAsset(String imageUrl) {
        return restTemplate.postForObject(
                pythonBaseUrl + "/runway/upload-image-asset",
                Map.of("image_url", imageUrl),
                String.class
        );
    }
    private String uploadAudioAsset(String ttsUrl) {
        return restTemplate.postForObject(
                pythonBaseUrl + "/runway/upload-audio-asset",
                Map.of("audio_bytes_uri", ttsUrl),
                String.class
        );
    }

}

