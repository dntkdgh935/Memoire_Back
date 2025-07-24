package com.web.memoire.atelier.ImTIm.model.service;

import com.web.memoire.atelier.ImTIm.jpa.repository.ImTImMemoryRepository;
import com.web.memoire.atelier.ImTIm.model.dto.ImTImResultDto;
import com.web.memoire.common.dto.Memory;
import com.web.memoire.common.entity.MemoryEntity;
import jakarta.persistence.EntityNotFoundException;
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
public class ImTImMemoryService {
    private final ImTImMemoryRepository imtimmemoryRepository;


    public List<Memory> getMemoriesByCollectionId(int collectionId) {
        List<MemoryEntity> entities = imtimmemoryRepository.findByCollectionidOrderByCreatedDateDesc(collectionId);
        List<Memory> dtos = new ArrayList<>();
        for (MemoryEntity e : entities) {
            dtos.add(e.toDto());
        }
        return dtos;
    }

    @Transactional
    public void createMemory(int collectionId, ImTImResultDto dto) throws ParseException {
        int nextOrder = imtimmemoryRepository.findMaxMemoryOrderByCollectionid(collectionId) + 1;

        String fileName = dto.getImageUrl().substring(dto.getImageUrl().lastIndexOf('/') + 1);

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = fmt.format(now);
        Date dt = fmt.parse(dateStr);

        MemoryEntity m = MemoryEntity.builder()
                .memoryType("image")
                .collectionid(collectionId)
                .title(dto.getTitle())
                .content(null)
                .filename(fileName)
                .filepath("/upload_files/memory_img")
                .createdDate(dt)
                .memoryOrder(nextOrder)
                .build();

        imtimmemoryRepository.save(m);
    }

    @Transactional
    public void updateExisting(int memoryId, ImTImResultDto dto) throws ParseException {
        MemoryEntity m = imtimmemoryRepository.findById(memoryId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 메모리 ID: " + memoryId));
        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = fmt.format(now);
        Date dt = fmt.parse(dateStr);

        m.setFilename(dto.getFilename());
        m.setCreatedDate(dt);

        imtimmemoryRepository.save(m);
    }
}
