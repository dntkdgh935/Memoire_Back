package com.web.memoire.atelier.text.controller;

import com.web.memoire.atelier.text.jpa.repository.MemoryRepository;
import com.web.memoire.common.entity.MemoryEntity;
import com.web.memoire.atelier.text.model.dto.TextGenerationRequest;
import com.web.memoire.atelier.text.model.dto.TextResultDto;
import com.web.memoire.atelier.text.model.service.TextToTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/atelier/text")
@RequiredArgsConstructor
public class TextToTextController {

    private final TextToTextService textToTextService;
    private final MemoryRepository memoryRepository;

    // ✅ 1) 특정 컬렉션의 메모리 목록 조회
    @GetMapping("/memories/{collectionId}")
    public ResponseEntity<List<MemoryEntity>> getMemories(@PathVariable String collectionId) {
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

    // ✅ 3) GPT 텍스트 생성
    @PostMapping("/generate")
    public ResponseEntity<TextResultDto> generateText(@RequestBody TextGenerationRequest request) {
        System.out.println("DEBUG: inputText = " + request.getInputText());
        System.out.println("DEBUG: content = " + request.getContent());
        TextResultDto result = textToTextService.generateText(request);
        return ResponseEntity.ok(result);
    }

    // ✅ 4) 새 메모리로 저장
    @PostMapping("/save")
    public ResponseEntity<String> saveNewMemory(@RequestBody TextResultDto dto) {
        // 메모리ID는 auto increment가 아니라면 수동으로 관리 필요
        int newId = memoryRepository.findMaxMemoryId() + 1;

        MemoryEntity memory = MemoryEntity.builder()
                .memoryid(newId)
                .title(dto.getTitle())
                .content(dto.getContent())
                .collectionid(dto.getCollectionId())
                .memoryType(dto.getMemoryType())
                .memoryOrder(dto.getMemoryOrder())
                .createdDate(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        memoryRepository.save(memory);
        return ResponseEntity.ok("메모리 저장 완료");
    }

    // ✅ 5) 기존 메모리 덮어쓰기
    @PutMapping("/update/{memoryId}")
    public ResponseEntity<String> updateMemory(@PathVariable int memoryId, @RequestBody TextResultDto dto) {
        return memoryRepository.findById(memoryId)
                .map(memory -> {
                    memory.setTitle(dto.getTitle());
                    memory.setContent(dto.getContent());
                    memoryRepository.save(memory);
                    return ResponseEntity.ok("메모리 덮어쓰기 완료");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}