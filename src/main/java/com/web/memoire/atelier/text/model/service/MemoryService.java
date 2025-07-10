package com.web.memoire.atelier.text.model.service;


import com.web.memoire.common.entity.MemoryEntity;

import java.util.List;

public interface MemoryService {

    MemoryEntity saveMemory(MemoryEntity memory);

    List<MemoryEntity> findByCollectionid(String collectionid);

    List<MemoryEntity> findByUserId(String userId);

    MemoryEntity findById(Long memoryId);
}