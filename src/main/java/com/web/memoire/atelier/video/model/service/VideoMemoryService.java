package com.web.memoire.atelier.video.model.service;

import com.web.memoire.common.dto.Memory;
import com.web.memoire.common.entity.MemoryEntity;
import com.web.memoire.atelier.video.jpa.repository.VideoMemoryRepository;
import com.web.memoire.atelier.video.model.dto.VideoResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoMemoryService {

    private final VideoMemoryRepository memoryRepository;

    public List<Memory> getMemoriesByCollectionId(String collectionId) {
        List<MemoryEntity> entities = memoryRepository.findByCollectionidOrderByCreatedDateDesc(collectionId);
        List<Memory> dtos = new ArrayList<>();
        for (MemoryEntity e : entities) {
            dtos.add(e.toDto());
        }
        return dtos;
    }

    @Transactional
    public void createMemory(String CollectionId, VideoResultDto result) throws ParseException {
        int nextOrder = memoryRepository.findByCollectionid(CollectionId).size() + 1;

        // 파일명 추출 (URL에서 마지막 경로)
        String videoUrl = result.getVideoUrl();
        String fileName = videoUrl.substring(videoUrl.lastIndexOf('/') + 1);

        Date dt = new Date();

        if (result.getCreatedAt() != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            dt = formatter.parse(result.getCreatedAt());
        }

        MemoryEntity entity = MemoryEntity.builder()
                .memoryid(123)   // 여기 수정해야 함
                .memoryType("video")
                .collectionid(String.valueOf(CollectionId))
                .title(result.getTitle())
                .content(null)
                .filename(fileName)
                .filepath(videoUrl)
                .createdDate(dt)
                .memoryOrder(nextOrder)
                .build();
        memoryRepository.save(entity);
    }

    @Transactional
    public void updateExisting(String memoryId, VideoResultDto result) throws ParseException {
        MemoryEntity entity = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메모리 ID: " + memoryId));

        entity.setMemoryType("video");
        entity.setFilename(result.getFileName());
        entity.setFilepath(result.getVideoUrl());
        entity.setTitle(result.getTitle());

        if (result.getCreatedAt() != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date dt = formatter.parse(result.getCreatedAt());
            entity.setCreatedDate(dt);
        }

        memoryRepository.save(entity);
    }
}
