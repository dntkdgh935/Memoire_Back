package com.web.memoire.library.model.service;

import com.web.memoire.common.dto.*;
import com.web.memoire.common.entity.*;
import com.web.memoire.library.jpa.repository.*;
import com.web.memoire.user.jpa.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
    private final LibUserCollScoreRepository libUserCollScoreRepository;

    private final WebClient webClient;
    @Autowired
    private LibReportRepository libReportRepository;
    @Autowired
    private Like like;

    // 모든 태그 가져오기
    public List<Tag> getAllTags() {
        List<TagEntity> tagEntities = libTagRepository.findAll();
        return tagEntities.stream()
                .map(TagEntity::toDto)
                .toList();
    }

    // 상위 5개 태그 가져오기
    public List<Tag> getTopTags() {
        List<TagEntity> tagEntities = libTagRepository.findTop5BySearchCountPlusLikeCount();
        return tagEntities.stream()
                .map(TagEntity::toDto)
                .collect(Collectors.toList());
    }

    // 비로그인 유저에게 public Collection Return
    public Page<CollView> getAll4Anon(Pageable pageable) {
        // 공개된 컬렉션 리스트 가져오기
        List<CollectionEntity> publicCollections = libCollectionRepository.findByVisibility(1);

        // totalScore에 따라 정렬
        List<CollectionEntity> sortedColls = sortCollsByTotalScore(publicCollections);

        // CollView 리스트 생성
        List<CollView> collViews = new ArrayList<>();

        // 정렬된 CollectionEntity 리스트를 순차적으로 CollView로 변환
        for (CollectionEntity coll : sortedColls) {
            UserEntity author = libUserRepository.findByUserId(coll.getAuthorid()).isPresent() ?libUserRepository.findByUserId(coll.getAuthorid()).get():null;
            if (author == null){
                continue;
            }
            if (author.getRole().equals("BAD") || author.getRole().equals("EXIT")) {
                continue;
            }
            collViews.add(makeCollectionView(coll.getCollectionid(), null));
        }

        //페이징 처리
        int start = (int) pageable.getOffset(); // page * size
        int end = Math.min(start + pageable.getPageSize(), collViews.size());
        if (start > end) {
            return new PageImpl<>(List.of(), pageable, collViews.size());
        }
        List<CollView> pagedResult = collViews.subList(start, end);
        return new PageImpl<>(pagedResult, pageable, collViews.size());

    }

    // CollectionEntity 리스트를 totalScore 기준으로 정렬
    private List<CollectionEntity> sortCollsByTotalScore(List<CollectionEntity> collections) {
        // 정렬 작업
        collections.sort((coll1, coll2) -> {
            int totalScore1 = calculateTotalScore(coll1);
            int totalScore2 = calculateTotalScore(coll2);
            return Integer.compare(totalScore2, totalScore1); // 내림차순 정렬
        });
        return collections;
    }

    // CollectionEntity의 totalScore 계산
    public int calculateTotalScore(CollectionEntity coll) {
        int likeCount = libLikeRepository.countByCollectionid(coll.getCollectionid());
        int bookmarkCount = libBookmarkRepository.countByCollectionid(coll.getCollectionid());
        int readCount = coll.getReadCount();
        return likeCount + bookmarkCount + readCount;
    }


    // ✅ public(visibility=1) collection 모두 불러오기
    public Page<CollView> getAllColls4LoginUser(String userId, Pageable pageable) {
        List<CollView> collViews = new ArrayList<>();

        //대상 컬렉션: visibility 가 public이거나 follower 대상인 경우
        List<CollectionEntity> publicCollections = libCollectionRepository.findByVisibilityIn(Arrays.asList("1", "2"));
        List<CollectionEntity> filteredColls = new ArrayList<>();

        //filteredCollections 만듦
        for (CollectionEntity coll : publicCollections) {
            int collid = coll.getCollectionid();
            if (canUserAccessCollection(collid, userId)) {
                filteredColls.add(coll);
            }
        }
        List<CollectionEntity> sortedColls = sortCollsByTotalScore(filteredColls);
        for (CollectionEntity coll : sortedColls) {
            if (canUserAccessCollection(coll.getId(), userId)) {
                collViews.add(makeCollectionView(coll.getCollectionid(), userId));
            }
        }
        //페이징 처리
        int start = (int) pageable.getOffset(); // page * size
        int end = Math.min(start + pageable.getPageSize(), collViews.size());
        if (start > end) {
            return new PageImpl<>(List.of(), pageable, collViews.size());
        }
        List<CollView> pagedResult = collViews.subList(start, end);
        return new PageImpl<>(pagedResult, pageable, collViews.size());
    }

    @Transactional
    public void addLike(String userid, int collectionId) {
        //TB_Like에 적용
        LikeEntity like = LikeEntity.builder()
                .userid(userid)
                .collectionid(collectionId)
                .likedDate(new Date())
                .build(); // likedDate는 자동으로 저장됨

        libLikeRepository.save(like);

        log.info("좋아요 아이디: "+ userid);
        log.info("좋아요 컬렉션: "+ collectionId);

        //TB_TAG에 적용 (좋아요된 태그들에 적용)
        List<CollectionTagEntity> colltags = libCollTagRepository.findByCollectionid(collectionId);
        for (CollectionTagEntity colltag : colltags) {
            TagEntity tag = libTagRepository.findById(colltag.getTagid()).get();
            // 태그의 like_count에 +1 적용해 저장
            int currentLikeCount = tag.getLikeCount();
            tag.setLikeCount(currentLikeCount + 1);
            libTagRepository.save(tag);
        }

        // TB_USER_COLL_SCORES에 적용 (userid, collectionId에 해당하는 row 만들거나 수정 - interacted=1)
        // 존재하지 않으면 새로 만들어 저장
        // TODO: 컬렉션이나 유저 생성시 ... 이거 처리 로직 만들어야 함.
        UserCollScoreEntity userColl = libUserCollScoreRepository.findByUserAndCollection(userid, collectionId);
        if (userColl== null) {
            //없으면 새로 만듦
            UserCollScoreEntity userCollScoreEntity = UserCollScoreEntity.builder()
                    .userid(userid)
                    .collectionid(collectionId)
                    .interacted(1)
                    .recAt(new Date())
                    .seen(1)
                    .score(8) // 좋아요 했으므로 기본값 줄어듦
                    .build();
            libUserCollScoreRepository.save(userCollScoreEntity);
        }
        //존재하면 일부 수정
        else{
            userColl.setInteracted(1);
            userColl.setSeen(1);
            userColl.setScore(userColl.getScore()-2);
            if (userColl.getScore()<0) {
                userColl.setScore(10);
            }
        }

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

        // TB_USER_COLL_SCORES에 적용 (userid, collectionId에 해당하는 row 만들거나 수정 - interacted=1)
        // 존재하지 않으면 새로 만들어 저장
        // TODO: 컬렉션이나 유저 생성시 ... 이거 처리 로직 만들어야 함.
        UserCollScoreEntity userColl = libUserCollScoreRepository.findByUserAndCollection(userid, collectionId);
        if (userColl== null) {
            //없으면 새로 만듦
            UserCollScoreEntity userCollScoreEntity = UserCollScoreEntity.builder()
                    .userid(userid)
                    .collectionid(collectionId)
                    .interacted(1)
                    .recAt(new Date())
                    .seen(1)
                    .score(5) // 북마크 했으므로 기본값 줄어든 채로 초기화
                    .build();
            libUserCollScoreRepository.save(userCollScoreEntity);
        }
        //존재하면 일부 수정
        else{
            userColl.setInteracted(1);
            userColl.setSeen(1);
            userColl.setScore(userColl.getScore()-5);
            if (userColl.getScore()<0) {
                userColl.setScore(10);
            }
        }

    }

    //TODO: 컬렉션이나 태그 추천에 사용
    private int getTagBMCount(int tagid){

        // 1. tagid로 CollectionTagEntity 조회
        List<CollectionTagEntity> colltags = libCollTagRepository.findByTagid(tagid);

        // 2. collectionid 목록 추출 (중복 제거)
        Set<Integer> collectionIds = colltags.stream()
                .map(CollectionTagEntity::getCollectionid)
                .collect(Collectors.toSet());

        // 3. collectionid로 CollectionEntity 일괄 조회
        if (collectionIds.isEmpty()) {
            log.warn("No collections found for tagid: {}", tagid);
            return 0;
        }
        List<CollectionEntity> colls = libCollectionRepository.findByCollectionidIn(collectionIds);
        return colls.size();

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

    @Transactional
    public CollView getCollectionDetail(int collectionId, String userId) throws Exception {
        CollectionEntity collection = libCollectionRepository.findByCollectionid(collectionId);
        log.info("getCollectionDetail 서비스 작동중");
        // 컬렉션 count ++
        if (collection == null) {
            throw new IllegalArgumentException("컬렉션을 찾을 수 없습니다.");
        }
        collection.setReadCount(collection.getReadCount()+1);
        libCollectionRepository.save(collection);

        //컬렉션 seen - 1로 세팅
        log.info(userId);
        log.info(String.valueOf(userId.length()));
        log.info("접근 시도 유저:"+userId.getClass()); // 결과:

        UserCollScoreEntity userColl = libUserCollScoreRepository.findByUserAndCollection(userId, collectionId);
        if (userColl == null) {
            //없으면 새로 만듦
            UserCollScoreEntity userCollScoreEntity = UserCollScoreEntity.builder()
                    .userid(userId)
                    .collectionid(collectionId)
                    .recAt(new Date())
                    .seen(1)
                    .score(9) // 좋아요 했으므로 기본값 줄어듦
                    .build();
            libUserCollScoreRepository.save(userCollScoreEntity);
        }
        //존재하면 일부 수정
        else {
            userColl.setSeen(1);
            userColl.setScore(userColl.getScore() - 1);
            if (userColl.getScore() < 0) {
                userColl.setScore(10);
            }
        }
        if (canUserAccessCollection(collectionId, collection.getAuthorid())){
            return makeCollectionView(collectionId, userId);
        }
        else{
            throw new Exception("이 컬렉션에 접근할 권한이 없습니다."); // 접근 권한 없음
        }
    }


    public CollView getCollectionDetail4Anon(int collectionId) throws Exception {
        CollectionEntity collection = libCollectionRepository.findByCollectionid(collectionId);
        log.info("getCollectionDetail 서비스 작동중");
        if (collection.getVisibility() == 1) {
            log.info("공개 컬렉션 뷰 만들기");
            return makeCollectionView(collectionId, null);

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

        //요청한 유저로부터의 관계 검색
        Optional<RelationshipEntity> optional = libRelationshipRepository.findById(id);
        log.info("✅ toggleFollowRequest: " + optional.isPresent());

        if ("3".equals(nextRel)) {  // 관계 없음으로 설정하려면, 해당 관계 삭제
            libRelationshipRepository.deleteById(id);  // 관계 삭제
        }
        else {
            // 상태가 3이 아니면 관계가 존재하므로 상태 변경
            if (optional.isPresent()) {
                RelationshipEntity relationship = optional.get();
                relationship.setStatus(nextRel);  // 상태 변경
                libRelationshipRepository.save(relationship);  // 업데이트된 관계 저장

                // 요청한 관계가 "2"였다면, 반대 관계 삭제
                if ("2".equals(nextRel)) {  // 관계 없음으로 설정하려면, 해당 관계 삭제
                    RelationshipId oppoid = new RelationshipId(targetid, userid);
                    libRelationshipRepository.deleteById(oppoid);
                }
            } else {
                // 관계가 없을 경우 새로 관계를 추가 (상태 0: 요청됨으로)
                RelationshipEntity newRelationship = new RelationshipEntity();
                newRelationship.setUserid(userid);
                newRelationship.setTargetid(targetid);
                newRelationship.setStatus(nextRel);
                newRelationship.setFollowDate(new Date());
                libRelationshipRepository.save(newRelationship);  // 새 관계 추가

                // 요청한 관계가 "2"였다면, 반대 관계 삭제
                // 요청한 관계가 "2"였다면, 반대 관계 삭제
                if ("2".equals(nextRel)) {  // 관계 없음으로 설정하려면, 해당 관계 삭제
                    RelationshipId oppoid = new RelationshipId(targetid, userid);
                    libRelationshipRepository.deleteById(oppoid);
                }
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
    public Page<CollView> getFollowingColls4LoginUser(String userid, Pageable pageable) {
        // 1. 팔로잉중인 유저 목록 불러오기
        List<RelationshipEntity> followingRels = libRelationshipRepository.findAllUserFollowing(userid);

        // 2. 유저별로 컬렉션 불러와서 합치기
        List<CollectionEntity> allCollections = new ArrayList<>();
        for (RelationshipEntity followingRel : followingRels) {
            List<CollectionEntity> collections = libCollectionRepository.findByAuthoridOrderByCreatedDateDesc(followingRel.getTargetid());
            allCollections.addAll(collections);
        }
        List<CollView> collViews = new ArrayList<>();
        for (CollectionEntity coll : allCollections) {
            if (canUserAccessCollection(coll.getCollectionid(), userid)) {
                collViews.add(makeCollectionView(coll.getCollectionid(), userid));
            }
        }
        //페이징 처리
        int start = (int) pageable.getOffset(); // page * size
        int end = Math.min(start + pageable.getPageSize(), collViews.size());
        if (start > end) {
            return new PageImpl<>(List.of(), pageable, collViews.size());
        }
        List<CollView> pagedResult = collViews.subList(start, end);
        return new PageImpl<>(pagedResult, pageable, collViews.size());
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

        // 컬렉션에 달린 태그들
        List<String> collTags = new ArrayList<>();
        libCollTagRepository.findByCollectionid(collectionId).forEach(colltag -> {
            collTags.add(libTagRepository.findByTagid(colltag.getTagid()).getTagName());
        });
        log.info("컬렉션에 달린 태그: "+collTags);

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
                .collTags(collTags)
                .build();
    }

    public Object findVisibleOwnerCollections(String userid, String ownerid) throws AccessDeniedException {
        List<CollView> collViews = new ArrayList<>();
        log.info("서비스 - 로그인 유저: "+userid+"방문 대상:"+ownerid);
        Optional<RelationshipEntity> userToOtherRel = libRelationshipRepository.findByUseridAndTargetid(userid, ownerid);
        Optional<RelationshipEntity> otherToUserRel = libRelationshipRepository.findByUseridAndTargetid(ownerid, userid);

        String status1 = userToOtherRel.map(RelationshipEntity::getStatus).orElse(null);
        String status2 = otherToUserRel.map(RelationshipEntity::getStatus).orElse(null);

        if (status1 != null) {
            log.info("관계1: " + status1);
        }
        if (status2 != null) {
            log.info("관계2: " + status2);
        }

        // 차단 상태면 접근 불가
        if ("2".equals(status1) || "2".equals(status2)) {
            log.error("차단 관계가 있습니다.");
            throw new AccessDeniedException("You are blocked by this user.");
        }

        // 유저의 모든 컬렉션 가져옴
        List<CollectionEntity> userCollections = libCollectionRepository.findByAuthoridAndVisibilityIn(ownerid, Arrays.asList("1", "2"));

        for (CollectionEntity collection : userCollections) {
            if (canUserAccessCollection(collection.getCollectionid(), userid)) {
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
            if (canUserSeeUser(loginUserid, user.getUserId())) {
                userCardViews.add(userCardView);
            }
        }
        return userCardViews;
    }

    private boolean canUserSeeUser(String userId, String targetId){
        //둘 중 하나가 차단한 경우 볼 수 없음.
        Optional<RelationshipEntity> relationship1 = libRelationshipRepository.findByUseridAndTargetid(userId, targetId);
        Optional<RelationshipEntity> relationship2 = libRelationshipRepository.findByUseridAndTargetid(targetId, userId);

        // 차단 상태라면 접근 불가
        if ((relationship1.isPresent() && "2".equals(relationship1.get().getStatus())) ||
                (relationship2.isPresent() && "2".equals(relationship2.get().getStatus()))) {
            return false;
        }
        return true;
    }

    private UserCardView makeUserView(String targetId, String loginUserid) {
        UserEntity user = libUserRepository.findByUserId(targetId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        String userId = user.getUserId();
        String loginId;
        if (user.getLoginId() == null) {
            loginId = "소셜로그인";
        } else {
            loginId = user.getLoginId();
        }

        String nickname = user.getNickname();
        String profileImagePath= user.getProfileImagePath();
        String statusMessage=user.getStatusMessage();

        // 관계가 없으면 "3"을 리턴하도록 처리
        String relStatusWLoginUser = libRelationshipRepository.findByUseridAndTargetid(loginUserid, targetId)
                .map(RelationshipEntity::getStatus)  // 존재할 경우 상태 가져오기
                .orElse("3");

        List <CollectionEntity> userColls= libCollectionRepository.findByAuthoridAndVisibility(targetId,1);
        List <CollectionTagEntity> userCollTags = new ArrayList<>();

        log.info("user colls: " + userColls);
        for (CollectionEntity coll : userColls) {
            userCollTags.addAll(libCollTagRepository.findByCollectionid(coll.getCollectionid()));
            // coll에들어있는 모든 tagid들을 set으로 저장
        }
        log.info("user tags: " + userCollTags);

        Map<String, Long> tagFrequencyMap = new HashMap<>();
        for (CollectionTagEntity colltag : userCollTags) {
            // 각 태그에 대해 TagEntity를 가져옵니다.
            log.info("태그: " + Integer.toString(colltag.getTagid()));
            TagEntity tagEntity = libTagRepository.findByTagid(colltag.getTagid());
            log.info("태그 엔티티: " + tagEntity);
            if (tagEntity != null) {
                String tagName = tagEntity.getTagName();
                log.info("태그이름: " + tagName);
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
                .userId(userId)
                .loginId(loginId)
                .nickname(nickname)
                .profileImagePath(profileImagePath)
                .statusMessage(statusMessage)
                .relStatusWLoginUser(relStatusWLoginUser)
                .userFreqTags(mostFrequentTags)
                .build();
    }

    public Object getUserTopTags(String userid) {
        List <CollectionEntity> userColls= libCollectionRepository.findByAuthoridAndVisibility(userid,1);
        List <CollectionTagEntity> userCollTags = new ArrayList<>();

        log.info("user colls: " + userColls);
        for (CollectionEntity coll : userColls) {
            userCollTags.addAll(libCollTagRepository.findByCollectionid(coll.getCollectionid()));
            // coll에들어있는 모든 tagid들을 set으로 저장
        }
        log.info("user tags: " + userCollTags);

        Map<String, Long> tagFrequencyMap = new HashMap<>();
        for (CollectionTagEntity colltag : userCollTags) {
            // 각 태그에 대해 TagEntity를 가져옵니다.
            log.info("태그: " + Integer.toString(colltag.getTagid()));
            TagEntity tagEntity = libTagRepository.findByTagid(colltag.getTagid());
            log.info("태그 엔티티: " + tagEntity);
            if (tagEntity != null) {
                String tagName = tagEntity.getTagName();
                log.info("태그이름: " + tagName);
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
        return mostFrequentTags;

    }

    // List<String>으로 바꿔야 함
    public List<CollView> searchColl(String query, String loginUserid) {
        String fastApiUrl = "http://localhost:8000/library/search";

        // TODO: 비공개/ 유저에 따른 처리 추가

        //1. 쿼리에 대한 응답 리스트 fastAPI에 받아오기
        Mono<List<Integer>> responseMono = webClient.post()
                .uri(fastApiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("query", query))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Integer>>() {});
        List<Integer> orderedIds = responseMono.block();  // 예: [5, 3, 9]
        if (orderedIds == null || orderedIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 해당 ID로 모든 컬렉션 조회
        List<CollectionEntity> collections = libCollectionRepository.findAllById(orderedIds);


        // 3. Map<Integer, CollectionEntity> 으로 수동 변환
        Map<Integer, CollView> collViewMap = new HashMap<>();
        for (CollectionEntity coll : collections) {
            //유저가 접근 가능한 것들만 결과에 붙임
            if (canUserAccessCollection(coll.getCollectionid(), loginUserid)){
                collViewMap.put(coll.getId(), makeCollectionView(coll.getId(), loginUserid));
            }
        }

        // 4. 순서를 유지하여 List<CollectionEntity> 구성
        List<CollView> result = new ArrayList<>();
        for (Integer id : orderedIds) {
            if (collViewMap.containsKey(id)) {
                result.add(collViewMap.get(id));
                log.info(collViewMap.get(id).getCollectionTitle() + " 북마크 됨? "+ collViewMap.get(id).isUserbookmark());
            }
        }
        return result;
    }

    public List<String> fetchCollectionIdsByQuery(String query) {
        String fastApiUrl = "http://localhost:8000/library/search";

        Mono<List<String>> responseMono = webClient.post()
                .uri(fastApiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("query", query))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {
                });

        // 블로킹 방식으로 결과 반환
        return responseMono.block();
    }

    public List<CollView> findCollViewsWithTag(String query, String userid) {
        //int tagid = libTagRepository.findByTagName(query).getTagid(); // 검색된 태그의 아이디 찾기
        Optional<TagEntity> tagOpt = Optional.ofNullable(libTagRepository.findByTagName(query));
        if (tagOpt.isEmpty()) {
            log.warn("No tag found for tagname: {}", query);
            return Collections.emptyList();
        }
        int tagid = tagOpt.get().getTagid();

        List <CollectionTagEntity> colltags = libCollTagRepository.findByTagid(tagid); // 태그가 달린 컬렉션들

        List<CollView> collViews = new ArrayList<>();

        for (CollectionTagEntity colltag : colltags) {
            CollView cv = makeCollectionView(colltag.getCollectionid(), userid);
            collViews.add(cv);
        }
        return collViews;
    }

    private List<CollectionEntity> findCollsWithTag(String query) {
        //int tagid = libTagRepository.findByTagName(query).getTagid(); // 검색된 태그의 아이디 찾기
        Optional<TagEntity> tagOpt = Optional.ofNullable(libTagRepository.findByTagName(query));
        if (tagOpt.isEmpty()) {
            log.warn("No tag found for tagname: {}", query);
            return Collections.emptyList();
        }
        int tagid = tagOpt.get().getTagid();

        List <CollectionTagEntity> colltags = libCollTagRepository.findByTagid(tagid); // 태그가 달린 컬렉션들

//        List<CollView> collViews = new ArrayList<>();
        List <CollectionEntity> colls = new ArrayList<>();
        for (CollectionTagEntity colltag : colltags) {
            colls.add(libCollectionRepository.findByCollectionid(colltag.getCollectionid()));
        }
        return colls;
    }

    @Transactional
    public void addTagSearchCount(String tagname) {
        log.info("검색된 태그:"+tagname);

        // tagname이 null이거나 빈 문자열이면 아무것도 하지 않음
        if (tagname == null || tagname.trim().isEmpty()) {
            log.warn("Invalid tagname: {}", tagname);
            return;
        }

        // tagname에 해당하는 TagEntity 조회
        Optional<TagEntity> tagEntityOpt = Optional.ofNullable(libTagRepository.findByTagName(tagname.trim()));
        if (tagEntityOpt.isEmpty()) {
            log.warn("No TagEntity found for tagname: {}", tagname);
            return;
        }

        // TagEntity의 검색 횟수 증가
        TagEntity tagEntity = tagEntityOpt.get();
        log.info("이전에 검색된 수: {}", tagEntity.getSearchCount());
        tagEntity.setSearchCount(tagEntity.getSearchCount() + 1);
        libTagRepository.save(tagEntity); // 변경 사항 저장
        log.info("수정 후: {}", tagEntity.getSearchCount());


    }

    public Object getTopicColls4LoginUser(String userId, String selectedTag, Pageable pageable) {
        List<CollectionEntity> colls= findCollsWithTag(selectedTag);
        log.info(selectedTag+ "태그 달린 컬렉션 개수: "+colls.size());
        log.info(colls.toString());

        List <CollView> collViews = new ArrayList<>();

        //접근권한에 따라 리턴해야 함 => 가능한 것만 filteredColls로 만듦
        List <CollectionEntity> filteredColls = new ArrayList<>();
        for (CollectionEntity coll : colls) {
            int collid = coll.getCollectionid();
            if (canUserAccessCollection(collid, userId)) {
                filteredColls.add(coll);
            }
        }
        // sort
        List<CollectionEntity> sortedColls = sortCollsByTotalScore(filteredColls);
        for (CollectionEntity coll : sortedColls) {
            if (canUserAccessCollection(coll.getId(), userId)) {
                collViews.add(makeCollectionView(coll.getCollectionid(), userId));
            }
        }
        //페이징 처리
        int start = (int) pageable.getOffset(); // page * size
        int end = Math.min(start + pageable.getPageSize(), collViews.size());
        if (start > end) {
            return new PageImpl<>(List.of(), pageable, collViews.size());
        }
        List<CollView> pagedResult = collViews.subList(start, end);
        return new PageImpl<>(pagedResult, pageable, collViews.size());

    }

    public Page<CollView>  getTopicColls4Anon(String selectedTag, Pageable pageable) {
        List<CollectionEntity> colls= findCollsWithTag(selectedTag);
        List <CollView> collViews = new ArrayList<>();

        List <CollectionEntity> filteredColls = new ArrayList<>();
        for (CollectionEntity coll : colls) {
            if (coll.getVisibility() == 1) {
                log.info("공개 컬렉션 뷰 만들기");
                filteredColls.add(coll);
            }
            // 그 외의 경우 접근 불가
            else {
                log.info("접근 불가 컬렉션");
            }
        }
        List<CollectionEntity> sortedColls = sortCollsByTotalScore(filteredColls);
        for (CollectionEntity coll : sortedColls) {
            collViews.add(makeCollectionView(coll.getCollectionid(), null));
        }
        //return collViews;

        //페이징 처리
        int start = (int) pageable.getOffset(); // page * size
        int end = Math.min(start + pageable.getPageSize(), collViews.size());
        if (start > end) {
            return new PageImpl<>(List.of(), pageable, collViews.size());
        }
        List<CollView> pagedResult = collViews.subList(start, end);
        return new PageImpl<>(pagedResult, pageable, collViews.size());
    }


    /*
    interaction score 높은 순+ 같은 순위면 컬렉션이 받은 좋아요+북마크(readCount) 개수순으로 추천
    30(topN)개씩 무한번 요청해 아래에 붙일 수 있음
    단 프론트가 너무 무거워지면 페이지 새로고침하기? 또는 다른 전략?
    * */
    public Page<CollView> getRecPage4LoginUser(String userid, Pageable pageable) {
        List <CollView> recColls = new ArrayList<>();

        // 유저와 상호작용한 이력이 있는 컬렉션 불러오기
        // List<UserCollScores> userCollScore: userid에 해당하는 TB_USER_COLL_SCORES를 score이 큰 순서로 불러옴 (추천은 나중에)
        // userCollScore를 돌면서 userCollScore.getCollectionid()에 해당하는 likeEntity 개수 + bookmarkentity 개수 카운트 + coll.getReadCount()를 구함
        // 구한 합계값 순위대로 interactedColls에 add함.
        List<UserCollScoreEntity> userScores = libUserCollScoreRepository.findByUseridOrderByScoreDesc(userid); // 사용자 상호작용 점수 내림차순
        List <CollView> interactedColls = userScores.stream()
                .map(score -> {
                    CollectionEntity coll = libCollectionRepository.findByCollectionid(score.getCollectionid());

                    if (coll == null) return null;

                    int likeCount = libLikeRepository.countByCollectionid(coll.getCollectionid());
                    int bookmarkCount = libBookmarkRepository.countByCollectionid(coll.getCollectionid());
                    int readCount = coll.getReadCount();
                    int totalScore = likeCount + bookmarkCount + readCount;

                    CollView view = makeCollectionView(coll.getCollectionid(), userid);
                    return view == null ? null : new AbstractMap.SimpleEntry<>(view, totalScore); // 💡 CollView와 점수 한 쌍
                })
                .filter(Objects::nonNull)
                .filter(entry -> canUserAccessCollection(entry.getKey().getCollectionid(), userid))
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue())) // 💡 totalScore 기준 내림차순
                .map(Map.Entry::getKey) // 💡 CollView만 추출
                .toList();

        // 유저와 상호작용한 이력이 "없는" 컬렉션 불러오기
        // List<CollectionEntity> pureColls: userid에 해당하는 TB_USER_COLL_SCORES가 없는 컬렉션 불러오기
        // coll 하나씩 돌면서 coll.collectionid를 가진 likeEntity 개수 + bookmarkentity 개수 카운트 + coll.getReadCount()를 구함
        // 구한 합계값 순위대로 RecColl에 add함.
        List<CollectionEntity> allCollections = libCollectionRepository.findAll(); // 공개된 컬렉션 전체

        Set<Integer> interactedIds = userScores.stream()
                .map(UserCollScoreEntity::getCollectionid)
                .collect(Collectors.toSet());

        List <CollView> pureColls = allCollections.stream()
                .filter(coll -> !interactedIds.contains(coll.getCollectionid()))
                .map(coll -> {
                    int likeCount = libLikeRepository.countByCollectionid(coll.getCollectionid());
                    int bookmarkCount = libBookmarkRepository.countByCollectionid(coll.getCollectionid());
                    int readCount = coll.getReadCount();

                    int totalScore = likeCount + bookmarkCount + readCount;
                    CollView view = makeCollectionView(coll.getCollectionid(), userid);
                    return view == null ? null : new AbstractMap.SimpleEntry<>(view, totalScore);
                })
                .filter(entry -> canUserAccessCollection(entry.getKey().getCollectionid(), userid))
                .filter(Objects::nonNull)
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue())) // 💡 totalScore 기준 내림차순
                .map(Map.Entry::getKey) // 💡 CollView만 추출
                .toList();

        // 상호작용 이력 없는  PureColl먼저 추천하도록 결과값 만듦
        recColls.addAll(pureColls);
        recColls.addAll(interactedColls);

        // 4. 페이징 처리
        int start = (int) pageable.getOffset(); // page * size
        int end = Math.min(start + pageable.getPageSize(), recColls.size());

        if (start > end) {
            return new PageImpl<>(List.of(), pageable, recColls.size());
        }

        List<CollView> pagedResult = recColls.subList(start, end);
        return new PageImpl<>(pagedResult, pageable, recColls.size());
    }

    @Transactional
    public void reportMemory(int memoryid, String userid, String reportReason) {
        //libReport
        ReportEntity reportEntity = new ReportEntity(memoryid, userid, reportReason);
        libReportRepository.saveAndFlush(reportEntity);
    }

    public Object getWhoLiked(int collectionid, String loginUser) {
        List <UserCardView> usersWhoLiked = new ArrayList<>();

        List <LikeEntity> collLikes = libLikeRepository.findByCollectionid(collectionid);
        for (LikeEntity like : collLikes) {
            UserCardView likedUser = makeUserView(like.getUserid(), loginUser);
            usersWhoLiked.add(likedUser);
        }
        return usersWhoLiked;
    }

    public Object getWhoBookmarked(int collectionid, String loginUser) {
        List <UserCardView> usersWhoBookmarked = new ArrayList<>();

        List <BookmarkEntity> collBMs =  libBookmarkRepository.findByCollectionid(collectionid);
        for (BookmarkEntity bm : collBMs) {
            UserCardView bmUser = makeUserView(bm.getUserid(), loginUser);
            usersWhoBookmarked.add(bmUser);
        }
        return usersWhoBookmarked;
    }

    public boolean canUserAccessCollection(int collectionId, String userId) {
        CollectionEntity collection = libCollectionRepository.findByCollectionid(collectionId);
        Optional<UserEntity> user = libUserRepository.findByUserId(userId);
        Optional<UserEntity> author = libUserRepository.findByUserId(collection.getAuthorid());
        if (collection == null) {
            return false; // 컬렉션이 존재하지 않음
        }
        //차단된 유저는 접근 불가
        if (user.get().getRole().equals("BAD") || user.get().getRole().equals("EXIT") ) {
            return false;
        }
        //차단된 유저의 컬렉션은 접근 불가
        if (author.get().getRole().equals("BAD") || author.get().getRole().equals("EXIT") ) {
            return false;
        }

        // 작성자 본인은 무조건 접근 가능
        if (userId.equals(collection.getAuthorid())) {
            return true;
        }

        // 공개(1)일 경우
        if (collection.getVisibility() == 1) {
            Optional<RelationshipEntity> relationship1 = libRelationshipRepository.findByUseridAndTargetid(userId, collection.getAuthorid());
            Optional<RelationshipEntity> relationship2 = libRelationshipRepository.findByUseridAndTargetid(collection.getAuthorid(), userId);

            // 차단 상태라면 접근 불가
            String status1 = relationship1.map(RelationshipEntity::getStatus).orElse(null);
            String status2 = relationship2.map(RelationshipEntity::getStatus).orElse(null);
            if ("2".equals(status1) || "2".equals(status2)) {
                return false;
            }
            // 차단이 아니라면 접근 가능
            return true;
        }

        // 팔로워만(2)일 경우
        if (collection.getVisibility() == 2) {
            Optional<RelationshipEntity> relationship = libRelationshipRepository.findByUseridAndTargetid(userId, collection.getAuthorid());
            String status = relationship.map(RelationshipEntity::getStatus).orElse(null);
            return "1".equals(status); // 팔로우 상태일 때만 true
        }

        // 작성자만(3)인 경우는 이미 본인 여부 확인됨
        return false; // 그 외는 접근 불가
    }

}