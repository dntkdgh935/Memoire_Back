package com.web.memoire.library.model.service;

import com.web.memoire.common.dto.CollView;
import com.web.memoire.common.dto.FollowRequest;
import com.web.memoire.common.dto.Tag;
import com.web.memoire.common.dto.UserCardView;
import com.web.memoire.common.entity.*;
import com.web.memoire.library.jpa.repository.*;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.model.dto.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryService {
    @Autowired
    private final LibTagRepository libTagRepository;
    private final LibCollectionRepository libCollectionRepository;
    private final LibMemoryRepository libMemoryRepository;
    private final LibBookmarkRepository libBookmarkRepository;
    private final LibLikeRepository libLikeRepository;
    private final LibUserRepository libUserRepository;
    private final LibRelationshipRepository libRelationshipRepository;
    private final LibCollTagRepository libCollTagRepository;

    // 모든 태그 가져오기
    public List<Tag> getAllTags() {
        List<TagEntity> tagEntities = libTagRepository.findAll();
        return tagEntities.stream()
                .map(TagEntity::toDto)
                .toList();
    }

    // 상위 5개 태그 가져오기
    public List<Tag> getTopTags() {
        List<TagEntity> tagEntities = libTagRepository.findTop5TagsByRownum();
        return tagEntities.stream()
                .map(TagEntity::toDto)
                .toList();
    }

    // 비로그인 유저에게 public Collection Return
    public List<CollView> getAllPublicCollectionView() {
        List<CollectionEntity> publicCollections = libCollectionRepository.findByVisibility(1);
        List<CollView> collViews = new ArrayList<>();
        for (CollectionEntity collection : publicCollections) {
            CollView cv = makeCollectionView(collection.getId(), null);
            collViews.add(cv);
        }
        return collViews;
    }


    // ✅ public(visibility=1) collection 모두 불러오기
    public List<CollView> getAllColls4LoginUser(String userId) {
        List<CollView> collViews = new ArrayList<>();

        //대상 컬렉션: visibility 가 public이거나 follower 대상인 경우
        List<CollectionEntity> publicCollections = libCollectionRepository.findByVisibilityIn(Arrays.asList("1", "2"));
        for (CollectionEntity collection : publicCollections) {
            Optional<RelationshipEntity> userToOtherRel = libRelationshipRepository.findByUseridAndTargetid(userId, collection.getAuthorid());
            Optional<RelationshipEntity> OtherToUserRel = libRelationshipRepository.findByUseridAndTargetid(collection.getAuthorid(), userId);

            // 1. 자신의 컬렉션은 보이지 않게 (authorid == userId인 경우 제외)
            if (collection.getAuthorid().equals(userId)) {
                continue; // 자신의 컬렉션은 제외
            }
            // 2. 서로가 차단된 경우 보이지 않게
            if ((userToOtherRel.isPresent() && userToOtherRel.get().getStatus().equals("2") )
                    || (OtherToUserRel.isPresent() && OtherToUserRel.get().getStatus().equals("2"))) {
                continue;
            }
            // 2. 팔로워 대상인데 팔로잉 안하는 경우 보이지 않게 (팔로워 대상인데 관계 없는 경우.. ㅋ)
            if (collection.getVisibility()==2) {
                // 팔로우 상태이면 추가
                if (userToOtherRel.isPresent() && "1".equals(userToOtherRel.get().getStatus())) {
                    collViews.add(makeCollectionView(collection.getId(), userId));
                } else {
                    // 팔로우하지 않으면 접근 불가
                    log.info("user: " + userId + ", author: " + collection.getAuthorid() + " 팔로우하지 않음.");
                }
            }
            // 4. 이외의 전체 공개 컬렉션 추가
            else if (collection.getVisibility()==1) {
                collViews.add(makeCollectionView(collection.getId(), userId));
            }
        }
        return collViews;
    }



    @Transactional
    public void addLike(String userid, int collectionId) {
        LikeEntity like = LikeEntity.builder()
                .userid(userid)
                .collectionid(collectionId)
                .likedDate(new Date())
                .build(); // likedDate는 자동으로 저장됨

        libLikeRepository.save(like);
    }
    @Transactional
    public void removeLike(String userid, int collectionId) {
        libLikeRepository.deleteByUseridAndCollectionid(userid, collectionId);
    }

    @Transactional
    public void addBM(String userid, int collectionId) {
        BookmarkEntity BM = BookmarkEntity.builder()
                .userid(userid)
                .collectionid(collectionId)
                .build();
        libBookmarkRepository.save(BM);
    }
    @Transactional
    public void removeBM(String userid, int collectionId) {
        libBookmarkRepository.deleteByUseridAndCollectionid(userid, collectionId );
    }

    public int countLikesByCollectionId(int collectionId){
        return libLikeRepository.countLikeEntitiesByCollectionid(collectionId);
    }
    public int countBookmarksByCollectionId(int collectionId){
        return libBookmarkRepository.countBookmarkEntitiesByCollectionid(collectionId);
    }

    public CollView getCollectionDetail(int collectionId, String userId) throws Exception {
        CollectionEntity collection = libCollectionRepository.findByCollectionid(collectionId);
        log.info("getCollectionDetail 서비스 작동중");
        // 컬렉션이 존재하지 않으면 예외 처리
        if (collection == null) {
            throw new IllegalArgumentException("컬렉션을 찾을 수 없습니다.");
        }

        //유저가 자신의 컬렉션은 그냥 접근 가능
        if (userId.equals(collection.getAuthorid())){
            return makeCollectionView(collectionId, userId);
        }
        // 공개 범위가 1 (공개)일 때
        if (collection.getVisibility() == 1) {
            // userId가 차단(2)된 경우 접근 불가
            Optional<RelationshipEntity> relationship1 = libRelationshipRepository.findByUseridAndTargetid(userId, collection.getAuthorid());
            Optional<RelationshipEntity> relationship2 = libRelationshipRepository.findByUseridAndTargetid(collection.getAuthorid(), userId);
            if ((relationship1.isPresent() && "2".equals(relationship1.get().getStatus()))
                    ||(relationship2.isPresent() && "2".equals(relationship2.get().getStatus()))) {
                 log.info("user: " + userId+", author: " + collection.getAuthorid());
                throw new Exception("이 컬렉션에 접근할 권한이 없습니다."); // 접근 권한 없음
            }
            else {
                // 그 외의 경우 접근 가능
                log.info("공개 컬렉션 뷰 만들기");
                return makeCollectionView(collectionId, userId);
            }
        }

        // 공개 범위가 2 (팔로워만)일 때
        else if (collection.getVisibility() == 2) {
            Optional<RelationshipEntity> relationship = libRelationshipRepository.findByUseridAndTargetid(userId, collection.getAuthorid());
            if (relationship.isPresent() && "1".equals(relationship.get().getStatus())) {
                return makeCollectionView(collectionId, userId);
            } else {
                throw new Exception("이 컬렉션에 접근할 권한이 없습니다."); // 접근 권한 없음
            }
        }

        // 공개 범위가 3 (작성자만)일 때
        else if (collection.getVisibility() == 3 && collection.getAuthorid().equals(userId)) {
            return makeCollectionView(collectionId, userId);// 작성자 본인이라면 실행
        }

        // 그 외의 경우 접근 불가
        else {
            throw new Exception("이 컬렉션에 접근할 권한이 없습니다.");
        }

    }

    public Object findByCollectionid(int collectionid) {
        CollectionEntity collection = libCollectionRepository.findByCollectionid(collectionid);
        return collection;
    }


    public Object getMemoriesByCollectionId(int collectionid) {
        List <MemoryEntity> memories =  libMemoryRepository.findByCollectionid(collectionid);
        Collections.sort(memories, Comparator.comparingInt(MemoryEntity::getMemoryOrder));
        return memories;
    }

    public Object getMemoryDetail(int memoryid) {
        MemoryEntity entity = libMemoryRepository.findByMemoryid(memoryid);
        log.info(entity.toString());
        return entity.toDto();
    }

    // TB_RELATIONSHIP : 팔로우 버튼 클릭 --> 요청(0) --> 승인시 팔로우(1)
    // 차단: 2
    @Transactional
    public void toggleFollowRequest(String userid, String targetid, String nextRel) {
        RelationshipId id = new RelationshipId(userid, targetid);

        Optional<RelationshipEntity> optional = libRelationshipRepository.findById(id);
        log.info("✅ toggleFollowRequest: " + optional.isPresent());

        if ("3".equals(nextRel)) {  // 관계 없음으로 설정하려면, 해당 관계 삭제
            libRelationshipRepository.deleteById(id);  // 관계 삭제
        } else {
            // 상태가 3이 아니면 관계가 존재하므로 상태 변경

            if (optional.isPresent()) {
                RelationshipEntity relationship = optional.get();
                relationship.setStatus(nextRel);  // 상태 변경
                libRelationshipRepository.save(relationship);  // 업데이트된 관계 저장
            } else {
                // 관계가 없을 경우 새로 관계를 추가 (상태 0: 요청됨으로)
                RelationshipEntity newRelationship = new RelationshipEntity();
                newRelationship.setUserid(userid);
                newRelationship.setTargetid(targetid);
                newRelationship.setStatus(nextRel);
                newRelationship.setFollowDate(new Date());
                libRelationshipRepository.save(newRelationship);  // 새 관계 추가
            }
        }
    }

    public Object getRelationshipStatus(String userid, String targetid) {
        RelationshipId id = new RelationshipId(userid, targetid);
        Optional<RelationshipEntity> optional = libRelationshipRepository.findById(id);

        // Optional이 비어있지 않은 경우
        if (optional.isPresent()) {
            return optional.get().getStatus();
        }
        else{
            return "3"; // no relationship
        }

    }

    // user가 팔로잉중인 유저들의 컬렉션들을 최신순으로 불러옴
    public Object getFollowingColls4LoginUser(String userid) {
        // 1. 팔로잉중인 유저 목록 불러오기
        List<RelationshipEntity> followingRels = libRelationshipRepository.findAllUserFollowing(userid);
        // 2. 유저별로 컬렉션 불러와서 합치기
        List<CollectionEntity> allCollections = new ArrayList<>();
        for (RelationshipEntity followingRel : followingRels) {
            List<CollectionEntity> collections = libCollectionRepository.findByAuthoridOrderByCreatedDateDesc(followingRel.getTargetid());
            allCollections.addAll(collections);
        }

        // 3. 정렬 로직 (TODO: 외부 시스템 연결)

        // 4. collView로 만들기
        List<CollView> collViews = new ArrayList<>();
        for (CollectionEntity collection : allCollections) {
            CollView cv = makeCollectionView(collection.getId(), userid);
            collViews.add(cv);
        }
        // 5. 리턴
        return collViews;
    }


    public List <CollView> searchCollections(String query, String userid) {
        // 컬렉션 이름에 키워드 포함한 것 리턴해, 한 컬렉션마다 CollView로 전환(makeCollectionView(collid, userid) 사용)

        // 컬렉션 이름에 query가 포함된 것들을 가져오기
        List<CollectionEntity> collections = libCollectionRepository.findByCollectionTitleContaining(query);

        List<CollView> collViews = new ArrayList<>();
        for (CollectionEntity collection : collections) {
            CollView cv = makeCollectionView(collection.getId(), userid);
            collViews.add(cv);
        }

        return collViews;
    }

    public List<FollowRequest> getFollowRequests(String userid) {
        log.info("LibraryService에서 팔로우 목록 조회 시작");
        List<RelationshipEntity> relationships = libRelationshipRepository.findByTargetidAndStatus(userid, "0");
        log.info("관계 상태: "+relationships);
        List<FollowRequest> followRequests = new ArrayList<>();


        for (RelationshipEntity relationship : relationships) {
            try {
                String requesterId = relationship.getUserid();
                String nickname = libUserRepository.findNicknameByUserId(requesterId);
                String profileImage = libUserRepository.findProfileImagePathByUserId(requesterId);

                // Optional을 사용하거나 null 체크를 할 수 있습니다.
                if (nickname == null) {
                    log.warn("닉네임이 없습니다. userId: {}", requesterId);
                    nickname = "알 수 없음";  // 기본값 설정
                }
                if (profileImage == null) {
                    log.warn("프로필 이미지가 없습니다. userId: {}", requesterId);
                    profileImage = "";  // 기본값 설정
                }
                log.info("요청자 닉네임:"+nickname+ ", 프로필 path: "+profileImage);

                FollowRequest followRequest = FollowRequest.builder()
                        .requesterid(requesterId)
                        .requesternickname(nickname)
                        .requesterProfileImage(profileImage)
                        .build();
                followRequests.add(followRequest);
            } catch (Exception e) {
                log.error("팔로우 요청 변환 중 에러 발생", e);
            }
        }
        log.info("찾은 팔로우 리퀘스트 리스트:"+followRequests);
        return followRequests;
    }

    public void setRelationship(String userid, String targetid, String nextRel) {
        try{
            RelationshipId id = new RelationshipId(userid, targetid);
            Optional<RelationshipEntity> optional = libRelationshipRepository.findById(id);

            if ("3".equals(nextRel)) {  // 관계 없음으로 설정하려면, 해당 관계 삭제
                libRelationshipRepository.deleteById(id);  // 관계 삭제
            } else {
                // 상태가 3이 아니면 관계가 존재하므로 상태 변경
                if (optional.isPresent()) {
                    RelationshipEntity relationship = optional.get();
                    relationship.setStatus(nextRel);  // 상태 변경
                    libRelationshipRepository.save(relationship);  // 업데이트된 관계 저장
                } else {
                    // 관계가 없을 경우 새로 관계를 추가 (상태 0(요청)으로)
                    RelationshipEntity newRelationship = new RelationshipEntity();
                    newRelationship.setUserid(userid);
                    newRelationship.setTargetid(targetid);
                    newRelationship.setStatus(nextRel);
                    newRelationship.setFollowDate(new Date());
                    libRelationshipRepository.save(newRelationship);  // 새 관계 추가
                }
            }
        }catch (Exception e){
            throw new RuntimeException("알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    //조회자 id(userId)에 맞게 컬렉션 정보를 리턴함
    private CollView makeCollectionView(int collectionId, String userId) {
        // 컬렉션 정보 조회
        CollectionEntity collection = libCollectionRepository.findByCollectionid(collectionId);

        // Memory 정보 조회 (memory_order = 1인 첫 번째 메모리)
        MemoryEntity memory = libMemoryRepository.findByCollectionidAndMemoryOrder(collection.getId(), 1);
        String thumbnailPath = memory != null ? memory.getFilepath() : null;
        String thumbType = memory != null ? memory.getMemoryType() : null;
        String textContent = memory != null ? memory.getContent() : null;

        // 작성자 정보 조회
        Optional<UserEntity> author = libUserRepository.findByUserId(collection.getAuthorid());
        String authorName = author.isPresent() ? author.get().getName() : null;
        String authorProfileImage = author.get().getProfileImagePath();
        LikeEntity like;
        BookmarkEntity bookmark;

        // TODO: userid 가 없는 경우(비로그인시) 따로 처리
        if (userId!=null) {
            // 로그인 유저의 좋아요 및 북마크 여부 확인
            like = libLikeRepository.findByUseridAndCollectionid(userId, collection.getId());
            bookmark = libBookmarkRepository.findByUseridAndCollectionid(userId, collection.getId());
        }
        else{
            like=null;
            bookmark=null;
        }
        // 컬렉션의 좋아요 수, 북마크 수
        int likeCount = countLikesByCollectionId(collection.getId());
        int bookmarkCount = countBookmarksByCollectionId(collection.getId());

        // CollView 객체 생성 및 반환
        return CollView.builder()
                .collectionid(collection.getId())
                .authorid(collection.getAuthorid())
                .authorname(authorName)
                .collectionTitle(collection.getCollectionTitle())
                .readCount(collection.getReadCount())
                .visibility(collection.getVisibility())
                .createdDate(collection.getCreatedDate())
                .titleEmbedding(collection.getTitleEmbedding())
                .color(collection.getColor())
                .thumbnailPath(thumbnailPath)
                .textContent(textContent)
                .userlike(like != null)
                .userbookmark(bookmark != null)
                .authorProfileImage(authorProfileImage)
                .thumbType(thumbType)
                .likeCount(likeCount)
                .bookmarkCount(bookmarkCount)
                .build();
    }

    public Object findVisibleOwnerCollections(String userid, String ownerid) throws AccessDeniedException {
        List<CollView> collViews = new ArrayList<>();
        log.info("서비스 - 로그인 유저: "+userid+"방문 대상:"+ownerid);
        Optional<RelationshipEntity> userToOtherRel = libRelationshipRepository.findByUseridAndTargetid(userid, ownerid);
        Optional<RelationshipEntity> OtherToUserRel = libRelationshipRepository.findByUseridAndTargetid(ownerid, userid);
        if (userToOtherRel.isPresent()) {
            log.info("관계1: " + userToOtherRel.get().getStatus());
        }
        if (OtherToUserRel.isPresent()) {
            log.info("관계2: " + OtherToUserRel.get().getStatus());
        }
        // 서로를 차단한 경우 오류 리턴
        if ((userToOtherRel.isPresent() && userToOtherRel.get().getStatus().equals("2"))||
                (OtherToUserRel.isPresent() && userToOtherRel.get().getStatus().equals("2"))) {
            // 예외 던지기
            log.error("차단 관계가 있습니다.");
            throw new AccessDeniedException("You are blocked by this user.");
        }

        // 유저의 모든 컬렉션 가져옴
        List<CollectionEntity> userCollections = libCollectionRepository.findByAuthoridAndVisibilityIn(ownerid, Arrays.asList("1", "2"));

        for (CollectionEntity collection : userCollections) {
            // 1. 팔로워 대상인데 팔로잉 안하는 경우 보이지 않게 (팔로워 대상인데 관계 없는 경우.. ㅋ)
            if (collection.getVisibility()==2) {
                // 팔로우 상태이면 추가
                if (userToOtherRel.isPresent() && "1".equals(userToOtherRel.get().getStatus())) {
                    collViews.add(makeCollectionView(collection.getId(), userid));
                } else {
                    // 팔로우하지 않으면 접근 불가
                    log.info("user: " + userid + ", author: " + collection.getAuthorid() + " 팔로우하지 않음.");
                }
            }
            // 2. 전체 공개이면 보임
            else if (collection.getVisibility()==1) {
                collViews.add(makeCollectionView(collection.getId(), userid));
            }
        }
        return collViews;
    }

    public Object searchUsers(String query, String loginUserid) {
        List<UserEntity> users = libUserRepository.findByNicknameContaining(query);
        List<UserCardView> userCardViews = new ArrayList<>();
        for (UserEntity user : users) {
            UserCardView userCardView = makeUserView(user.getUserId(), loginUserid);
            userCardViews.add(userCardView);
        }
        return userCardViews;
    }

    private UserCardView makeUserView(String targetId, String loginUserid) {
        //User테이블에서 정보 조회
        //TB_TAG에서 정보 조회
        //TB_RELATIONSHIP에서 정보 조회

        UserEntity user = libUserRepository.findByUserId(targetId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        String loginId = user.getLoginId();
        String nickname = user.getNickname();
        String profileImagePath= user.getProfileImagePath();
        String statusMessage=user.getStatusMessage();

        // 관계가 없으면 "3"을 리턴하도록 처리
        String relStatusWLoginUser = libRelationshipRepository.findByUseridAndTargetid(loginUserid, targetId)
                .map(RelationshipEntity::getStatus)  // 존재할 경우 상태 가져오기
                .orElse("3");

        List<String> userFreqTags = new ArrayList<>();
        List <CollectionEntity> userColls= libCollectionRepository.findByAuthoridAndVisibility(targetId,1);
        List <CollectionTagEntity> userCollTags = new ArrayList<>();

        log.info("user colls: " + userColls);
        for (CollectionEntity coll : userColls) {
            userCollTags.addAll(libCollTagRepository.findByCollectionid(coll.getCollectionid()));
            // coll에들어있는 모든 tagid들을 set으로 저장
        }
        log.info("user tags: " + userCollTags);

        Map<String, Long> tagFrequencyMap = new HashMap<>();
        for (CollectionTagEntity tag : userCollTags) {
            // 각 태그에 대해 TagEntity를 가져옵니다.
            TagEntity tagEntity = libTagRepository.findByTagid(tag.getTagid());
            if (tagEntity != null) {
                String tagName = tagEntity.getTagName();
                // 기존에 있는 태그라면 빈도수를 1 증가, 없다면 새로 추가
                tagFrequencyMap.put(tagName, tagFrequencyMap.getOrDefault(tagName, 0L) + 1);
            }
        }

        // 빈도수 높은 3개의 태그를 추출 (내림차순 정렬 후 최대 3개 선택)
        List<String> mostFrequentTags = tagFrequencyMap.entrySet().stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))  // 빈도수 내림차순 정렬
                .limit(3)  // 상위 3개 태그만 추출
                .map(Map.Entry::getKey)  // 태그 이름만 추출
                .collect(Collectors.toList());  // 리스트로 반환

        return UserCardView.builder()
                .loginId(loginId)
                .nickname(nickname)
                .profileImagePath(profileImagePath)
                .statusMessage(statusMessage)
                .relStatusWLoginUser(relStatusWLoginUser)
//                .userFreqTags()
                .build();
    }
}
/*
 // 컬렉션 정보 조회
        CollectionEntity collection = libCollectionRepository.findByCollectionid(collectionId);

        // Memory 정보 조회 (memory_order = 1인 첫 번째 메모리)
        MemoryEntity memory = libMemoryRepository.findByCollectionidAndMemoryOrder(collection.getId(), 1);
        String thumbnailPath = memory != null ? memory.getFilepath() : null;
        String thumbType = memory != null ? memory.getMemoryType() : null;
        String textContent = memory != null ? memory.getContent() : null;

        // 작성자 정보 조회
        Optional<UserEntity> author = libUserRepository.findByUserId(collection.getAuthorid());
        String authorName = author.isPresent() ? author.get().getName() : null;
        String authorProfileImage = author.get().getProfileImagePath();
        LikeEntity like;
        BookmarkEntity bookmark;

        // TODO: userid 가 없는 경우(비로그인시) 따로 처리
        if (userId!=null) {
            // 로그인 유저의 좋아요 및 북마크 여부 확인
            like = libLikeRepository.findByUseridAndCollectionid(userId, collection.getId());
            bookmark = libBookmarkRepository.findByUseridAndCollectionid(userId, collection.getId());
        }
        else{
            like=null;
            bookmark=null;
        }
        // 컬렉션의 좋아요 수, 북마크 수
        int likeCount = countLikesByCollectionId(collection.getId());
        int bookmarkCount = countBookmarksByCollectionId(collection.getId());

        // CollView 객체 생성 및 반환
        return CollView.builder()
                .collectionid(collection.getId())
                .authorid(collection.getAuthorid())
                .authorname(authorName)
                .collectionTitle(collection.getCollectionTitle())
                .readCount(collection.getReadCount())
                .visibility(collection.getVisibility())
                .createdDate(collection.getCreatedDate())
                .titleEmbedding(collection.getTitleEmbedding())
                .color(collection.getColor())
                .thumbnailPath(thumbnailPath)
                .textContent(textContent)
                .userlike(like != null)
                .userbookmark(bookmark != null)
                .authorProfileImage(authorProfileImage)
                .thumbType(thumbType)
                .likeCount(likeCount)
                .bookmarkCount(bookmarkCount)
                .build();
 */

