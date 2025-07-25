package com.web.memoire.atelier.video.jpa.repository;

import com.web.memoire.common.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface VideoMemoryRepository extends JpaRepository<MemoryEntity, String> {
    List<MemoryEntity> findByCollectionidAndMemoryTypeOrderByCreatedDateDesc(int collectionId, String memoryType);

    @Query("""
      SELECT COALESCE(MAX(m.memoryOrder), 0)
      FROM MemoryEntity m
      WHERE m.collectionid = :collectionId
    """)
    Integer findMaxMemoryOrderByCollectionid(int collectionId);
}