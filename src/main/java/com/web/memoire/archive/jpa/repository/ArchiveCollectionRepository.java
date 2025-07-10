package com.web.memoire.archive.jpa.repository;

import com.web.memoire.common.entity.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchiveCollectionRepository extends JpaRepository<CollectionEntity, String> {

    // 유저의 컬렉션 전체 조회
    @Query(value = "SELECT c FROM CollectionEntity c where c.authorid = :userid")
    List<CollectionEntity> findAllUserCollections(@Param("userid") String userid);

    // 유저의 컬렉션 총 개수 조회
    @Query(value = "SELECT count(c) FROM CollectionEntity c where c.authorid = :userid")
    int countAllCollectionsByUserId(@Param("userid") String userid);

}
