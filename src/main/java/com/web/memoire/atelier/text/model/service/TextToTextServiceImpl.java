package com.web.memoire.atelier.text.model.service;

import com.web.memoire.atelier.text.exception.TextGenerationException;
import com.web.memoire.atelier.text.jpa.entity.MemoryEntity;
import com.web.memoire.atelier.text.jpa.repository.MemoryRepository;
import com.web.memoire.atelier.text.model.dto.TextGenerationRequest;
import com.web.memoire.atelier.text.model.dto.TextResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TextToTextServiceImpl implements TextToTextService {

    private final PythonApiService pythonApiService;
    private final MemoryRepository memoryRepository;  // ✅ MemoryService 대신 MemoryRepository 직접 주입

    @Override
    public TextResultDto generateText(TextGenerationRequest request) {
        try {
            // 1. Python 서버 호출
            TextResultDto resultDto = pythonApiService.callGpt(request);

            // 2. 저장 옵션 true일 때 MemoryEntity로 변환 후 저장
            if (request.isSaveToMemory()) {
                MemoryEntity memory = MemoryEntity.builder()
                        .title(resultDto.getTitle())
                        .content(resultDto.getContent())
                        .collectionId(resultDto.getCollectionId())
                        .memoryType("text")
                        .createdDate(LocalDateTime.now())
                        .memoryOrder(resultDto.getMemoryOrder())
                        .build();

                memoryRepository.save(memory);  // ✅ 직접 저장
            }

            return resultDto;

        } catch (Exception e) {
            throw new TextGenerationException("텍스트 생성 중 오류 발생", e);
        }
    }
}