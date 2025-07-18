package com.web.memoire.archive.controller;

import com.web.memoire.archive.model.service.ArchiveService;
import com.web.memoire.common.dto.*;
import com.web.memoire.user.model.dto.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/archive")
@CrossOrigin
public class ArchiveController {

    @Autowired
    private ArchiveService archiveService;



    @Value("C:/upload_files")
    private String uploadDir;

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
                    } else if (archiveService.findRelationshipById(userid, coll.getAuthorid()).getStatus().equals("1") && !archiveService.findRelationshipByUserIdAndTargetId(userid, coll.getAuthorid()).getStatus().equals("2")) {
                        // ok
                        c.add(coll);
                    } else {
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
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("/updateStatusMessage 에러");
            }
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/updateStatusMessage 에러");
        }
    }


//    오류 가능성: transactional처리가 따로따로 됨 (insertCollection -> insertMemory)
    @PostMapping(value = "/newColl", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> insertNewCollection(@ModelAttribute Collection collection, @ModelAttribute Memory memory, @RequestParam(name = "file", required = false) MultipartFile file) {
        log.info("ArchiveController.insertNewCollection...");
        log.info("collection : " + collection); // collection : Collection(collectionid=null, authorid=blabla, collectionTitle=blabla, readCount=0, visibility=1, createdDate=null, titleEmbedding=null, color=#000000)
        log.info("memory: " + memory); // memory: Memory(memoryid=0, memoryType=text, collectionid=0, title=123123123123123, content=1231231135161, filename=null, filepath=null, createdDate=null, memoryOrder=0)
        collection.setCreatedDate(new Date());
        //=============================
        collection.setTitleEmbedding(archiveService.getEmbeddedTitle(collection.getCollectionTitle()));
        //=============================
        int collectionid = archiveService.insertCollection(collection);
        if (collectionid > 0) {
            memory.setCollectionid(collectionid);
            memory.setCreatedDate(new Date());
            memory.setMemoryOrder(1);
            if (memory.getMemoryType().equals("text")) {
                memory.setFilename(null);
                memory.setFilepath(null);
                if (archiveService.insertMemory(memory) > 0) {
                    return ResponseEntity.ok("저장 성공");
                } else {
                    // 텍스트 메모리 저장 실패
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newColl 에러");
                }
            } else if (memory.getMemoryType().equals("image") || memory.getMemoryType().equals("video")) {
                if (file != null && !file.isEmpty()) {
                    String savePath = uploadDir;
                    String uuid = UUID.randomUUID().toString();
                    String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
                    memory.setFilename(uuid + "." + ext);
                    if (memory.getMemoryType().equals("image")) {
                        savePath += "/memory_img";
                    } else if (memory.getMemoryType().equals("video")) {
                        savePath += "/memory_video";
                    }
                    memory.setFilepath(savePath.substring(2) + "/" + memory.getFilename());
                    memory.setContent(null);
                    if (archiveService.insertMemory(memory) > 0) {
                        try {
                            file.transferTo(new File(savePath, memory.getFilename()));
                            return ResponseEntity.ok("저장 성공");
                        } catch (Exception e) {
                            // 파일 저장 실패
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newColl 에러");
                        }
                    } else {
                        // 미디어 메모리 저장 실패
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newColl 에러");
                    }
                } else {
                    // 미디어 타입인데 파일 없음
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newColl 에러");
                }
            } else {
                // 파일 타입이 조건에 부합하지 않음
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newColl 에러");
            }

        } else {
            // 컬렉션 저장 실패
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newColl 에러");
        }
    }

    @PostMapping(value = "/newMemory", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> insertNewMemory(@ModelAttribute Memory memory, @RequestParam String authorid, @RequestParam(name = "file", required = false) MultipartFile file) {
        log.info("ArchiveController.insertNewMemory...");
        log.info("memory: " + memory); // memory: Memory(memoryid=0, memoryType=text, collectionid=0, title=123123123123123, content=1231231135161, filename=null, filepath=null, createdDate=null, memoryOrder=0)

        memory.setCreatedDate(new Date());
        try {
            memory.setMemoryOrder(archiveService.findAllUserMemories(authorid, memory.getCollectionid()).size() + 1);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newMemory 에러");
        }
        if (memory.getMemoryType().equals("text")) {
            memory.setFilename(null);
            memory.setFilepath(null);
            if (archiveService.insertMemory(memory) > 0) {
                return ResponseEntity.ok("저장 성공");
            } else {
                // 텍스트 메모리 저장 실패
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newMemory 에러");
            }
        } else if (memory.getMemoryType().equals("image") || memory.getMemoryType().equals("video")) {
            if (file != null && !file.isEmpty()) {
                String savePath = uploadDir;
                String uuid = UUID.randomUUID().toString();
                String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
                memory.setFilename(uuid + "." + ext);
                if (memory.getMemoryType().equals("image")) {
                    savePath += "/memory_img";
                } else if (memory.getMemoryType().equals("video")) {
                    savePath += "/memory_video";
                }
                memory.setFilepath(savePath.substring(2) + "/" + memory.getFilename());
                memory.setContent(null);
                if (archiveService.insertMemory(memory) > 0) {
                    try {
                        file.transferTo(new File(savePath, memory.getFilename()));
                        return ResponseEntity.ok("저장 성공");
                    } catch (Exception e) {
                        // 파일 저장 실패
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newMemory 에러");
                    }
                } else {
                    // 미디어 메모리 저장 실패
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newMemory 에러");
                }
            } else {
                // 미디어 타입인데 파일 없음
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newMemory 에러");
            }
        } else {
            // 파일 타입이 조건에 부합하지 않음
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newMemory 에러");
        }
    }

    @PostMapping("/editColl")
    public ResponseEntity<?> editCollection(@ModelAttribute Collection collection) {
        log.info("ArchiveController.editCollection...");
        log.info("collection : " + collection); // collection : Collection(collectionid=null, authorid=blabla, collectionTitle=blabla, readCount=0, visibility=1, createdDate=null, titleEmbedding=null, color=#000000)
//        TODO: 여긴 추가하거나 말거나 (컬렉션을 편집하면 임베딩을 새로고침)
//        TODO: collection.setTitleEmbedding(blabla);
        collection.setTitleEmbedding(null);
        // 기존 컬렉션을 가져오는 로직 (예: DB에서 불러옴)
        Collection existingCollection = archiveService.getCollectionById(collection.getCollectionid());
        if (existingCollection != null && !existingCollection.getCollectionTitle().equals(collection.getCollectionTitle())) {
            collection.setTitleEmbedding(archiveService.getEmbeddedTitle(collection.getCollectionTitle()));
        }

//        컬렉션 수정하면 날짜 초기화
        collection.setCreatedDate(new Date());
        if (archiveService.insertCollection(collection) > 0) {
            return ResponseEntity.ok("저장 성공");
        } else {
            // 컬렉션 저장 실패
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/editColl 에러");
        }
    }

    @DeleteMapping("/collection/{collectionid}")
    public ResponseEntity<?> collectionDelete(@PathVariable int collectionid, @RequestParam String userid) {
        try {
            ArrayList<Memory> list = archiveService.findAllUserMemories(userid, collectionid);
            for (Memory memory : list) {
                if (memory.getMemoryType().equals("image")) {
                    new File(uploadDir + "\\memory_img\\" + memory.getFilename()).delete();
                } else if (memory.getMemoryType().equals("video")) {
                    new File(uploadDir + "\\memory_video\\" + memory.getFilename()).delete();
                }
                if (archiveService.deleteMemory(memory.getMemoryid()) <= 0) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/collection/{collectionid} 에러");
                }
            }
            if (archiveService.deleteCollection(collectionid) > 0) {
                return ResponseEntity.ok("삭제 성공");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/collection/{collectionid} 에러");
            }
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/collection/{collectionid} 에러");
        }
    }

    @PostMapping(value = "/editMemory", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editMemory(@ModelAttribute Memory memory, @RequestParam(name = "file", required = false) MultipartFile file, @RequestParam String previousFileType, @RequestParam String previousFileName) {
        log.info("ArchiveController.editMemory...");
        log.info("memory: " + memory); // memory: Memory(memoryid=0, memoryType=text, collectionid=0, title=123123123123123, content=1231231135161, filename=null, filepath=null, createdDate=null, memoryOrder=0)
        memory.setCreatedDate(new Date());
        if (memory.getMemoryType().equals("text")) {
            memory.setFilename(null);
            memory.setFilepath(null);
            if (archiveService.insertMemory(memory) > 0) {
                try {
                    if (previousFileType.equals("image")) {
                        new File(uploadDir + "\\memory_img\\" + previousFileName).delete();
                    } else if (previousFileType.equals("video")) {
                        new File(uploadDir + "\\memory_video\\" + previousFileName).delete();
                    }
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/editMemory 에러");
                }
                return ResponseEntity.ok("저장 성공");
            } else {
                // 텍스트 메모리 저장 실패
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/editMemory 에러");
            }
        } else if (memory.getMemoryType().equals("image") || memory.getMemoryType().equals("video")) {
            if (file != null && !file.isEmpty()) {
                String savePath = uploadDir;
                String uuid = UUID.randomUUID().toString();
                String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
                memory.setFilename(uuid + "." + ext);
                if (memory.getMemoryType().equals("image")) {
                    savePath += "/memory_img";
                } else if (memory.getMemoryType().equals("video")) {
                    savePath += "/memory_video";
                }
                memory.setFilepath(savePath.substring(2) + "/" + memory.getFilename());
                memory.setContent(null);
                if (archiveService.insertMemory(memory) > 0) {
                    try {
                        file.transferTo(new File(savePath, memory.getFilename()));
                        if (previousFileType.equals("image")) {
                            new File(uploadDir + "\\memory_img\\" + previousFileName).delete();
                        } else if (previousFileType.equals("video")) {
                            new File(uploadDir + "\\memory_video\\" + previousFileName).delete();
                        }
                        return ResponseEntity.ok("저장 성공");
                    } catch (Exception e) {
                        // 파일 저장 실패
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/editMemory 에러");
                    }
                } else {
                    // 미디어 메모리 저장 실패
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/editMemory 에러");
                }
            } else {
                // 미디어 타입인데 파일 없음
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/editMemory 에러");
            }
        } else {
            // 파일 타입이 조건에 부합하지 않음
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/editMemory 에러");
        }
    }

    @DeleteMapping("/memory/{memoryid}")
    public ResponseEntity<?> memoryDelete(@PathVariable int memoryid, @RequestParam String userid) {
        log.info("ArchiveController.memoryDelete...");

        Memory memory = archiveService.findMemoryByMemoryid(memoryid);
        int collectionid = memory.getCollectionid();
        ArrayList<Memory> list = archiveService.findAllUserMemories(userid, collectionid);
        if (list.size() == 1) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/memory/{memoryid} 에러");
        }
        if (archiveService.deleteMemory(memoryid) > 0) {
            return ResponseEntity.ok("삭제 성공");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/memory/{memoryid} 에러");
    }

    @GetMapping("/setThumbnail/{memoryid}")
    public ResponseEntity<?> setThumbnail(@PathVariable int memoryid) {
        log.info("ArchiveController.setThumbnail...");
        Memory memory = archiveService.findMemoryByMemoryid(memoryid);
        if (archiveService.setThumbnail(memory) > 0) {
            return ResponseEntity.ok("썸네일 설정 성공");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/setThumbnail/{memoryid} 에러");
    }


}