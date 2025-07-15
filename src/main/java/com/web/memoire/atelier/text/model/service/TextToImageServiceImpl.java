package com.web.memoire.atelier.text.model.service;

import com.web.memoire.atelier.text.exception.ImageGenerationException;
import com.web.memoire.atelier.text.jpa.repository.MemoryRepository;
import com.web.memoire.atelier.text.model.dto.ImagePromptRequest;
import com.web.memoire.atelier.text.model.dto.ImageResultDto;
import com.web.memoire.common.entity.MemoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class TextToImageServiceImpl implements TextToImageService {

    private final PythonApiService pythonApiService;
    private final MemoryRepository memoryRepository;

    @Override
    public ImageResultDto generateImage(ImagePromptRequest request) {
        try {
            // ✅ FastAPI 호출
            ImageResultDto resultDto = pythonApiService.callDalle(request);

            // ✅ 메모리 저장 여부 확인
            if (request.isSaveToMemory()) {
                MemoryEntity memory = MemoryEntity.builder()
                        .title(resultDto.getTitle())
                        .content(resultDto.getPrompt())
                        .collectionid(resultDto.getCollectionId())
                        .filename(resultDto.getFilename())
                        .filepath(resultDto.getFilepath())
                        .memoryType(resultDto.getMemoryType())
                        .memoryOrder(resultDto.getMemoryOrder())
                        .createdDate(java.sql.Timestamp.valueOf(LocalDateTime.now()))
                        .build();

                memoryRepository.save(memory);
            }

            return resultDto;

        } catch (Exception e) {
            throw new ImageGenerationException("이미지 생성 중 오류 발생", e);
        }
    }
}