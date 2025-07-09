package com.web.memoire.atelier.text.model.service;

import com.web.memoire.atelier.text.exception.ImageGenerationException;
import com.web.memoire.atelier.text.jpa.entity.AtelierMemoryEntity;
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
                AtelierMemoryEntity memory = AtelierMemoryEntity.builder()
                        .title(resultDto.getTitle())
                        .content(resultDto.getPrompt())
                        .collectionId(resultDto.getCollectionId())
                        .filename(resultDto.getFilename())
                        .filepath(resultDto.getFilepath())
                        .memoryType("image")
                        .createdDate(LocalDateTime.now())
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