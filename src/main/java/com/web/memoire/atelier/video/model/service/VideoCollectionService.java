package com.web.memoire.atelier.video.model.service;

import com.web.memoire.atelier.video.jpa.repository.VideoCollectionRepository;
import com.web.memoire.common.dto.Collection;
import com.web.memoire.common.entity.CollectionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoCollectionService {

    private final VideoCollectionRepository collectionRepository;

    public List<Collection> getCollectionsByUserId(String userId) {
        List<CollectionEntity> entities = collectionRepository.findByAuthorid(userId);
        List<Collection> dtos = new ArrayList<>();
        for (CollectionEntity e : entities) {
            dtos.add(e.toDto());
        }
        return dtos;
    }
}
