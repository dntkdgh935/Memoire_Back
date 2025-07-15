package com.web.memoire.library.controller;


import com.web.memoire.library.model.service.LibraryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/library")
@CrossOrigin
public class LibraryController {

    @Autowired
    private LibraryService libraryService;

    private String tempLoginUserId="bc2b3a47-c06d-4693-8e47-7e8422458919";


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
    public ResponseEntity<?> getCollectionDetail(@PathVariable int collectionId) {
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
            @RequestParam("collectionId") int collectionId,
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
            @RequestParam("collectionId") int collectionId,
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
            @RequestParam("collectionId") int collectionId)
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
            @RequestParam("collectionId") int collectionId
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
    public ResponseEntity<?> getMemoriesByCollectionId(@PathVariable int collectionid) {
        log.info("LibraryController.getMemoriesByCollectionId...");

        try {
            return ResponseEntity.ok(libraryService.getMemoriesByCollectionId(collectionid));
        } catch (Exception e) {
            log.error("Error while fetching memories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("메모리 조회 실패");
        }
    }

    // memoryId에 해당하는 메모리 정보 조회
    @GetMapping("/memory/{memoryId}")
    public ResponseEntity<?> getMemoryDetail(@PathVariable int memoryId) {
        log.info("LibraryController.getMemoryDetail... memoryId: {}", memoryId);

        try {
            return ResponseEntity.ok(libraryService.getMemoryDetail(memoryId)); // 서비스 호출
        } catch (Exception e) {
            log.error("Error while fetching memory detail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("메모리 상세 조회 실패");
        }
    }

    @PostMapping("/toggleFollow")
    public ResponseEntity<?> toggleFollowRequest(
            @RequestParam("userid") String userid,
            @RequestParam("targetid") String targetid
    ) {
        log.info("🔁 팔로우 토글 요청 - user: {}, target: {}", userid, targetid);

        try {
            libraryService.toggleFollowRequest(userid, targetid);
            return ResponseEntity.ok("팔로우 상태 토글 완료");
        } catch (Exception e) {
            log.error("팔로우 상태 토글 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("팔로우 상태 토글 실패");
        }
    }




}