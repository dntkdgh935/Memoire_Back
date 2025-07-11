package com.web.memoire.library.controller;



import com.web.memoire.library.jpa.repository.LibMemoryRepository;
import com.web.memoire.library.model.service.LibraryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/library")
@CrossOrigin
public class LibraryController {

    @Autowired
    private LibraryService libraryService;

    private String tempLoginUserId="c5950e60-6872-4510-9823-2a887e957079";


    //ArhciveMain.js용=========================================================
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


    //tempUser에게 모든 public collection 리턴
    @GetMapping("/discover/all")
    public ResponseEntity<?> getAllColls() {
        log.info("LibraryController.getAllColls...");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userid = auth.getName();
        log.info("\uD83C\uDFF0 현재 로그인 유저: "+auth.toString());

        try {
            return ResponseEntity.ok(libraryService.getAllPublicCollectionView(tempLoginUserId));//(userid));//("user001"));
        } catch (Exception e) {
            log.error("Error while fetching colls", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("전체 컬렉션 조회 실패");
        }
    }

    // LibCollDetailView.js용 (컬렉션 상세 페이지)=========================================================
    // 컬렉션 아이디로 컬렉션 정보 가져옴
    @GetMapping("/collection/{collectionId}")
    public ResponseEntity<?> getCollectionDetail(@PathVariable String collectionId) {
        log.info("LibraryController.getCollectionDetail...");
        try {
            return ResponseEntity.ok(libraryService.getCollectionDetail(collectionId, tempLoginUserId));
        } catch (Exception e) {
            log.error("Error while fetching collection detail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("컬렉션 상세 조회 실패");
        }
    }

    @PostMapping("/togglelike")
    public ResponseEntity<?> toggleLikeColl(
            @RequestParam("collectionId") String collectionId,
            @RequestParam("isLiked") boolean isLiked
    ) {
        log.info("👍 좋아요 요청 - user: {}, collection: {}, isLiked: {}", tempLoginUserId, collectionId, isLiked);

        try {
            if (isLiked) {
                libraryService.addLike(tempLoginUserId, collectionId);
            } else {
                libraryService.removeLike(tempLoginUserId, collectionId);
            }

            return ResponseEntity.ok("처리 성공");
        } catch (Exception e) {
            log.error("좋아요 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("처리 중 오류 발생");
        }
    }

    @PostMapping("/togglebm")
    public ResponseEntity<?> toggleBMColl(
            @RequestParam("collectionId") String collectionId,
            @RequestParam("isBookmarked") boolean isBookmarked
    ) {
        log.info("👍북마크 요청 - user: {}, collection: {}, isBookmarked: {}", tempLoginUserId, collectionId, isBookmarked);

        try {
            if (isBookmarked) {
                libraryService.addBM(tempLoginUserId, collectionId);
            } else {
                libraryService.removeBM(tempLoginUserId, collectionId);
            }

            return ResponseEntity.ok("처리 성공");
        } catch (Exception e) {
            log.error("북마크 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("처리 중 오류 발생");
        }
    }

    @GetMapping("/countlike")
    public ResponseEntity<?> countLike(
            @RequestParam("collectionId") String collectionId)
    {
        try {
            int likeCount = libraryService.countLikesByCollectionId(collectionId);
            return ResponseEntity.ok(likeCount);
        } catch (Exception e) {
            log.error("좋아요 수 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("좋아요 수 조회 실패");
        }
    }


    @GetMapping("/countbm")
    public ResponseEntity<?> countBookmark(
            @RequestParam("collectionId") String collectionId
    ) {
        try {
            int bmCount = libraryService.countBookmarksByCollectionId(collectionId);
            return ResponseEntity.ok(bmCount);
        } catch (Exception e) {
            log.error("북마크 수 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("북마크 수 조회 실패");
        }
    }



    // collectionid에 해당하는 모든 메모리 조회
    @GetMapping("/collection/memories/{collectionid}")
    public ResponseEntity<?> getMemoriesByCollectionId(@PathVariable String collectionid) {
        log.info("LibraryController.getMemoriesByCollectionId...");

        try {
            return ResponseEntity.ok(libraryService.findByCollectionid(collectionid));
        } catch (Exception e) {
            log.error("Error while fetching memories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("메모리 조회 실패");
        }

    }

}
