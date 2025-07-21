package com.web.memoire.atelier.ImTIm.controller;


import com.web.memoire.atelier.ImTIm.model.dto.ImTImGenerationRequest;
import com.web.memoire.atelier.ImTIm.model.dto.ImTImResultDto;
import com.web.memoire.atelier.ImTIm.model.service.ImTImCollectionService;
import com.web.memoire.atelier.ImTIm.model.service.ImTImMemoryService;
import com.web.memoire.atelier.ImTIm.model.service.ImTImPythonApiService;
import com.web.memoire.common.dto.Collection;
import com.web.memoire.common.dto.Memory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/atelier/imtim")
@RequiredArgsConstructor
public class ImTImController {

    private final ImTImMemoryService imtimMemoryService;
    private final ImTImCollectionService imtimCollectionService;
    private final ImTImPythonApiService imtimPythonApiService;

    @GetMapping("/collections/{userId}")
    public ResponseEntity<List<Collection>> getCollections(@PathVariable("userId") String userId) {
        List<Collection> collections = imtimCollectionService.getCollectionsByUserId(userId);
        return ResponseEntity.ok(collections);
    }

    @GetMapping("/collections/{collectionId}/memories")
    public ResponseEntity<List<Memory>> getMemoriesByCollection(
            @PathVariable("collectionId") int collectionId) {
        List<Memory> memories = imtimMemoryService.getMemoriesByCollectionId(collectionId);
        return ResponseEntity.ok(memories);
    }

    @PostMapping("/generate")
    public ResponseEntity<ImTImResultDto> generateImage(
            @RequestBody ImTImGenerationRequest request
    ) {
        log.info("Generate image request: {}", request);
        ImTImResultDto result = imtimPythonApiService.generateImage(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{collectionId}")
    public ResponseEntity<Memory> createMemory(
            @PathVariable int collectionId,
            @RequestBody ImTImResultDto imtimResultDto
    ) throws ParseException {

        imtimMemoryService.createMemory(
                collectionId,
                imtimResultDto
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/save/{memoryId}")
    public ResponseEntity<Memory> updateMemory(
            @PathVariable int memoryId,
            @RequestBody ImTImResultDto imtimResultDto) throws ParseException {
        imtimMemoryService.updateExisting(memoryId, imtimResultDto);
        return ResponseEntity.noContent().build();
    }



}
