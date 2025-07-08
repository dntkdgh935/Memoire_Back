package com.web.memoire.atelier.text.model.service;

import com.web.memoire.atelier.text.jpa.entity.MemoryEntity;
import com.web.memoire.atelier.text.model.dto.Memory;

import java.util.List;

public interface MemoryService {

    void saveMemory(MemoryEntity entity);

    List<Memory> getMemoriesByUser(String userId);

    List<Memory> getMemoriesByCollection(String collectionId);
}