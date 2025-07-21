package com.web.memoire.atelier.text.controller;

import com.web.memoire.atelier.text.jpa.repository.MemoryRepository;
import com.web.memoire.atelier.text.model.dto.ImagePromptRequest;
import com.web.memoire.atelier.text.model.dto.ImageResultDto;
import com.web.memoire.atelier.text.model.dto.ImageSaveRequest;
import com.web.memoire.atelier.text.model.service.TextToImageService;
import com.web.memoire.common.entity.MemoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/atelier/image")
@RequiredArgsConstructor
public class TextToImageController {

    private final TextToImageService textToImageService;
    private final MemoryRepository memoryRepository;

    // 2) 단일 메모리 조회
    @GetMapping("/memory/{memoryId}")
    public ResponseEntity<MemoryEntity> getMemoryById(@PathVariable int memoryId) {
        return memoryRepository.findById(memoryId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3) 이미지 생성 요청 (DALL·E 호출 포함)
    @PostMapping("/generate")
    public ResponseEntity<ImageResultDto> generateImage(@RequestBody ImagePromptRequest request) {
        ImageResultDto result = textToImageService.generateImage(request);
        return ResponseEntity.ok(result);
    }

    // 4) 새 메모리로 저장
    @PostMapping("/save")
    public ResponseEntity<ImageResultDto> saveNewMemory(@RequestBody ImageSaveRequest req) {
        ImageResultDto saved = textToImageService.saveNewImageMemory(req);
        return ResponseEntity.ok(saved);
    }

    // 5) 기존 메모리 덮어쓰기
    @PutMapping("/update/{memoryId}")
    public ResponseEntity<ImageResultDto> updateMemory(
            @PathVariable int memoryId,
            @RequestBody ImageSaveRequest req
    ) {
        ImageResultDto updated = textToImageService.overwriteImageMemory(memoryId, req);
        return ResponseEntity.ok(updated);
    }
}