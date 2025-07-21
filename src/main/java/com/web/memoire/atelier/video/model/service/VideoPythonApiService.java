package com.web.memoire.atelier.video.model.service;

import com.web.memoire.atelier.video.exception.TtsSyncException;
import com.web.memoire.atelier.video.model.dto.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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
        // 1) raw 비디오 생성
        String rawVideoUrl;
        if (req.getTtsUrl() != null && !req.getTtsUrl().isBlank()) {
            String imageAssetId = uploadImageAsset(req.getImageUrl());
            String audioAssetId = uploadAudioAsset(req.getTtsUrl());
            rawVideoUrl = restTemplate.postForObject(
                    pythonBaseUrl + "/runway/lip-sync",
                    Map.of("image_asset_id", imageAssetId, "audio_asset_id", audioAssetId),
                    String.class
            );
        } else {
            String prompt = req.getVideoPrompt()
                    + (req.getExtraPrompt() != null ? " " + req.getExtraPrompt() : "");
            rawVideoUrl = restTemplate.postForObject(
                    pythonBaseUrl + "/runway/generate-image-video",
                    Map.of(
                            "image_data_uri", req.getImageUrl(),
                            "prompt_text", prompt,
                            "model", "gen3a_turbo",
                            "ratio", "1280:720",
                            "duration", 10
                    ),
                    String.class
            );
        }

        // 2) 자연음 생성 (옵션)
        String musicUrl = null;

        AudioRequest audioReq = AudioRequest.builder()
                .prompt(req.getVideoPrompt())
                .duration(15)
                .numSteps(25)
                .build();
        musicUrl = restTemplate.postForObject(
                pythonBaseUrl + "/audio/stable-generate",
                audioReq,
                String.class
        );


        FfmpegGenerationRequest ffmpegReq = FfmpegGenerationRequest.builder()
                .videoUrl(rawVideoUrl)
                .ttsUrl(req.getTtsUrl())
                .musicUrl(musicUrl)
                .build();
        FfmpegGenerationResponse ffmpegRes = restTemplate.postForObject(
                pythonBaseUrl + "/ffmpeg/generate",
                ffmpegReq,
                FfmpegGenerationResponse.class
        );

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

