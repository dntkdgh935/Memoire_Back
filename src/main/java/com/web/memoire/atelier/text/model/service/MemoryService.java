package com.web.memoire.atelier.text.model.service;


import com.web.memoire.common.entity.MemoryEntity;


import java.util.List;

public interface MemoryService {

    AtelierMemoryEntity saveMemory(AtelierMemoryEntity memory);

    List<AtelierMemoryEntity> findByCollectionId(String collectionId);

    List<AtelierMemoryEntity> findByUserId(String userId);

    AtelierMemoryEntity findById(Long memoryId);
}