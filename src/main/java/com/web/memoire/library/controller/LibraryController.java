package com.web.memoire.library.controller;


import com.web.memoire.library.model.service.LibraryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/library")
@CrossOrigin
public class LibraryController {

    @Autowired
    private LibraryService libraryService;


    @GetMapping("/top5tags")
    public ResponseEntity<?> getTopTags() {
        log.info("LibraryController.getAllTags...");
        try {
            return ResponseEntity.ok(libraryService.getTopTags());
        } catch (Exception e) {
            log.error("Error while fetching tags", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("태그 조회 실패");
        }
    }

    //임시로 user001의 정보 리턴
    @GetMapping("/discover/all")
    public ResponseEntity<?> getAllColls() {
        log.info("LibraryController.getAllColls...");
        try {
            return ResponseEntity.ok(libraryService.getAllPublicCollectionView("user001"));
        } catch (Exception e) {
            log.error("Error while fetching colls", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("전체 컬렉션 조회 실패");
        }
    }
}
