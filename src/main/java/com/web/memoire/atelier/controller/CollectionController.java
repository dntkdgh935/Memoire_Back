package com.web.memoire.atelier.controller;

import com.web.memoire.common.entity.CollectionEntity;
import com.web.memoire.atelier.jpa.repository.CollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionRepository collectionRepository;

    @GetMapping
    public ResponseEntity<List<CollectionEntity>> getAllCollections() {
        List<CollectionEntity> collections = collectionRepository.findAll();
        return ResponseEntity.ok(collections);
    }
}