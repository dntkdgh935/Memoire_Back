package com.web.memoire.library.controller;


import com.web.memoire.common.dto.CollView;
import com.web.memoire.common.dto.Collection;
import com.web.memoire.common.dto.FollowRequest;
import com.web.memoire.common.dto.Relationship;
import com.web.memoire.common.entity.CollectionEntity;
import com.web.memoire.library.model.service.LibraryService;
import com.web.memoire.user.model.dto.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/library")
@CrossOrigin
public class LibraryController {

    @Autowired
    private LibraryService libraryService;


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
    public ResponseEntity<?> getAllColls(@PathVariable String selectedTag) {
        log.info("LibraryController.getAllColls...");
        log.info("비로그인 유저 전체 컬렉션 조회");
        //Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        //비로그인 유저가 '전체' 선택한 경우
        if (selectedTag.equals("전체")) {
            try {
                return ResponseEntity.ok(libraryService.getAllPublicCollectionView());//(userid));//("user001"));
            } catch (Exception e) {
                log.error("Error while fetching colls", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("전체 컬렉션 조회 실패");
            }
        }
        //비로그인 유저가 다른 태그 선택한 경우
        else if (selectedTag.length()>0 &&(selectedTag instanceof String)){
            try{
                return ResponseEntity.ok(libraryService.getTopicColls4Anon(selectedTag));
            }
            catch (Exception e){
                log.error("Error while fetching colls", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("비로그인 유저 태그 컬렉션 조회 실패");
            }
        }
        return null;
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
            log.info("선택된 태그: "+selectedTag);
            try{
                return ResponseEntity.ok(libraryService.getTopicColls4LoginUser(userid, selectedTag));
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("미개발 단계");
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
    // TODO: 프론트쪽 요청 바꾸기
    @GetMapping("/collection/{collectionId}/{userid}")
    public ResponseEntity<?> getCollectionDetail(@PathVariable int collectionId, @PathVariable String userid) {

        log.info("LibraryController.getCollectionDetail...");
        try {
            return ResponseEntity.ok(libraryService.getCollectionDetail(collectionId, userid));
        } catch (Exception e) {
            log.error("Error while fetching collection detail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("컬렉션 상세 조회 실패");
        }
    }

    @GetMapping("/collection/{collectionId}")
    public ResponseEntity<?> getCollectionDetail4Anon(@PathVariable int collectionId) {

        log.info("LibraryController.getCollectionDetail...");
        try {
            return ResponseEntity.ok(libraryService.getCollectionDetail4Anon(collectionId));
        } catch (Exception e) {
            log.error("Error while fetching collection detail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("컬렉션 상세 조회 실패");
        }
    }

    @PostMapping("/togglelike")
    public ResponseEntity<?> toggleLikeColl(
            @RequestParam("userid") String userid,
            @RequestParam("collectionId") int collectionId,

            @RequestParam("isLiked") boolean isLiked
    ) {
        log.info("👍 좋아요 요청 - user: {}, collection: {}, isLiked: {}", userid, collectionId, isLiked);

        try {
            if (isLiked) {
                libraryService.addLike(userid, collectionId);
            } else {
                libraryService.removeLike(userid, collectionId);
            }

            return ResponseEntity.ok("처리 성공");
        } catch (Exception e) {
            log.error("좋아요 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("처리 중 오류 발생");
        }
    }

    @PostMapping("/togglebm")
    public ResponseEntity<?> toggleBMColl(
            @RequestParam("userid") String userid,
            @RequestParam("collectionId") int collectionId,
            @RequestParam("isBookmarked") boolean isBookmarked
    ) {
        log.info("👍북마크 요청 - user: {}, collection: {}, isBookmarked: {}", userid, collectionId, isBookmarked);

        try {
            if (isBookmarked) {
                libraryService.addBM(userid, collectionId);
            } else {
                libraryService.removeBM(userid, collectionId);
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

    @GetMapping("/search/collection")
    public ResponseEntity<?> searchCollections(
            @RequestParam("query") String query,
            @RequestParam("userid") String userid) {
        log.info("📨 검색어 요청: {}", query);

        try {
            List<CollView> result = libraryService.hello(query, userid);
            log.info("🟢 FastAPI 응답 결과: {}", result);

            // ✅ JSON 배열 그대로 클라이언트에 반환
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("🚨 컬렉션 검색 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("검색 중 오류 발생");
        }
    }

    @GetMapping("/search/tag")
    public ResponseEntity<?> searchTags(
            @RequestParam("query") String query,
            @RequestParam("userid") String userid) {
        log.info("📨 tag 검색어 요청: {}", query);

        try {
            List<CollView> result = libraryService.findCollViewsWithTag(query, userid);
            // tag 검색수 추가
            libraryService.addTagSearchCount(query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("🚨 태그로 컬렉션 검색 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("검색 중 오류 발생");
        }
    }



    // 유저 검색 (검색어와 userid를 함께 받기)
    @GetMapping("/search/user")
    public ResponseEntity<?> searchUsers(
            @RequestParam("query") String query,
            @RequestParam("loginUserid") String loginUserid) {
        log.info("LibraryController.searchUsers... 검색어: {}, userid: {}", query, loginUserid);

        try {
            // 라이브러리 서비스에서 컬렉션 검색 실행
            // 차단 관계가 있는 유저는 검색되면 안 됨
            return ResponseEntity.ok(libraryService.searchUsers(query, loginUserid));
        } catch (Exception e) {
            log.error("컬렉션 검색 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("유저 검색 실패");
        }
    }

    @GetMapping("/followreqs")
    public ResponseEntity<List<FollowRequest>> getFollowRequests(@RequestParam("userid") String userid) {
        try {
            log.info("LibraryController.getFollowRequests... <UNK> userid: {}", userid);
            List<FollowRequest> followRequests = libraryService.getFollowRequests(userid);
            log.info("요청 내용:"+followRequests);
            if (followRequests.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new ArrayList<>());
            } else {
                return ResponseEntity.ok(followRequests);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/followapproval")
    public ResponseEntity<Object> followApproval(@RequestParam("requesterid") String requesterid, @RequestParam("targetid") String targetid) {
        try{
            String nextRel="1";
            libraryService.setRelationship(requesterid, targetid, nextRel);
            return ResponseEntity.ok().build();
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("DB에 관계 업데이트 실패");
        }
    }

    @GetMapping("/archiveVisit")
    public ResponseEntity<?> getArchiveMain(@RequestParam String userid, @RequestParam String ownerid) {
        log.info("ArchiveController.getArchiveMain...방문 가능 여부 확인필");
        log.info("컨트롤러 로그인 유저: "+userid+"방문 대상:"+ownerid);
        try {
            return ResponseEntity.ok(libraryService.findVisibleOwnerCollections(userid, ownerid));
        }catch(AccessDeniedException ae){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ae.getMessage());
        }
        catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/collections 에러");
        }
    }

    @GetMapping("/userTopTags")
    public ResponseEntity<?> userTopTags(@RequestParam("userid") String userid){
        try {
            return ResponseEntity.ok(libraryService.getUserTopTags(userid));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("유저 top tags 리턴 실패");
        }
    }

    // TODO: 추후 "전체" 대체
    // TODO: 로그인추천 30개씩 리턴
    @GetMapping("/recommend/{userid}")
    public ResponseEntity<?> topNRec4LoginUser (@PathVariable String userid,
                                                @PageableDefault(size = 5) Pageable pageable) {
        log.info("LibraryController.getRecommendations... for userId: {}, page:{}", userid, pageable);

        try {
            return ResponseEntity.ok(libraryService.getTopNRec4LoginUser(userid, pageable));
        }
        catch (Exception e) {
            log.error("Error while fetching recommendations", e);
            return ResponseEntity.status(500).body("추천 요청 실패");
        }
    }

    // TODO: 비로그인 추천 30개씩 리턴.. 비로그인은 그냥 좋아요+북마크 순서대로?
    @GetMapping("/recommend/guest")
    public ResponseEntity<?> topNRec4AnonUser () {
        log.info("LibraryController.getRecommendations for ANON user");

        try {
            //TODO: getAllPublicCollectionView에 정렬 로직 추가하기
            return ResponseEntity.ok(libraryService.getAllPublicCollectionView());
        }
        catch (Exception e) {
            log.error("Error while fetching recommendations", e);
            return ResponseEntity.status(500).body("추천 요청 실패");
        }
    }

    // 메모리 신고 기능
    @PostMapping("/report/{memoryid}/{userid}")
    public ResponseEntity<?> reportMemory(
            @PathVariable("memoryid") int memoryid,
            @PathVariable("userid") String userid,
            @RequestBody Map<String, String> body) {

        String reportReason = body.get("content");

        try {
            libraryService.reportMemory(memoryid, userid, reportReason); // 서비스 계층에서 처리
            return ResponseEntity.ok().body("메모리 신고 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("🚨 신고 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // 좋아요한 사람 리스트 불러오기(로그인 유저 = 컬렉션 주인인 경우만 요청 가능)
    @GetMapping("/whoLiked")
    public ResponseEntity<?> getWhoLiked(@RequestParam int collectionid,
                                         @RequestParam String userid) {
        log.info("LibController.getWhoLiked...");
        try {
            return ResponseEntity.ok(libraryService.getWhoLiked(collectionid, userid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/follower 에러");
        }
    }

    // 북마크한 사람 리스트 불러오기(로그인 유저 = 컬렉션 주인인 경우만 요청 가능)
    @GetMapping("/whoBookmarked")
    public ResponseEntity<?> getWhoBookmarked(@RequestParam int collectionid,
                                         @RequestParam String userid) {
        log.info("LibController.getWhoBookmarked...");
        try {
            return ResponseEntity.ok(libraryService.getWhoBookmarked(collectionid, userid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/follower 에러");
        }
    }


}