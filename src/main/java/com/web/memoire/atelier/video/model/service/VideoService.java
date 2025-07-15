package com.web.memoire.atelier.video.model.service;

import com.web.memoire.atelier.video.model.dto.TtsPreviewRequest;
import com.web.memoire.atelier.video.model.dto.VideoGenerationRequest;
import com.web.memoire.atelier.video.model.dto.VideoResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoPythonApiService pythonApiService;
    private final VideoMemoryService memoryService;


    public String uploadImage(MultipartFile file) {
        return pythonApiService.uploadImage(file);
    }

    public String generateTTS(TtsPreviewRequest requestDto) {
        return pythonApiService.generateTTS(requestDto);
    }

    public String previewTTS(TtsPreviewRequest requestDto) {
        return pythonApiService.previewTTS(requestDto);
    }

    public VideoResultDto generateVideo(VideoGenerationRequest requestDto) {
        return pythonApiService.generateVideo(requestDto);
    }
}
