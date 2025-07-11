package com.web.memoire.atelier.text.controller;

import com.web.memoire.atelier.text.jpa.repository.MemoryRepository;
import com.web.memoire.common.entity.MemoryEntity;
import com.web.memoire.atelier.text.model.dto.TextGenerationRequest;
import com.web.memoire.atelier.text.model.dto.TextResultDto;
import com.web.memoire.atelier.text.model.service.TextToTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/atelier/text")
@RequiredArgsConstructor
public class TextToTextController {

    private final TextToTextService textToTextService;
    private final MemoryRepository memoryRepository;  // ✅ 직접 주입

    // ✅ 1) 특정 컬렉션의 메모리 목록 조회
    @GetMapping("/memories/{collectionId}")
    public ResponseEntity<List<MemoryEntity>> getMemories(@PathVariable String collectionId) {
        System.out.println("🔍 전달받은 collectionId = " + collectionId);
        List<MemoryEntity> memories = memoryRepository.findByCollectionid(collectionId);
        System.out.println("📦 가져온 메모리 수 = " + memories.size());
        return ResponseEntity.ok(memories);
    }

    // ✅ 2) 단일 메모리 조회 (id 기반)
    @GetMapping("/memory/{memoryId}")
    public ResponseEntity<MemoryEntity> getMemoryById(@PathVariable Integer memoryId) {
        return memoryRepository.findById(memoryId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ 3) GPT 텍스트 생성 요청
    @PostMapping("/generate")
    public ResponseEntity<TextResultDto> generateText(@RequestBody TextGenerationRequest request) {
        TextResultDto result = textToTextService.generateText(request);
        return ResponseEntity.ok(result);
    }
}