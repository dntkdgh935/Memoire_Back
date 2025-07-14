package com.web.memoire.atelier.video.model.service;

import com.web.memoire.common.dto.Collection;
import com.web.memoire.common.dto.Memory;
import com.web.memoire.atelier.video.model.dto.TtsPreviewRequest;
import com.web.memoire.atelier.video.model.dto.VideoGenerationRequest;
import com.web.memoire.atelier.video.model.dto.VideoResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final PythonApiService pythonApiService;
    private final MemoryService memoryService;


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

    public void handleFailure() {
        pythonApiService.handleFailure();
    }

    public void saveVideo(String userId, VideoResultDto resultDto) {
        memoryService.insertMemory(resultDto);
    }

}
