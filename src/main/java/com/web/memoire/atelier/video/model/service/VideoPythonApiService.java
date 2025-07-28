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
import org.springframework.web.client.RestClientException;
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

    public String generateTTS(TtsConfigRequest request) {
        if (isDisabled()) {
            return null;
        }
        if(request.getScript() == null || request.getScript().isBlank()) {
            throw new TtsSyncException("TTS 텍스트를 입력해주세요.");
        }
        if (request.getGender() == null) {
            throw new TtsSyncException("음성 성별(voiceGender)을 선택해주세요.");
        }
        log.info("request : {}", request);

        try {
            HttpHeaders jsonHeaders = new HttpHeaders();
            jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> configPayload = Map.of(
                    "script",      request.getScript(),
                    "voiceGender", request.getGender()
            );
            HttpEntity<Map<String, Object>> configEntity =
                    new HttpEntity<>(configPayload, jsonHeaders);

            TtsConfigResponse cfg = restTemplate.postForObject(
                    pythonBaseUrl + "/atelier/openai/generate-tts-config",
                    configEntity,
                    TtsConfigResponse.class
            );
            if (cfg == null) {
                throw new TtsSyncException("TTS 설정 분석 실패: 빈 응답");
            }
            log.info("cfg : {}", cfg);
            log.info("speech : {}", request.getSpeech());

            Map<String,Object> genPayload = Map.of(
                    "speech",    request.getSpeech(),
                    "voice_id",  cfg.getVoiceId(),
                    "model_id",  cfg.getModelId(),
                    "stability",     cfg.getStability(),
                    "similarity_boost", cfg.getSimilarity_boost()
            );

            HttpEntity<Map<String,Object>> genEntity =
                    new HttpEntity<>(genPayload, jsonHeaders);

            String audioUrl = restTemplate.postForObject(
                    pythonBaseUrl + "/atelier/tts/generate",
                    genEntity,
                    String.class
            );
            if (audioUrl == null) {
                throw new TtsSyncException("TTS 생성 실패: 빈 URL");
            }
            log.info("audioUrl : {}", audioUrl);
            return audioUrl;

        } catch (RestClientException ex) {
            throw new TtsSyncException("음성 생성 중 오류 발생: " + ex.getMessage(), ex);
        }
    }

    public VideoResultDto generateVideo(VideoGenerationRequest req) throws IOException {
        if (isDisabled()) {
            return null;
        }
        String rawVideoUrl;
        log.info("request : {}", req);

        if (Boolean.TRUE.equals(req.getLipSyncEnabled())) {
            rawVideoUrl = restTemplate.postForObject(
                    pythonBaseUrl + "/atelier/runway/lipsync",
                    Map.of("image_url", req.getImageUrl(), "audio_url", req.getTtsUrl()),
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
            body.put("tts_url",   req.getTtsUrl());
            log.info("body : {}", body);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, jsonHeaders);

            ResponseEntity<Map> videoRes = restTemplate.postForEntity(
                    pythonBaseUrl + "/atelier/video/generate-video",
                    entity,
                    Map.class
            );

            String baseVideoUrl  = (String) videoRes.getBody().get("video_url");
            log.info("baseVideoUrl is {}", baseVideoUrl);


            if (req.getTtsUrl() != null) {
                log.info("ttsUrl is {}", req.getTtsUrl());
                FfmpegGenerationResponse res = restTemplate.postForObject(
                        pythonBaseUrl + "/atelier/ffmpeg/generate",
                        Map.of("video_url", baseVideoUrl, "tts_url", req.getTtsUrl()),
                        FfmpegGenerationResponse.class
                );
                rawVideoUrl = res.getVideo_url();
            } else {
                rawVideoUrl = baseVideoUrl;
            }
        }
        return VideoResultDto.builder()
                .videoUrl(rawVideoUrl)
                .build();
    }
}

