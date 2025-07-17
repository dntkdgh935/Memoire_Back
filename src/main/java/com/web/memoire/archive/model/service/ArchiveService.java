package com.web.memoire.archive.model.service;

import com.web.memoire.archive.jpa.repository.*;
import com.web.memoire.common.dto.*;
import com.web.memoire.common.entity.*;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final ArchiveBookmarkRepository archiveBookmarkRepository;
    @Autowired
    private final ArchiveCollectionRepository archiveCollectionRepository;
    @Autowired
    private final ArchiveLikeRepository archiveLikeRepository;
    @Autowired
    private final ArchiveMemoryRepository archiveMemoryRepository;
    @Autowired
    private final ArchiveRelationshipRepository archiveRelationshipRepository;
    @Autowired
    private OpenAIService openAIService;

    // UserRepository
    public User findUserById(String userid) {
        return userRepository.findById(userid)
                .map(UserEntity::toDto)
                .orElseThrow(() -> {
                    log.warn("사용자 조회 실패: userid '{}' 에 해당하는 사용자를 찾을 수 없습니다.", userid);
                    // 적절한 예외를 던집니다. 예를 들어, NoSuchElementException 또는 Custom Exception
                    return new NoSuchElementException("사용자를 찾을 수 없습니다.");
                });
    }

    @Transactional
    public int updateStatusMessage(String userid, String statusMessage) {
        User user = findUserById(userid);
        if (user == null) {
            return 0;
        }
        user.setStatusMessage(statusMessage);
        UserEntity updatedEntity = userRepository.save(user.toEntity());
        return updatedEntity != null ? 1 : 0;
    }

    // ArchiveBookmarkRepository
    public ArrayList<Bookmark> findAllUserBookmarks(String userid) {
        List<BookmarkEntity> entityList = archiveBookmarkRepository.findAllUserBookmarks(userid);
        ArrayList<Bookmark> list = new ArrayList<>();
        for (BookmarkEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public int countCollectionBookmarks(int collectionid) {
        return archiveBookmarkRepository.countCollectionBookmarks(collectionid);
    }

    public ArrayList<Bookmark> findAllCollectionBookmarks(int collectionid) {
        List<BookmarkEntity> entityList = archiveBookmarkRepository.findAllCollectionBookmarks(collectionid);
        ArrayList<Bookmark> list = new ArrayList<>();
        for (BookmarkEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    // ArchiveCollectionRepository
    public List<CollView> findAllUserCollections(String userid) {
        List<CollectionEntity> entityList = archiveCollectionRepository.findAllUserCollections(userid);
        return collectionArrayToCollViewArray(userid, (ArrayList<CollectionEntity>) entityList);
    }

    public int countAllCollectionsByUserId(String userid) {
        return archiveCollectionRepository.countAllCollectionsByUserId(userid);
    }

    public Collection findCollectionById(int collectionid) {
        return archiveCollectionRepository.findCollectionById(collectionid).toDto();
    }

    public CollView findCollViewByCollectionId(String userid, int collectionid) {
        CollectionEntity coll = archiveCollectionRepository.findCollectionById(collectionid);
        return collectionToCollView(userid, coll.getCollectionid());
    }

    @Transactional
    public int insertCollection(Collection collection) {
        CollectionEntity entity = archiveCollectionRepository.save(collection.toEntity());
        return entity != null ? entity.getCollectionid() : 0;
    }

    @Transactional
    public int deleteCollection(int collectionid) {
        try {
            archiveCollectionRepository.deleteById(collectionid);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // ArchiveCollectionTagRepository

    // ArchiveLikeRepository
    public ArrayList<Like> findAllUserLikes(String userid) {
        List<LikeEntity> entityList = archiveLikeRepository.findAllUserLikes(userid);
        ArrayList<Like> list = new ArrayList<>();
        for (LikeEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public int countCollectionLikes(int collectionid) {
        return archiveLikeRepository.countCollectionLikes(collectionid);
    }

    public ArrayList<Like> findAllCollectionLikes(int collectionid) {
        List<LikeEntity> entityList = archiveLikeRepository.findAllCollectionLikes(collectionid);
        ArrayList<Like> list = new ArrayList<>();
        for (LikeEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    // ArchiveMemoryRepository
    public ArrayList<Memory> findAllUserMemories(String userid, int collectionid) {
        List<MemoryEntity> entityList = archiveMemoryRepository.findAllUserMemories(userid, collectionid);
        ArrayList<Memory> list = new ArrayList<>();
        for (MemoryEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public int countAllMemoriesByUserId(String userid) {
        return archiveMemoryRepository.countAllMemoriesByUserId(userid);
    }

    @Transactional
    public int insertMemory(Memory memory) {
        MemoryEntity entity = archiveMemoryRepository.save(memory.toEntity());
        return entity != null ? entity.getMemoryid() : 0;
    }

    @Transactional
    public int deleteMemory(int memoryid) {
        try {
            archiveMemoryRepository.deleteById(memoryid);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // ArchiveRelationshipRepository
    public ArrayList<Relationship> findAllUserFollowing(String userid) {
        List<RelationshipEntity> entityList = archiveRelationshipRepository.findAllUserFollowing(userid);
        ArrayList<Relationship> list = new ArrayList<>();
        for (RelationshipEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public int countUserFollowing(String userid) {
        return archiveRelationshipRepository.countAllFollowingByUserId(userid);
    }

    public ArrayList<Relationship> findAllUserFollower(String userid) {
        List<RelationshipEntity> entityList = archiveRelationshipRepository.findAllUserFollower(userid);
        ArrayList<Relationship> list = new ArrayList<>();
        for (RelationshipEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public int countUserFollower(String userid) {
        return archiveRelationshipRepository.countAllFollowerByUserId(userid);
    }

    public ArrayList<Relationship> findAllUserRequestFollowing(String userid) {
        List<RelationshipEntity> entityList = archiveRelationshipRepository.findAllUserRequestFollowing(userid);
        ArrayList<Relationship> list = new ArrayList<>();
        for (RelationshipEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public ArrayList<Relationship> findAllUserRequestFollower(String userid) {
        List<RelationshipEntity> entityList = archiveRelationshipRepository.findAllUserRequestFollower(userid);
        ArrayList<Relationship> list = new ArrayList<>();
        for (RelationshipEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public ArrayList<Relationship> findAllUserBlock(String userid) {
        List<RelationshipEntity> entityList = archiveRelationshipRepository.findAllUserBlock(userid);
        ArrayList<Relationship> list = new ArrayList<>();
        for (RelationshipEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public Relationship findRelationshipById(String userid, String targetid) {
        return archiveRelationshipRepository.findRelationshipById(userid, targetid).toDto();
    }

    public Relationship findRelationshipByUserIdAndTargetId(String userid, String targetid) {
        return archiveRelationshipRepository.findRelationshipByTargetId(userid, targetid).toDto();
    }

    // ArchiveReportRepository

    // ArchiveTagRepository

    // 기타 메소드
    private List<CollView> collectionArrayToCollViewArray(String userid, ArrayList<CollectionEntity> collection) {
        return collection.stream().map(c -> {
            // ✅ memory_order = 1인 MemoryEntity 가져오기
            MemoryEntity memory = archiveMemoryRepository.findFirstMemoryByCollectionId(c.getCollectionid());
            String thumbnailPath = memory != null ? memory.getFilepath() : null;
            String thumbType = memory != null ? memory.getMemoryType() : null;
            String textContent = memory != null ? memory.getContent() : null;

            //✅ Author 정보 가져오기
            Optional<UserEntity> author = userRepository.findByUserId(c.getAuthorid());
            String authorName = author.isPresent() ? author.get().getName() : null;
            // 작성자 프로필 이미지 가져오기
            String authorProfileImage = userRepository.findByUserId(c.getAuthorid())
                    .map(UserEntity::getProfileImagePath)
                    .orElse("/default_profile.jpg");

            // ✅ 좋아요, 북마크 엔티티 가져오기
            LikeEntity like = archiveLikeRepository.findLikeById(userid, c.getCollectionid());
            BookmarkEntity bookmark = archiveBookmarkRepository.findBookmarkById(userid, c.getCollectionid());

            //collection의 총 좋아요 수/ 총 북마크 수 가져오기
            int likeCount = archiveLikeRepository.countCollectionLikes(c.getCollectionid());
            int bookmarkCount = archiveBookmarkRepository.countCollectionBookmarks(c.getCollectionid());
            log.info("✅북마크 수: " + bookmarkCount);

            return CollView.builder()
                    .collectionid(c.getCollectionid())
                    .authorid(c.getAuthorid())
                    .authorname(authorName)
                    .collectionTitle(c.getCollectionTitle())
                    .readCount(c.getReadCount())
                    .visibility(c.getVisibility())
                    .createdDate(c.getCreatedDate())
                    .titleEmbedding(c.getTitleEmbedding())
                    .color(c.getColor())
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

    private CollView collectionToCollView(String userid, int collectionid) {
        CollectionEntity collection = archiveCollectionRepository.findCollectionById(collectionid);
        //✅ memory_order = 1인 MemoryEntity 가져오기
        MemoryEntity memory = archiveMemoryRepository.findFirstMemoryByCollectionId(collection.getCollectionid());
        String thumbnailPath = memory != null ? memory.getFilepath() : null;
        String thumbType = memory != null ? memory.getMemoryType() : null;
        String textContent = memory != null ? memory.getContent() : null;

        //✅ Author 정보 가져오기
        Optional<UserEntity> author = userRepository.findByUserId(collection.getAuthorid());
        String authorName = author.isPresent() ? author.get().getName() : null;
        // 작성자 프로필 이미지 가져오기
        String authorProfileImage = userRepository.findByUserId(collection.getAuthorid())
                .map(UserEntity::getProfileImagePath)
                .orElse("/default_profile.jpg");

        //✅ 로그인 유저의 좋아요, 북마크 여부 가져오기
        LikeEntity like = archiveLikeRepository.findLikeById(userid, collection.getCollectionid());
        BookmarkEntity bookmark = archiveBookmarkRepository.findBookmarkById(userid, collection.getCollectionid());

        //collection의 총 좋아요 수/ 총 북마크 수 가져오기
        int likeCount = archiveLikeRepository.countCollectionLikes(collection.getCollectionid());
        int bookmarkCount = archiveBookmarkRepository.countCollectionBookmarks(collection.getCollectionid());
        log.info("✅북마크 수: " + bookmarkCount);

        return CollView.builder()
                .collectionid(collection.getCollectionid())
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


    // 예시: JSON 형식으로 임베딩 배열을 추출
    private static String extractEmbedding(String jsonResponse) throws JSONException {
        // JSONObject로 변환
        JSONObject jsonObject = new JSONObject(jsonResponse);

        // "data" 배열에서 첫 번째 객체 추출
        JSONArray data = jsonObject.getJSONArray("data");
        JSONObject firstItem = data.getJSONObject(0);

        // "embedding" 키가 존재하는지 확인 후, 존재하면 해당 값을 문자열로 변환하여 반환
        if (firstItem.has("embedding")) {
            JSONArray embeddingArray = firstItem.getJSONArray("embedding");
            return embeddingArray.toString();  // JSONArray를 문자열로 변환
        } else {
            throw new JSONException("\"embedding\" key not found");
        }
    }

    public String getEmbeddedTitle(@NotBlank String collectionTitle) {
        // OpenAIService를 호출하여 임베딩 값을 가져옴
        Mono<String> embeddingMono = openAIService.getEmbedding(collectionTitle);
        String embeddedTitle = embeddingMono.block();  // 임베딩 결과 동기적으로 받아옴
        return extractEmbedding(embeddedTitle);  // 임베딩 값에서 필요한 숫자만 추출
    }

    public Collection getCollectionById(@NotNull int collectionid) {
        return archiveCollectionRepository.findCollectionById(collectionid).toDto();
    }
}
