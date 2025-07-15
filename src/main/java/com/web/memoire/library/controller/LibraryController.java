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

    //TODO: tempLoginUserId 모두 대체하기
    private String tempLoginUserId="ee418479-6ac2-43f1-96e0-e5413f4926cb";

    //TODO: WebClient 추가하기(추천용)
//    private final WebClient webClient;

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


    // 비로그인시 전체 public collection 리턴
    // TODO:
    @GetMapping("/discover/{selectedTag}")
    public ResponseEntity<?> getAllColls() {
        log.info("LibraryController.getAllColls...");
        log.info("비로그인 유저 전체 컬렉션 조회");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        try {
            return ResponseEntity.ok(libraryService.getAllPublicCollectionView());//(userid));//("user001"));
        } catch (Exception e) {
            log.error("Error while fetching colls", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("전체 컬렉션 조회 실패");
        }
    }

    // 로그인 유저에게 selectedTag(전체, 팔로잉, 기타)에 대한 추천 진행
    @GetMapping("/discover/{selectedTag}/{userid}")
    public ResponseEntity<?> getRecColls4LoginUser(@PathVariable String selectedTag, @PathVariable String userid) {
        log.info("LibraryController.getAllColls...4 로그인 유저!!");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("\uD83C\uDFF0 현재 로그인 유저: "+auth.toString());

        if (selectedTag.equals("전체")) {
            try {
                return ResponseEntity.ok(libraryService.getAllColls4LoginUser(userid));
            } catch (Exception e) {
                log.error("Error while fetching colls", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("전체 컬렉션 조회 실패");
            }
        }
        else if (selectedTag.equals("팔로잉")) {
            try{
                return ResponseEntity.ok(libraryService.getFollowingColls4LoginUser(userid));
            }catch (Exception e) {
                log.error("Error while fetching 팔로잉 중인 colls", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("팔로잉 컬렉션 조회 실패");
            }
        }
        // 기타 태그 선택시
        else if (selectedTag.length()>0 &&(selectedTag instanceof String))
        {
            try{
//                return ResponseEntity.ok(libraryService.getTopicColls4LoginUser(userid, selectedTag));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("미개발 단계");
            }catch (Exception e) {
                log.error("Error while fetching 팔로잉 중인 colls", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("팔로잉 컬렉션 조회 실패");
            }
        }
        // 태그가 없거나 옵션에 없는 경우(오류)
        else{
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("태그 선택 오류");
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
            @RequestParam("targetid") String targetid,
            @RequestParam("nextRel") String nextRel
    ) {
        log.info("🔁 팔로우 토글 요청 - user: {}, target: {}", userid, targetid);

        try {
            libraryService.toggleFollowRequest(userid, targetid, nextRel);
            return ResponseEntity.ok("팔로우 상태 토글 완료");
        } catch (Exception e) {
            log.error("팔로우 상태 토글 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("팔로우 상태 토글 실패");
        }
    }

    @GetMapping("/getRelationshipStatus")
    public ResponseEntity<?> getRelationshipStatus(String userid, String targetid) {
        log.info("🔁 관계 확인 요청 : userid: {}, target: {}", userid, targetid);

        try{
            return ResponseEntity.ok(libraryService.getRelationshipStatus(userid, targetid));

        }catch(Exception e){
            log.error("관계 확인 실패");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("관계 확인 실패");
        }
    }
    // TODO: Natural Recommendation
    // FastAPI로 GET 요청 보내기
    /*@GetMapping("/get-recommendations/{userId}")
    public ResponseEntity<?> getRecommendations(@PathVariable String userId) {
        log.info("LibraryController.getRecommendations... for userId: {}", userId);

        try {
            // FastAPI로 추천 요청 보내기
            String url = "/recommendations/" + userId;

            // WebClient를 사용하여 FastAPI 서버에 GET 요청 보내기
            WebClient webClient = webClientBuilder.baseUrl("http://localhost:8000").build();

            // FastAPI 서버에서 추천 점수 받기
            String recommendations = webClient.get()  // GET 요청으로 변경
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();  // 동기식 호출

            log.info("Received recommendations from FastAPI: {}", recommendations);

            return ResponseEntity.ok(recommendations); // 추천 결과 반환
        } catch (Exception e) {
            log.error("Error while fetching recommendations", e);
            return ResponseEntity.status(500).body("추천 요청 실패");
        }
    }
*/


}