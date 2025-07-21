package com.web.memoire.archive.jpa.repository;

import com.web.memoire.common.entity.CollectionTagEntity;
import com.web.memoire.common.entity.CollectionTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchiveCollectionTagRepository extends JpaRepository<CollectionTagEntity, CollectionTagId> {

    // 컬렉션에 포함된 태그 조회
    @Query(value = "SELECT t FROM CollectionTagEntity t WHERE t.collectionid = :collectionid")
    List<CollectionTagEntity> findCollectionTagByCollectionId(@Param("collectionid") int collectionid);

    void deleteByCollectionid(int collectionid);
}
