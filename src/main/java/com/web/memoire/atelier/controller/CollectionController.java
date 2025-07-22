package com.web.memoire.atelier.controller;

import com.web.memoire.atelier.jpa.repository.CollectionRepository;
import com.web.memoire.common.entity.CollectionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {
    private final CollectionRepository collectionRepository;

    // 💡여기를 수정했습니다. (중복된 /collections 삭제)
    @GetMapping("/{userId}")
    public ResponseEntity<List<CollectionEntity>> getCollections(@PathVariable String userId) {
        List<CollectionEntity> collections = collectionRepository.findByAuthorid(userId);
        return ResponseEntity.ok(collections);
    }
}