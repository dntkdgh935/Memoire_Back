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

    public List<Memory> getMemoriesByCollectionId(int collectionId) {
        List<MemoryEntity> entities = memoryRepository.findByCollectionidOrderByCreatedDateDesc(collectionId);
        List<Memory> dtos = new ArrayList<>();
        for (MemoryEntity e : entities) {
            dtos.add(e.toDto());
        }
        return dtos;
    }

    @Transactional
    public void createMemory(int CollectionId, VideoResultDto result) throws ParseException {
        int nextOrder = memoryRepository.findMaxMemoryOrderByCollectionid(CollectionId) + 1;

        // 파일명 추출 (URL에서 마지막 경로)
        String videoUrl = result.getVideoUrl();
        String fileName = videoUrl.substring(videoUrl.lastIndexOf('/') + 1);

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = fmt.format(now);
        Date dt = fmt.parse(dateStr);


        MemoryEntity entity = MemoryEntity.builder()
                .memoryType("video")
                .collectionid(CollectionId)
                .title(result.getTitle())
                .content(null)
                .filename(fileName)
                .filepath("/upload_files/memory_video")
                .createdDate(dt)
                .memoryOrder(nextOrder)
                .build();

        memoryRepository.save(entity);
    }

    @Transactional
    public void updateExisting(String memoryId, VideoResultDto result) throws ParseException {
        MemoryEntity entity = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메모리 ID: " + memoryId));

        String videoUrl = result.getVideoUrl();
        String fileName = videoUrl.substring(videoUrl.lastIndexOf('/') + 1);

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = fmt.format(now);
        Date dt = fmt.parse(dateStr);

        entity.setMemoryType("video");
        entity.setFilename(fileName);
        entity.setFilepath("/upload_files/memory_video");
        entity.setCreatedDate(dt);


        memoryRepository.save(entity);
    }
}
