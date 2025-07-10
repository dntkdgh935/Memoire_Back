package com.web.memoire.archive.controller;

import com.web.memoire.archive.model.service.ArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/archive")
@CrossOrigin
public class ArchiveController {

    @Autowired
    private ArchiveService archiveService;

    @GetMapping("/collections")
    public ResponseEntity<?> getArchiveMain(@RequestParam String userid) {
        log.info("ArchiveController.getArchiveMain...");
        try {
            return ResponseEntity.ok(archiveService.findAllUserCollections(userid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/collections 에러");
        }
    }

    @GetMapping("/numCollections")
    public ResponseEntity<?> getCollectionNum(@RequestParam String userid) {
        log.info("ArchiveController.getCollectionNum...");
        try {
            return ResponseEntity.ok(archiveService.countAllCollectionsByUserId(userid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/numCollections 에러");
        }
    }

    @GetMapping("/numMemory")
    public ResponseEntity<?> getMemoryNum(@RequestParam String userid) {
        log.info("ArchiveController.getMemoryNum...");
        try {
            return ResponseEntity.ok(archiveService.countAllMemoriesByUserId(userid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/numMemory 에러");
        }
    }

    @GetMapping("/numFollowing")
    public ResponseEntity<?> getUserFollowing(@RequestParam String userid) {
        log.info("ArchiveController.getUserFollowing...");
        try {
            return ResponseEntity.ok(archiveService.countUserFollowing(userid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/following 에러");
        }
    }

    @GetMapping("/numFollowers")
    public ResponseEntity<?> getUserFollowers(@RequestParam String userid) {
        log.info("ArchiveController.getUserFollowers...");
        try {
            return ResponseEntity.ok(archiveService.countUserFollower(userid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/followers 에러");
        }
    }

}
