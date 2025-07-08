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


    @GetMapping("/tags")
    public ResponseEntity<?> getAllTags() {
        log.info("LibraryController.getAllTags...");
        try {
            return ResponseEntity.ok(libraryService.getAllTags());
        } catch (Exception e) {
            log.error("Error while fetching tags", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("태그 조회 실패");
        }
    }

}
