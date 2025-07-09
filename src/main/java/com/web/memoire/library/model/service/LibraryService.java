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
import java.util.List;




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

            // ✅ 좋아요, 북마크 엔티티 가져오기
            LikeEntity like = libLikeRepository.findByUseridAndCollectionid(userId, collection.getId());
            BookmarkEntity bookmark = libBookmarkRepository.findByUseridAndCollectionid(userId, collection.getId());

            // ✅ 작성자 프로필 이미지 가져오기
            String authorProfileImage = libUserRepository.findByUserId(collection.getAuthorid())
                    .map(UserEntity::getProfileImagePath)
                    .orElse("/default_profile.jpg");

            return CollView.builder()
                    .collectionid(collection.getId())
                    .authorid(collection.getAuthorid())
                    .collectionTitle(collection.getCollectionTitle())
                    .readCount(collection.getReadCount())
                    .visibility(collection.getVisibility())
                    .createdDate(collection.getCreatedDate())
                    .titleEmbedding(collection.getTitleEmbedding())
                    .thumbnailPath(thumbnailPath)
                    .textContent("대표 이미지 설명 또는 메모리 내용") // 필요 시 추출
                    .userlike(like != null)
                    .userbookmark(bookmark != null)
                    .authorProfileImage(authorProfileImage)
                    .thumbType(thumbType)
                    .build();
        }).toList();
    }

}

