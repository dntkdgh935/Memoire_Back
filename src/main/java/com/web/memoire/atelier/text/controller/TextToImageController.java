package com.web.memoire.atelier.text.controller;

import com.web.memoire.atelier.text.exception.ImageGenerationException;
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
import java.util.Date;
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

    // ✅ 3) 이미지 생성 요청 (DALL·E 호출 포함)
    @PostMapping("/generate")
    public ResponseEntity<ImageResultDto> generateImage(@RequestBody ImagePromptRequest request) {
        System.out.println("DEBUG: prompt = " + request.getPrompt());
        System.out.println("DEBUG: style = " + request.getStyle());
        ImageResultDto result = textToImageService.generateImage(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/save")
    public ResponseEntity<String> saveNewMemory(@RequestBody ImageResultDto dto) {
        try {
            // 🧠 1. memory_order 계산
            Integer maxOrder = memoryRepository.findMaxMemoryOrderByCollectionId(dto.getCollectionId());
            int nextOrder = (maxOrder != null) ? maxOrder + 1 : 1;

            // 🧠 2. imageUrl에서 filename, filepath 분리
            String imageUrl = dto.getImageUrl();
            String filename = "generated_image.jpg";
            String filepath = "/images/";

            if (imageUrl != null && imageUrl.contains("/")) {
                int lastSlash = imageUrl.lastIndexOf("/");
                if (lastSlash >= 0 && lastSlash < imageUrl.length() - 1) {
                    filename = imageUrl.substring(lastSlash + 1);
                }
                filepath = imageUrl.substring(0, lastSlash);  // 또는 "/images"로 고정
            }

            // 🧠 3. 메모리 엔티티 저장
            MemoryEntity memory = MemoryEntity.builder()
                    .title(dto.getTitle() != null ? dto.getTitle() : "제목 없음")
                    .content(imageUrl)  // content는 실제로 이미지 URL 그대로
                    .collectionid(dto.getCollectionId())
                    .memoryType(dto.getMemoryType() != null ? dto.getMemoryType() : "image")
                    .memoryOrder(nextOrder)
                    .filename(filename)
                    .filepath(filepath)
                    .createdDate(new Date())
                    .build();

            memoryRepository.save(memory);
            return ResponseEntity.ok("이미지 메모리 저장 완료");

        } catch (Exception e) {
            e.printStackTrace();
            throw new ImageGenerationException("이미지 생성 중 오류 발생", e);
        }
    }

    // ✅ 5) 기존 메모리 덮어쓰기
    @PutMapping("/update/{memoryId}")
    public ResponseEntity<String> updateMemory(@PathVariable int memoryId, @RequestBody ImageResultDto dto) {
        return memoryRepository.findById(memoryId)
                .map(memory -> {
                    memory.setTitle(dto.getTitle());
                    memory.setContent(dto.getPrompt());
                    memory.setFilename(dto.getFilename());
                    memory.setFilepath(dto.getImageUrl());
                    memoryRepository.save(memory);
                    return ResponseEntity.ok("이미지 메모리 덮어쓰기 완료");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}