package com.web.memoire.atelier.video.controller;

import com.web.memoire.atelier.video.model.dto.TtsPreviewRequest;
import com.web.memoire.atelier.video.model.dto.VideoGenerationRequest;
import com.web.memoire.atelier.video.model.dto.VideoResultDto;
import com.web.memoire.atelier.video.model.service.VideoCollectionService;
import com.web.memoire.atelier.video.model.service.VideoMemoryService;
import com.web.memoire.atelier.video.model.service.VideoPythonApiService;
import com.web.memoire.common.dto.Collection;
import com.web.memoire.common.dto.Memory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/atelier/video")
public class VideoController {

    private final VideoCollectionService collectionService;
    private final VideoMemoryService memoryService;
    private final VideoPythonApiService videopythonApiService;

    @Autowired
    public VideoController(VideoCollectionService collectionService,
                           VideoMemoryService memoryService,
                           VideoPythonApiService videopythonApiService) {
        this.collectionService = collectionService;
        this.memoryService = memoryService;
        this.videopythonApiService = videopythonApiService;

    }

    @GetMapping("/collections/{userId}")
    public ResponseEntity<List<Collection>> getCollections(@PathVariable("userId") String userId) {
        List<Collection> collections = collectionService.getCollectionsByUserId(userId);
        return ResponseEntity.ok(collections);
    }

    @GetMapping("/collections/{collectionId}/memories")
    public ResponseEntity<List<Memory>> getMemoriesByCollection(
            @PathVariable("collectionId") int collectionId) {
        List<Memory> memories = memoryService.getMemoriesByCollectionId(collectionId);
        return ResponseEntity.ok(memories);
    }



    @PostMapping("/generate-tts")
    public ResponseEntity<String> generateTTS(@RequestBody TtsPreviewRequest requestDto) {
        String ttsUrl = videopythonApiService.generateTTS(requestDto);
        return ResponseEntity.ok(ttsUrl);
    }


    @PostMapping("/generate-video")
    public ResponseEntity<VideoResultDto> generateVideo(@RequestBody VideoGenerationRequest requestDto) {
        VideoResultDto result = videopythonApiService.generateVideo(requestDto);
        return ResponseEntity.ok(result);
    }



    @PostMapping("/{collectionId}")
    public ResponseEntity<Memory> createMemory(
            @PathVariable int collectionId,
            @RequestBody VideoResultDto videoResultDto
    ) throws ParseException {

        memoryService.createMemory(
                collectionId,
                videoResultDto
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/save/{memoryId}")
    public ResponseEntity<Memory> updateMemory(
            @PathVariable String memoryId,
            @RequestBody VideoResultDto videoResultDto) throws ParseException {
        memoryService.updateExisting(memoryId, videoResultDto);
        return ResponseEntity.noContent().build();
    }
}
