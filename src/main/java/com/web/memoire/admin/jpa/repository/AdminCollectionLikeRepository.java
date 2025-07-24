package com.web.memoire.admin.jpa.repository;

import com.web.memoire.common.entity.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface AdminCollectionLikeRepository extends JpaRepository<LikeEntity, Integer> {

    /**
     * 좋아요 수가 가장 많은 컬렉션의 ID와 좋아요 수를 조회합니다.
     * @param limit 조회할 컬렉션의 수
     * @return 각 컬렉션의 ID와 좋아요 수를 담은 Map 리스트
     */
    @Query(value = "SELECT new Map(l.collectionid as collectionId, count(l.collectionid) as likesCount) " +
            "FROM LikeEntity l " +
            "GROUP BY l.collectionid " +
            "ORDER BY likesCount DESC " +
            "LIMIT :limit") // JPQL에서는 LIMIT 문법을 사용합니다.
    List<Map<String, Object>> findTopLikedCollectionsByCount(@Param("limit") int limit);
}