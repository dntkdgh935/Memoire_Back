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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

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

        try {
            return restTemplate.postForObject(pythonBaseUrl + "/generate-tts", request, String.class);
        } catch (Exception ex){
            throw new TtsSyncException("음성 생성 중 오류 발생: "+ ex.getMessage());
        }
    }

    public VideoResultDto generateVideo(VideoGenerationRequest req) {
        String rawVideoUrl;

        if (Boolean.TRUE.equals(req.getLipSyncEnabled())) {
            String imageAssetId = uploadImageAsset(req.getImageUrl());
            String audioAssetId = uploadAudioAsset(req.getTtsUrl());
            rawVideoUrl = restTemplate.postForObject(
                    pythonBaseUrl + "/runway/lip-sync",
                    Map.of("image_asset_id", imageAssetId, "audio_asset_id", audioAssetId),
                    String.class
            );
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 2. 폼 데이터 (form-urlencoded 형식)
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("video_noperson_raw", req.getVideoPrompt());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    pythonBaseUrl + "/atelier/openai/generate-video-background-prompt",
                    entity,
                    Map.class
            );

            String refinedVideoPrompt = (String) response.getBody().get("prompt");
            log.info("refinedVideoPrompt is {}", refinedVideoPrompt);

            log.info("imageUrl: {}", req.getImageUrl());
            rawVideoUrl = restTemplate.postForObject(
                    pythonBaseUrl + "/atelier/runway/generate-image-video",
                    Map.of(
                            "image_url", req.getImageUrl(),
                            "prompt", refinedVideoPrompt
                    ),
                    String.class
            );
        }

        // Vision 호출
        String imageDescription = restTemplate.postForObject(
                pythonBaseUrl + "/atelier/vision/analyze-image",
                Map.of("imageUrl", req.getImageUrl()),
                String.class
        );
        log.info("imageDescription is {}", imageDescription);

        // 자연음 프롬프트 생성(GPT)
        String soundPrompt = restTemplate.postForObject(
                pythonBaseUrl + "/atelier/openai/generate-sound-prompt",
                Map.of("imageDescription", imageDescription),
                String.class
        );
        log.info("soundPrompt is {}", soundPrompt);

        AudioRequest audioReq = AudioRequest.builder()
                .prompt(soundPrompt)
                .duration(15)
                .numSteps(25)
                .build();
        String musicUrl = restTemplate.postForObject(
                pythonBaseUrl + "/atelier/stable/generate",
                audioReq,
                String.class
        );
        log.info("musicUrl is {}", musicUrl);

        FfmpegGenerationRequest ffmpegReq = FfmpegGenerationRequest.builder()
                .videoUrl(rawVideoUrl)
                .ttsUrl(req.getLipSyncEnabled() ? null : req.getTtsUrl())
                .musicUrl(musicUrl)
                .build();

        FfmpegGenerationResponse ffmpegRes = restTemplate.postForObject(
                pythonBaseUrl + "/atelier/ffmpeg/generate",
                ffmpegReq,
                FfmpegGenerationResponse.class
        );
        log.info("ffmpegRes is {}", ffmpegRes);

        return VideoResultDto.builder()
                .videoUrl(ffmpegRes.getProcessedVideoUrl())
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

