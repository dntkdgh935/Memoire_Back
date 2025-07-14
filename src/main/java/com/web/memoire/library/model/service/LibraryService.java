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

    // ✅ public(visibility=1) collection 모두 불러오기
    public List<CollView> getAllPublicCollectionView(String userId) {
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
    public void addLike(String userid, String collectionId) {
        LikeEntity like = LikeEntity.builder()
                .userid(userid)
                .collectionid(collectionId)
                .likedDate(new Date())
                .build(); // likedDate는 자동으로 저장됨

        libLikeRepository.save(like);
    }
    @Transactional
    public void removeLike(String userid, String collectionId) {
        libLikeRepository.deleteByUseridAndCollectionid(userid, collectionId);
    }

    @Transactional
    public void addBM(String userid, String collectionId) {
        BookmarkEntity BM = BookmarkEntity.builder()
                .userid(userid)
                .collectionid(collectionId)
                .build();
        libBookmarkRepository.save(BM);
    }
    @Transactional
    public void removeBM(String userid, String collectionId) {
        libBookmarkRepository.deleteByUseridAndCollectionid(userid, collectionId );
    }

    public int countLikesByCollectionId(String collectionId){
        return libLikeRepository.countLikeEntitiesByCollectionid(collectionId);
    }
    public int countBookmarksByCollectionId(String collectionId){
        return libBookmarkRepository.countBookmarkEntitiesByCollectionid(collectionId);
    }

    public CollView getCollectionDetail(String collectionId, String userId) {
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



    public Object findByCollectionid(String collectionid) {
        CollectionEntity collection = libCollectionRepository.findByCollectionid(collectionid);
        return collection;
    }
    public Object getMemoriesByCollectionId(String collectionid) {
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
    public void toggleFollowRequest(String userid, String targetid) {
        RelationshipId id = new RelationshipId(userid, targetid);

        Optional<RelationshipEntity> optional = libRelationshipRepository.findById(id);
        log.info("✅ toggleFollowRequest: " + optional.isPresent());

        if (optional.isEmpty()) {
            // 관계 없음 → 요청 상태로 새로 추가
            RelationshipEntity newRelation = RelationshipEntity.builder()
                    .userid(userid)
                    .targetid(targetid)
                    .status("0") // 요청 상태
                    .followDate(new Date())
                    .build();
            libRelationshipRepository.save(newRelation);

        } else {
            RelationshipEntity relation = optional.get();
            String status = relation.getStatus();

            // 요청 상태이거나, 이미 팔로우 상태에서 클릭할 경우
            if ("1".equals(status) || "0".equals(status)) {
                // 팔로우 상태 → 삭제
                libRelationshipRepository.delete(relation);
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
}

