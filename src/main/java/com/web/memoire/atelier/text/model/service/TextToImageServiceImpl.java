package com.web.memoire.atelier.text.model.service;

import com.web.memoire.atelier.text.exception.ImageGenerationException;
import com.web.memoire.common.entity.MemoryEntity;
import com.web.memoire.atelier.text.jpa.repository.MemoryRepository;
import com.web.memoire.atelier.text.model.dto.ImagePromptRequest;
import com.web.memoire.atelier.text.model.dto.ImageResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class TextToImageServiceImpl implements TextToImageService {

    private final PythonApiService pythonApiService;
    private final MemoryRepository memoryRepository;  // ✅ 이걸로 대체

    @Override
    public ImageResultDto generateImage(ImagePromptRequest request) {
        try {
            ImageResultDto resultDto = pythonApiService.callDalle(request);

            if (request.isSaveToMemory()) {
                MemoryEntity memory = MemoryEntity.builder()
                        .title(resultDto.getTitle())
                        .content(resultDto.getPrompt())
                        .collectionid(resultDto.getCollectionId())
                        .filename(resultDto.getFilename())
                        .filepath(resultDto.getFilepath())
                        .memoryType("image")
                        .createdDate(java.sql.Timestamp.valueOf(LocalDateTime.now()))
                        .memoryOrder(resultDto.getMemoryOrder())
                        .build();

                memoryRepository.save(memory);  // ✅ 여기서 직접 저장
            }

            return resultDto;

        } catch (Exception e) {
            throw new ImageGenerationException("이미지 생성 중 오류 발생", e);
        }
    }
}