package com.web.memoire.atelier.ImTIm.model.service;

import com.web.memoire.atelier.ImTIm.jpa.repository.ImTImCollectionRepository;
import com.web.memoire.atelier.video.jpa.repository.VideoCollectionRepository;
import com.web.memoire.common.dto.Collection;
import com.web.memoire.common.dto.Memory;
import com.web.memoire.common.entity.CollectionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImTImCollectionService {
    private final ImTImCollectionRepository imTImCollectionRepository;

    public List<Collection> getCollectionsByUserId(String userId) {
        List<CollectionEntity> entities = imTImCollectionRepository.findByAuthorid(userId);
        List<Collection> dtos = new ArrayList<>();
        for (CollectionEntity e : entities) {
            dtos.add(e.toDto());
        }
        return dtos;
    }
}