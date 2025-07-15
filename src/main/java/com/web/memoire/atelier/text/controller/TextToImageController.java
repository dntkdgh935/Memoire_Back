package com.web.memoire.atelier.text.controller;

import com.web.memoire.atelier.text.jpa.repository.MemoryRepository;
import com.web.memoire.atelier.text.model.dto.ImagePromptRequest;
import com.web.memoire.atelier.text.model.dto.ImageResultDto;
import com.web.memoire.atelier.text.model.service.TextToImageService;
import com.web.memoire.common.entity.MemoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/atelier/image")
@RequiredArgsConstructor
public class TextToImageController {

    private final TextToImageService textToImageService;
    private final MemoryRepository memoryRepository;

    // ✅ 1) 특정 컬렉션의 메모리 목록 조회
    @GetMapping("/memories/{collectionId}")
    public ResponseEntity<List<MemoryEntity>> getMemories(@PathVariable int collectionId) {
        System.out.println("🔍 전달받은 collectionId = " + collectionId);
        List<MemoryEntity> memories = memoryRepository.findByCollectionid(collectionId);
        System.out.println("📦 가져온 메모리 수 = " + memories.size());
        return ResponseEntity.ok(memories);
    }

    // ✅ 2) 단일 메모리 조회
    @GetMapping("/memory/{memoryId}")
    public ResponseEntity<MemoryEntity> getMemoryById(@PathVariable Integer memoryId) {
        return memoryRepository.findById(memoryId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ 3) 이미지 생성 요청
    @PostMapping("/generate")
    public ResponseEntity<ImageResultDto> generateImage(@RequestBody ImagePromptRequest request) {
        System.out.println("DEBUG: prompt = " + request.getPrompt());
        System.out.println("DEBUG: style = " + request.getStyle());
        ImageResultDto result = textToImageService.generateImage(request);
        return ResponseEntity.ok(result);
    }

    // ✅ 4) 새 메모리로 저장
    @PostMapping("/save")
    public ResponseEntity<String> saveNewMemory(@RequestBody ImageResultDto dto) {
        int newId = memoryRepository.findMaxMemoryId() + 1;

        MemoryEntity memory = MemoryEntity.builder()
                .memoryid(newId)
                .title(dto.getTitle())
                .content(dto.getPrompt())
                .collectionid(dto.getCollectionId())
                .memoryType(dto.getMemoryType())
                .memoryOrder(dto.getMemoryOrder())
                .filename(dto.getFilename())
                .filepath(dto.getFilepath())
                .createdDate(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        memoryRepository.save(memory);
        return ResponseEntity.ok("이미지 메모리 저장 완료");
    }

    // ✅ 5) 기존 메모리 덮어쓰기
    @PutMapping("/update/{memoryId}")
    public ResponseEntity<String> updateMemory(@PathVariable int memoryId, @RequestBody ImageResultDto dto) {
        return memoryRepository.findById(memoryId)
                .map(memory -> {
                    memory.setTitle(dto.getTitle());
                    memory.setContent(dto.getPrompt());
                    memory.setFilename(dto.getFilename());
                    memory.setFilepath(dto.getFilepath());
                    memoryRepository.save(memory);
                    return ResponseEntity.ok("이미지 메모리 덮어쓰기 완료");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}