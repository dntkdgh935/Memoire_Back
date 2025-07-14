package com.web.memoire.atelier.video.controller;

import com.web.memoire.common.dto.Memory;
import com.web.memoire.common.dto.Collection;
import com.web.memoire.atelier.video.model.dto.TtsPreviewRequest;
import com.web.memoire.atelier.video.model.dto.VideoGenerationRequest;
import com.web.memoire.atelier.video.model.dto.VideoResultDto;
import com.web.memoire.atelier.video.model.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/atelier/video")
public class VideoController {

    private final VideoService videoService;
    private final CollectionService collectionService;
    private final MemoryService memoryService;
    private final PythonApiService pythonApiService;

    @Autowired
    public VideoController(VideoService videoService,
                           CollectionService collectionService,
                           MemoryService memoryService,
                           PythonApiService pythonApiService) {
        this.videoService = videoService;
        this.collectionService = collectionService;
        this.memoryService = memoryService;
        this.pythonApiService = pythonApiService;

    }

    @GetMapping("/collections/{userId}")
    public ResponseEntity<List<Collection>> getCollections(@PathVariable("userId") int userId) {
        List<Collection> collections = collectionService.getCollectionsByUserId(userId);
        return ResponseEntity.ok(collections);
    }

    @GetMapping("/collections/{collectionId}/memories")
    public ResponseEntity<List<Memory>> getMemoriesByCollection(
            @PathVariable("collectionId") int collectionId) {
        List<Memory> memories = memoryService.getMemoriesByCollectionId(collectionId);
        return ResponseEntity.ok(memories);
    }



    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = pythonApiService.uploadImage(file);
        return ResponseEntity.ok(imageUrl);
    }

    @PostMapping("/generate-tts")
    public ResponseEntity<String> generateTTS(@RequestBody TtsPreviewRequest requestDto) {
        String ttsUrl = pythonApiService.generateTTS(requestDto);
        return ResponseEntity.ok(ttsUrl);
    }

    @PostMapping("/preview-tts")
    public ResponseEntity<String> previewTTS(@RequestBody TtsPreviewRequest requestDto) {
        String previewUrl = pythonApiService.previewTTS(requestDto);
        return ResponseEntity.ok(previewUrl);
    }

    @PostMapping("/generate-video")
    public ResponseEntity<VideoResultDto> generateVideo(@RequestBody VideoGenerationRequest requestDto) {
        VideoResultDto result = pythonApiService.generateVideo(requestDto);
        return ResponseEntity.ok(result);
    }



//    @PostMapping("/save")
//    public ResponseEntity<Memory> createMemory(@RequestBody VideoResultDto videoResultDto) {
//        Memory saved = memoryService.saveNew(videoResultDto);
//        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
//    }
//
//    @PutMapping("/save/{memoryId}")
//    public ResponseEntity<Memory> updateMemory(
//            @PathVariable int memoryId,
//            @RequestBody VideoResultDto videoResultDto) throws ParseException {
//        memoryService.updateExisting(memoryId, videoResultDto);
//        return ResponseEntity.noContent().build();
//    }
}
