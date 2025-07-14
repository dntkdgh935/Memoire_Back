package com.web.memoire.atelier.video.model.service;

import com.web.memoire.common.dto.Collection;
import com.web.memoire.common.dto.Memory;
import com.web.memoire.common.entity.MemoryEntity;
import com.web.memoire.common.entity.CollectionEntity;
import com.web.memoire.atelier.video.jpa.repository.MemoryRepository;
import com.web.memoire.atelier.video.model.dto.VideoResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private final MemoryRepository memoryRepository;

    @Transactional
    public void insertMemory(VideoResultDto result) {
        MemoryEntity entity = result.toEntity();
        memoryRepository.save(entity);
    }

    public List<Memory> getMemoriesByCollectionId(int collectionId) {
        List<MemoryEntity> entities = memoryRepository.findByCollectionidOrderByCreatedDateDesc(collectionId);
        List<Memory> dtos = new ArrayList<>();
        for (MemoryEntity e : entities) {
            dtos.add(e.toDto());
        }
        return dtos;
    }

//    @Transactional
//    public Memory createMemory(VideoResultDto result) {
//        int collectionId = result.getCollectionId();
//        // 현재 컬렉션 내 메모리 개수 + 1 → 순서 지정
//        int nextOrder = memoryRepository.findByCollectionId(collectionId).size() + 1;
//
//        // 파일명 추출 (URL에서 마지막 경로)
//        String videoUrl = result.getVideoUrl();
//        String fileName = videoUrl.substring(videoUrl.lastIndexOf('/') + 1);
//
//        // 생성일시(Date) 파싱
//        Date createdDate = new Date();
//        if (result.getCreatedAt() != null) {
//            try {
//                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
//                createdDate = fmt.parse(result.getCreatedAt());
//            } catch (ParseException e) {
//                // 포맷 오류 시 현재 시각 사용
//                createdDate = new Date();
//            }
//        }
//
//        MemoryEntity entity = MemoryEntity.builder()
//                .memoryType("video")
//                .collectionId(collectionId)
//                .title(result.getTitle())
//                .content(null)      // content 없음
//                .filename(fileName)
//                .filepath(videoUrl)
//                .userId(result.getUserId())
//                .memoryOrder(nextOrder)
//                .createdDate(createdDate.toInstant()
//                        .atZone(ZoneId.systemDefault())
//                        .toLocalDateTime())
//                .build();
//        MemoryEntity saved = memoryRepository.save(entity);
//        return toCommonDto(saved);
//    }
//
//    @Transactional
//    public void updateExisting(int memoryId, VideoResultDto result) throws ParseException {
//        MemoryEntity entity = memoryRepository.findById(memoryId)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메모리 ID: " + memoryId));
//
//        entity.setMemoryType("video");
//        entity.setFilename(result.getFileName());
//        entity.setFilepath(result.getVideoUrl());
//        entity.setTitle(result.getTitle());
//
//        if (result.getCreatedAt() != null) {
//            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
//            Date dt = formatter.parse(result.getCreatedAt());
//            entity.setCreatedDate(dt);
//        }
//
//        memoryRepository.save(entity);
//    }
}
