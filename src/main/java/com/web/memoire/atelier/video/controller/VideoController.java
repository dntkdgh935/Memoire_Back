//package com.web.memoire.atelier.video.controller;
//
//import com.web.memoire.common.dto.Memory;
//import com.web.memoire.common.dto.Collection;
//import com.web.memoire.atelier.video.model.dto.TtsPreviewRequest;
//import com.web.memoire.atelier.video.model.dto.VideoGenerationRequest;
//import com.web.memoire.atelier.video.model.dto.VideoResultDto;
//import com.web.memoire.atelier.video.model.service.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/atelier/video")
//public class VideoController {
//
//    private final VideoService videoService;
//    private final CollectionService collectionService;
//    private final MemoryService memoryService;
//    private final VideoPythonApiService videopythonApiService;
//
//    @Autowired
//    public VideoController(VideoService videoService,
//                           CollectionService collectionService,
//                           MemoryService memoryService,
//                           VideoPythonApiService videopythonApiService) {
//        this.videoService = videoService;
//        this.collectionService = collectionService;
//        this.memoryService = memoryService;
//        this.videopythonApiService = videopythonApiService;
//
//    }
//
//    @GetMapping("/collections/{userId}")
//    public ResponseEntity<List<Collection>> getCollections(@PathVariable("userId") int userId) {
//        List<Collection> collections = collectionService.getCollectionsByUserId(userId);
//        return ResponseEntity.ok(collections);
//    }
//
//    @GetMapping("/collections/{collectionId}/memories")
//    public ResponseEntity<List<Memory>> getMemoriesByCollection(
//            @PathVariable("collectionId") String collectionId) {
//        List<Memory> memories = memoryService.getMemoriesByCollectionId(collectionId);
//        return ResponseEntity.ok(memories);
//    }
//
//
//
//    @PostMapping("/upload-image")
//    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
//        String imageUrl = videopythonApiService.uploadImage(file);
//        return ResponseEntity.ok(imageUrl);
//    }
//
//    @PostMapping("/generate-tts")
//    public ResponseEntity<String> generateTTS(@RequestBody TtsPreviewRequest requestDto) {
//        String ttsUrl = videopythonApiService.generateTTS(requestDto);
//        return ResponseEntity.ok(ttsUrl);
//    }
//
//    @PostMapping("/preview-tts")
//    public ResponseEntity<String> previewTTS(@RequestBody TtsPreviewRequest requestDto) {
//        String previewUrl = videopythonApiService.previewTTS(requestDto);
//        return ResponseEntity.ok(previewUrl);
//    }
//
//    @PostMapping("/generate-video")
//    public ResponseEntity<VideoResultDto> generateVideo(@RequestBody VideoGenerationRequest requestDto) {
//        VideoResultDto result = videopythonApiService.generateVideo(requestDto);
//        return ResponseEntity.ok(result);
//    }
//
//
//
////    @PostMapping("/save")
////    public ResponseEntity<Memory> createMemory(@RequestBody VideoResultDto videoResultDto) {
////        Memory saved = memoryService.saveNew(videoResultDto);
////        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
////    }
////
////    @PutMapping("/save/{memoryId}")
////    public ResponseEntity<Memory> updateMemory(
////            @PathVariable int memoryId,
////            @RequestBody VideoResultDto videoResultDto) throws ParseException {
////        memoryService.updateExisting(memoryId, videoResultDto);
////        return ResponseEntity.noContent().build();
////    }
//}
