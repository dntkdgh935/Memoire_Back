package com.web.memoire.archive.jpa.repository;

import com.web.memoire.common.entity.LikeEntity;
import com.web.memoire.common.entity.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchiveLikeRepository extends JpaRepository<LikeEntity, LikeId> {

    // 유저가 좋아요 누른 컬렉션 조회
    @Query(value = "SELECT l FROM LikeEntity l where l.userid = :userid")
    List<LikeEntity> findAllUserLikes(@Param("userid") String userid);

    // 컬렉션의 좋아요 수 조회
    @Query(value = "SELECT count(l) from LikeEntity l where l.collectionid = :collectionid")
    int countCollectionLikes(@Param("collectionid") int collectionid);

    // 컬렉션의 좋아요 상세 조회
    @Query(value = "SELECT l from LikeEntity l where l.collectionid = :collectionid")
    List<LikeEntity> findAllCollectionLikes(@Param("collectionid") int collectionid);

    // userid와 collectionid로 좋아요 조회
    @Query(value = "SELECT l FROM LikeEntity l WHERE l.userid = :userid AND l.collectionid = :collectionid")
    LikeEntity findLikeById(@Param("userid") String userid, @Param("collectionid") int collectionid);

}
