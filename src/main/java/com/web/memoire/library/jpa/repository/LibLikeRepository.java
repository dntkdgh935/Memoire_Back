package com.web.memoire.library.jpa.repository;

import com.web.memoire.common.entity.LikeEntity;
import com.web.memoire.common.entity.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibLikeRepository extends JpaRepository<LikeEntity, LikeId> {
    LikeEntity findByUseridAndCollectionid(String userId, String collectionId);
}
