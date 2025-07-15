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

    @Value("D:/upload_files")
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

//    // 새 공지글 등록 요청 처리용 (파일 업로드 기능 포함)
//    // insert 쿼리문 실행 요청임 => 전송방식 POST 임 => @PostMapping 지정해야 함
//    // "/admin/**" 으로 보안설정을 따로 하고 싶다면, 클래스 위의 @RequestMapping 사용하면 안됨
//    @PostMapping(value = "/admin/notice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<Map<String, Object>> noticeInsertMethod(
//            @ModelAttribute Notice notice,
//            @RequestParam(name="ofile", required=false) MultipartFile mfile	) {
//        log.info("/admin/notice : " + notice);
//
//        Map<String, Object> map = new HashMap<>();
//
//        //공지사항 첨부파일 저장 폴더를 경로 저장 (application.properties 에 경로 설정 추가)
//        String savePath = uploadDir + "/notice";
//        log.info("savePath : " + savePath);
//
//        //첨부파일이 있을 때
//        if (mfile != null && !mfile.isEmpty()) {
//            // 전송온 파일이름 추출함
//            String fileName = mfile.getOriginalFilename();
//            String renameFileName = null;
//
//            //저장 폴더에는 변경된 파일이름을 파일을 저장 처리함
//            //바꿀 파일명 : 년월일시분초.확장자
//            if (fileName != null && fileName.length() > 0) {
//                renameFileName = FileNameChange.change(fileName, "yyyyMMddHHmmss");
//                log.info("변경된 첨부 파일명 확인 : " + renameFileName);
//
//                try {
//                    //저장 폴더에 바뀐 파일명으로 파일 저장하기
//                    mfile.transferTo(new File(savePath + "\\" + renameFileName));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//                }
//            } //파일명 바꾸어 저장하기
//
//            //notice 객체에 첨부파일 정보 저장하기
//            notice.setOriginalFilePath(fileName);
//            notice.setRenameFilePath(renameFileName);
//        } //첨부파일 있을 때
//
//        //새로 등록할 공지글 번호는 현재 마지막 등록글 번호에 + 1 한 값으로 저장 처리함
//        notice.setNoticeNo(noticeService.selectLast().getNoticeNo() + 1);
//
//        if (noticeService.insertNotice(notice) > 0) {
//            map.put("status", "success");
//            map.put("message", "새 공지 등록 성공!");
//            return ResponseEntity.status(HttpStatus.CREATED).body(map);
//        } else {
//            map.put("status", "fail");
//            map.put("message", "DB 등록 실패");
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
//        }
//
//    }  // insertNotice closed
    @PostMapping(value = "/newColl", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> insertNewCollection(@ModelAttribute Collection collection, @ModelAttribute Memory memory, @RequestParam(name="file", required=false) MultipartFile file) {
        log.info("ArchiveController.insertNewCollection...");
        log.info("collection : " + collection); //collection : Collection(collectionid=null, authorid=blabla, collectionTitle=blabla, readCount=0, visibility=1, createdDate=null, titleEmbedding=null, color=#000000)
        log.info("memory: " + memory); //memory: Memory(memoryid=0, memoryType=text, collectionid=0, title=123123123123123, content=1231231135161, filename=null, filepath=null, createdDate=null, memoryOrder=0)
        collection.setCreatedDate(new Date());
//        TODO: collection.setTitleEmbedding(blabla);
        int collectionid = archiveService.insertCollection(collection);
        if (collectionid > 0) {
            memory.setCollectionid(collectionid);
            memory.setCreatedDate(new Date());
            memory.setMemoryOrder(1);
            if (memory.getMemoryType().equals("text")) {
                if (archiveService.insertMemory(memory) > 0) {
                    return ResponseEntity.ok("저장 성공");
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newColl 에러");
                }
            }
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
                memory.setFilepath(savePath);
                if (archiveService.insertMemory(memory) > 0) {
                    try {
                        file.transferTo(new File(savePath, memory.getFilename()));
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newColl 에러");
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newColl 에러");
                }


            }
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/newColl 에러");
        }
        return ResponseEntity.ok("저장 성공");
    }


}
