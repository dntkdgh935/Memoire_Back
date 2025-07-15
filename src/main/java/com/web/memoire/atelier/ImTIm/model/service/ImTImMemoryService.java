package com.web.memoire.atelier.ImTIm.model.service;

import com.web.memoire.atelier.ImTIm.jpa.repository.ImTImMemoryRepository;
import com.web.memoire.atelier.ImTIm.model.dto.ImTImResultDto;
import com.web.memoire.common.dto.Memory;
import com.web.memoire.common.entity.MemoryEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImTImMemoryService {
    private final ImTImMemoryRepository imtimmemoryRepository;


    public List<Memory> getMemoriesByCollectionId(String collectionId) {
        List<MemoryEntity> entities = imtimmemoryRepository.findByCollectionidOrderByCreatedDateDesc(collectionId);
        List<Memory> dtos = new ArrayList<>();
        for (MemoryEntity e : entities) {
            dtos.add(e.toDto());
        }
        return dtos;
    }

    @Transactional
    public void createMemory(String collectionId, ImTImResultDto dto) {
        int nextOrder = imtimmemoryRepository.findMaxMemoryOrderByCollectionid(collectionId) + 1;

        MemoryEntity m = MemoryEntity.builder()
                .memoryType("image")             // 구분값
                .collectionid(collectionId)      // FK
                .title(dto.getTitle())           // 사용자 지정 제목
                .content(dto.getContent())       // 설명/내용
                .filename(dto.getFilename())     // 파일명
                .filepath(dto.getFilepath())     // 파일 경로 또는 URL
                .createdDate(new Date())         // 생성 시각
                .memoryOrder(nextOrder)          // 순서
                .build();

        imtimmemoryRepository.save(m);
    }

    @Transactional
    public void updateExisting(int memoryId, ImTImResultDto dto) {
        MemoryEntity m = imtimmemoryRepository.findById(memoryId)
                .orElseThrow(() -> new EntityNotFoundException("Memory not found: " + memoryId));

        m.setTitle(dto.getTitle());
        m.setContent(dto.getContent());
        m.setFilename(dto.getFilename());
        m.setFilepath(dto.getFilepath());        // 업데이트 시각 (엔티티에 @UpdateTimestamp가 있으면 생략 가능)

        imtimmemoryRepository.save(m);
    }
}
