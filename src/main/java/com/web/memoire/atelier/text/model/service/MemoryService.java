package com.web.memoire.atelier.text.model.service;

import com.web.memoire.atelier.text.jpa.entity.MemoryEntity;

import java.util.List;

public interface MemoryService {

    MemoryEntity saveMemory(MemoryEntity memory);

    List<MemoryEntity> findByCollectionId(String collectionId);

    List<MemoryEntity> findByUserId(String userId);

    MemoryEntity findById(Long memoryId);
}