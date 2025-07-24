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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

        try {
            return restTemplate.postForObject(pythonBaseUrl + "/generate-tts", request, String.class);
        } catch (Exception ex){
            throw new TtsSyncException("음성 생성 중 오류 발생: "+ ex.getMessage());
        }
    }

    public VideoResultDto generateVideo(VideoGenerationRequest req) throws IOException {
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
            String imageUrl = req.getImageUrl();
            String fileName = UUID.randomUUID().toString() + ".png";
            Path uploadDir = Paths.get("C:", "upload_files");
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(fileName);

            // 예시: URL에서 다운로드(혹은 메모리에 있는 이미지 byte라면 바로 저장)
            try (InputStream in = new URL(imageUrl).openStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            // 실제 파일 경로를 파이썬으로 넘길 준비
            String springImagePath = filePath.toString(); // "C:/upload_files/xxx.png"


            HttpHeaders formHeaders  = new HttpHeaders();
            formHeaders .setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> form  = new LinkedMultiValueMap<>();
            form.add("video_noperson_raw", req.getVideoPrompt());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    pythonBaseUrl + "/atelier/openai/generate-video-background-prompt",
                    new HttpEntity<>(form, formHeaders),
                    Map.class
            );

            String refinedVideoPrompt = (String) response.getBody().get("prompt");
            log.info("refinedVideoPrompt is {}", refinedVideoPrompt);

            HttpHeaders jsonHeaders = new HttpHeaders();
            jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> jsonBody = Map.of(
                    "image_path", springImagePath,
                    "prompt",     refinedVideoPrompt
            );

            HttpEntity<Map<String,String>> jsonentity = new HttpEntity<>(jsonBody, jsonHeaders);

            rawVideoUrl = restTemplate.postForObject(
                pythonBaseUrl + "/atelier/runway/generate-image-video",
                    jsonentity,
                    String.class
            );


        }

        HttpHeaders formHeaders = new HttpHeaders();
        formHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("image_url", req.getImageUrl());

        // Vision 호출
        String description = restTemplate.postForObject(
                pythonBaseUrl + "/atelier/vision/analyze-image",
                new LinkedMultiValueMap<String,String>() {{ add("image_url", req.getImageUrl()); }},
                String.class
        );
        log.info("imageDescription is {}", description);


        // 자연음 프롬프트 생성(GPT)
        MultiValueMap<String,String> soundForm = new LinkedMultiValueMap<>();
        soundForm.add("image_description", description);

        String soundPrompt = restTemplate.postForObject(
                pythonBaseUrl + "/atelier/openai/generate-sound-prompt",
                new HttpEntity<>(soundForm, formHeaders),
                String.class
        );
        log.info("soundPrompt is {}", soundPrompt);


        // 자연음 생성
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

        AudioRequest audioReq = AudioRequest.builder()
                .prompt(soundPrompt)
                .duration(15)
                .numSteps(25)
                .build();

        Map<String, String> stableResp = restTemplate.postForObject(
                pythonBaseUrl + "/atelier/stable/generate",
                new HttpEntity<>(audioReq, jsonHeaders),
                Map.class
        );
        String musicUrl = stableResp.get("generated_natural_url");
        log.info("musicUrl is {}", musicUrl);


        // 합성 (ffmpeg)
        FfmpegGenerationRequest ffmpegReq = FfmpegGenerationRequest.builder()
                .videoUrl(rawVideoUrl)
                .ttsUrl(req.getLipSyncEnabled()? null : req.getTtsUrl())
                .musicUrl(musicUrl)
                .build();

        FfmpegGenerationResponse ffmpegRes = restTemplate.postForObject(
                pythonBaseUrl + "/atelier/ffmpeg/generate",
                new HttpEntity<>(ffmpegReq, jsonHeaders),
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

