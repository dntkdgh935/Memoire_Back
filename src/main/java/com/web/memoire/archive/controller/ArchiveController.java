package com.web.memoire.archive.controller;

import com.web.memoire.archive.model.service.ArchiveService;
import com.web.memoire.common.dto.Bookmark;
import com.web.memoire.common.dto.CollView;
import com.web.memoire.common.dto.Relationship;
import com.web.memoire.user.model.dto.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/archive")
@CrossOrigin
public class ArchiveController {

    @Autowired
    private ArchiveService archiveService;

    @GetMapping("/userinfo")
    public ResponseEntity<?> getUserInfo(@RequestParam String userid) {
        log.info("ArchiveController.getUserInfo...");
        try {
            return ResponseEntity.ok(archiveService.findUserById(userid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/userinfo 에러");
        }
    }

    @GetMapping("/follower")
    public ResponseEntity<?> getUserFollower(@RequestParam String userid) {
        log.info("ArchiveController.getUserFollower...");
        try {
            ArrayList<User> userList = new ArrayList<>();
            for (Relationship rel : archiveService.findAllUserFollower(userid)) {
                User user = archiveService.findUserById(rel.getUserid());
                // 상대방 개인정보 처리
                user.setUserId(null);
                user.setName(null);
                user.setBirthday(null);
                user.setPhone(null);
                user.setPassword(null);
                user.setRole(null);
                user.setAutoLoginFlag(null);
                user.setAutoLoginToken(null);
                user.setRegistrationDate(null);
                user.setSanctionCount(null);
                user.setStatusMessage(null);
                user.setFaceLoginUse(null);
                userList.add(user);
            }
            return ResponseEntity.ok(userList);
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/follower 에러");
        }
    }

    @GetMapping("/following")
    public ResponseEntity<?> getUserFollowing(@RequestParam String userid) {
        log.info("ArchiveController.getUserFollowing...");
        try {
            ArrayList<User> userList = new ArrayList<>();
            for (Relationship rel : archiveService.findAllUserFollowing(userid)) {
                User user = archiveService.findUserById(rel.getTargetid());
                // 상대방 개인정보 처리
                user.setUserId(null);
                user.setName(null);
                user.setBirthday(null);
                user.setPhone(null);
                user.setPassword(null);
                user.setRole(null);
                user.setAutoLoginFlag(null);
                user.setAutoLoginToken(null);
                user.setRegistrationDate(null);
                user.setSanctionCount(null);
                user.setStatusMessage(null);
                user.setFaceLoginUse(null);
                userList.add(user);
            }
            return ResponseEntity.ok(userList);
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/following 에러");
        }
    }

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

    @GetMapping("/bookmarkCollections")
    public ResponseEntity<?> getBookmarkCollections(@RequestParam String userid) {
        log.info("ArchiveController.getBookmarkCollections...");

        try {
            ArrayList<Bookmark> b = archiveService.findAllUserBookmarks(userid);
            ArrayList<CollView> c = new ArrayList<>();
            // check bookmark validity
            for (Bookmark bm : b) {
                CollView coll = archiveService.findCollViewByCollectionId(userid, bm.getCollectionid());
                // owner
                if (userid.equals(coll.getAuthorid())) {
                    c.add(coll);
                }
                // public
                else if (coll.getVisibility() == 1) {
                    // ok
                    c.add(coll);
                }
                // follower
                else if (coll.getVisibility() == 2) {
                    if (archiveService.findRelationshipById(userid, coll.getAuthorid()) == null) {
                        // do not add
                    }
                    else if (archiveService.findRelationshipById(userid, coll.getAuthorid()).getStatus().equals("1") && !archiveService.findRelationshipByUserIdAndTargetId(userid, coll.getAuthorid()).getStatus().equals("2")) {
                        // ok
                        c.add(coll);
                    }
                    else {
                        // do not add
                    }
                }
                // private
                else if (coll.getVisibility() == 3) {
                    // do not add
                }
            }
            return ResponseEntity.ok(c);
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/bookmarkCollections 에러");
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
    public ResponseEntity<?> getUserFollowingNum(@RequestParam String userid) {
        log.info("ArchiveController.getUserFollowingNum...");
        try {
            return ResponseEntity.ok(archiveService.countUserFollowing(userid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/numFollowing 에러");
        }
    }

    @GetMapping("/numFollowers")
    public ResponseEntity<?> getUserFollowersNum(@RequestParam String userid) {
        log.info("ArchiveController.getUserFollowersNum...");
        try {
            return ResponseEntity.ok(archiveService.countUserFollower(userid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/numFollowers 에러");
        }
    }

    @PostMapping("/updateStatusMessage")
    public ResponseEntity<?> updateStatusMessage(@RequestParam String userid, @RequestParam String statusMessage) {
        log.info("ArchiveController.updateStatusMessage...");
        try {
            int result = archiveService.updateStatusMessage(userid, statusMessage);
            if (result == 1) {
                return ResponseEntity.ok("상태메시지 수정 성공!");
            }
            else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("/updateStatusMessage 에러");
            }
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/updateStatusMessage 에러");
        }
    }

}
