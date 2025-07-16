package com.web.memoire.library.model.service;

import com.web.memoire.common.dto.CollView;
import com.web.memoire.common.dto.Tag;
import com.web.memoire.common.entity.*;
import com.web.memoire.library.jpa.repository.*;
import com.web.memoire.user.jpa.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;


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

    // 비로그인 유저 대상 =====================================================================
    // ✅ 모든 태그 가져오기
    public List<Tag> getAllTags() {
        List<TagEntity> tagEntities = libTagRepository.findAll();
        return tagEntities.stream()
                .map(TagEntity::toDto)
                .toList();
    }

    // ✅ 상위 5개 태그 가져오기
    public List<Tag> getTopTags() {
        List<TagEntity> tagEntities = libTagRepository.findTop5TagsByRownum();
        return tagEntities.stream()
                .map(TagEntity::toDto)
                .toList();
    }

    // 비로그인 유저에게 public Collection Return
    public List<CollView> getAllPublicCollectionView() {
        List<CollectionEntity> publicCollections = libCollectionRepository.findByVisibility(1);

        return publicCollections.stream().map(collection -> {
            // ✅ memory_order = 1인 MemoryEntity 가져오기
            MemoryEntity memory = libMemoryRepository.findByCollectionidAndMemoryOrder(collection.getId(), 1);
            String thumbnailPath = memory != null ? memory.getFilepath() : null;
            String thumbType = memory != null ? memory.getMemoryType() : null;
            String textContent = memory != null ? memory.getContent() : null;

            //✅ Author 정보 가져오기
            Optional<UserEntity> author = libUserRepository.findByUserId(collection.getAuthorid());
            String authorName = author.isPresent() ? author.get().getName() : null;
            // 작성자 프로필 이미지 가져오기
            String authorProfileImage = libUserRepository.findByUserId(collection.getAuthorid())
                    .map(UserEntity::getProfileImagePath)
                    .orElse("/default_profile.jpg");

            // 비로그닝 유저는 like/ bookmark 정보 없음
            LikeEntity like = null;
            BookmarkEntity bookmark = null;

            //collection의 총 좋아요 수/ 총 북마크 수 가져오기
            int likeCount = countLikesByCollectionId(collection.getId());
            int bookmarkCount = countBookmarksByCollectionId(collection.getId());
            log.info("✅북마크 수: " + bookmarkCount);

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
                    .textContent(textContent) // 필요 시 추출
                    .userlike(like != null)
                    .userbookmark(bookmark != null)
                    .authorProfileImage(authorProfileImage)
                    .thumbType(thumbType)
                    .likeCount(likeCount)
                    .bookmarkCount(bookmarkCount)
                    .build();
        }).toList();
    }


    // ✅ public(visibility=1) collection 모두 불러오기
    public List<CollView> getAllColls4LoginUser(String userId) {
        List<CollectionEntity> publicCollections = libCollectionRepository.findByVisibility(1);

        return publicCollections.stream().map(collection -> {
            // ✅ memory_order = 1인 MemoryEntity 가져오기
            MemoryEntity memory = libMemoryRepository.findByCollectionidAndMemoryOrder(collection.getId(), 1);
            String thumbnailPath = memory != null ? memory.getFilepath() : null;
            String thumbType = memory != null ? memory.getMemoryType() : null;
            String textContent = memory != null ? memory.getContent() : null;

            //✅ Author 정보 가져오기
            Optional<UserEntity> author = libUserRepository.findByUserId(collection.getAuthorid());
            String authorName = author.isPresent() ? author.get().getName() : null;
            // 작성자 프로필 이미지 가져오기
            String authorProfileImage = libUserRepository.findByUserId(collection.getAuthorid())
                    .map(UserEntity::getProfileImagePath)
                    .orElse("/default_profile.jpg");

            // ✅ 좋아요, 북마크 엔티티 가져오기
            LikeEntity like = libLikeRepository.findByUseridAndCollectionid(userId, collection.getId());
            BookmarkEntity bookmark = libBookmarkRepository.findByUseridAndCollectionid(userId, collection.getId());

            //collection의 총 좋아요 수/ 총 북마크 수 가져오기
            int likeCount = countLikesByCollectionId(collection.getId());
            int bookmarkCount = countBookmarksByCollectionId(collection.getId());
            log.info("✅북마크 수: " + bookmarkCount);

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
                    .textContent(textContent) // 필요 시 추출
                    .userlike(like != null)
                    .userbookmark(bookmark != null)
                    .authorProfileImage(authorProfileImage)
                    .thumbType(thumbType)
                    .likeCount(likeCount)
                    .bookmarkCount(bookmarkCount)
                    .build();
        }).toList();
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

    public CollView getCollectionDetail(int collectionId, String userId) {
        CollectionEntity collection = libCollectionRepository.findByCollectionid(collectionId);

        //✅ memory_order = 1인 MemoryEntity 가져오기
        MemoryEntity memory = libMemoryRepository.findByCollectionidAndMemoryOrder(collection.getId(), 1);
        String thumbnailPath = memory != null ? memory.getFilepath() : null;
        String thumbType = memory != null ? memory.getMemoryType() : null;
        String textContent = memory != null ? memory.getContent() : null;

        //✅ Author 정보 가져오기
        Optional<UserEntity> author = libUserRepository.findByUserId(collection.getAuthorid());
        String authorName = author.isPresent() ? author.get().getName() : null;
        // 작성자 프로필 이미지 가져오기
        String authorProfileImage = libUserRepository.findByUserId(collection.getAuthorid())
                .map(UserEntity::getProfileImagePath)
                .orElse("/default_profile.jpg");

        //✅ 로그인 유저의 좋아요, 북마크 여부 가져오기
        LikeEntity like = libLikeRepository.findByUseridAndCollectionid(userId, collection.getId());
        BookmarkEntity bookmark = libBookmarkRepository.findByUseridAndCollectionid(userId, collection.getId());

        //collection의 총 좋아요 수/ 총 북마크 수 가져오기
        int likeCount = countLikesByCollectionId(collection.getId());
        int bookmarkCount = countBookmarksByCollectionId(collection.getId());
        log.info("✅북마크 수: " + bookmarkCount);

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
                .textContent(textContent) // 필요 시 추출
                .userlike(like != null)
                .userbookmark(bookmark != null)
                .authorProfileImage(authorProfileImage)
                .thumbType(thumbType)
                .likeCount(likeCount)
                .bookmarkCount(bookmarkCount)
                .build();
    }



    public Object findByCollectionid(int collectionid) {

        CollectionEntity collection = libCollectionRepository.findByCollectionid(collectionid);
        return collection;
    }
    public Object getMemoriesByCollectionId(int collectionid) {
        List <MemoryEntity> memories =  libMemoryRepository.findByCollectionid(collectionid);
        return memories;
    }

    public Object getMemoryDetail(int memoryid) {
        MemoryEntity entity = libMemoryRepository.findByMemoryid(memoryid);
        log.info(entity.toString());
        return entity.toDto();
    }
//    //TB_RELATIONSHIP : userid, targetid간의 관계를 확인하는 요청
//    public Object getRelationshipInfo(String userid, String targetid) {
//        RelationshipId id = new RelationshipId(userid, targetid);
//        Optional<RelationshipEntity> optional = libRelationshipRepository.findById(id);
//        return optional.toDto();
//    }


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


    // TODO: 클릭한 토픽에 대한 추천 (외부 시스템 가져올시 삭제)
//    public Object getTopicColls4LoginUser(String userid, String selectedTag) {
//        // 1. user 대상 추천 가능한 Collection 리스트 불러오기 ()
//        //      + 공개범위가 public인 컬렉션 중
//        //      -       relationship(2)인 targetid가 authorid인 컬렉션 제외
//        //      + 공개범위가 follower인 컬렉션 중
//        //              authorid가 targetid이고, viewer의 id인데, relatinoship status가 1인 경우 추가
//        // 2. TB_COLLECTION 중 TB_TAG_TITLE_SIMILARITY에 selectedTag에 대한 유사도 점수가 있는 컬렉션들 뽑아 리스트로 만들고 정렬
//        // 3. TB_COLLECTION 중 유사도 정보가 없는 컬렉션들을 껴넣음
//        //   (정보 있는 컬렉션 10개마다 껴넣기(남은 개수가 10개도 없으면, 그만큼 넣고 마지막에 정보 없는 컬렉션을 붙이기)
//        // 4. 유저 인터랙션 정보에 따라 재정렬
//        // 5. 리턴
//    }


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
        String authorProfileImage = libUserRepository.findByUserId(collection.getAuthorid())
                .map(UserEntity::getProfileImagePath)
                .orElse("/default_profile.jpg");

        // 로그인 유저의 좋아요 및 북마크 여부 확인
        LikeEntity like = libLikeRepository.findByUseridAndCollectionid(userId, collection.getId());
        BookmarkEntity bookmark = libBookmarkRepository.findByUseridAndCollectionid(userId, collection.getId());

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

}

