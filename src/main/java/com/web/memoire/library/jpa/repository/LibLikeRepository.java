package com.web.memoire.library.jpa.repository;

import com.web.memoire.common.entity.LikeEntity;
import com.web.memoire.common.entity.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LibLikeRepository extends JpaRepository<LikeEntity, LikeId> {
    LikeEntity findByUseridAndCollectionid(String userId, int collectionId);

    void deleteByUseridAndCollectionid(String userid, int collectionId);

    int countLikeEntitiesByCollectionid(int collectionId);

    int countByCollectionid(int collectionid);

    List<LikeEntity> findByCollectionid(int collectionid);

    Collection<Object> findByUserid(String userid);
}
