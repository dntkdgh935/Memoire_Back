package com.web.memoire.atelier.ImTIm.jpa.repository;

import com.web.memoire.common.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImTImMemoryRepository extends JpaRepository<MemoryEntity, Integer> {
    List<MemoryEntity> findByCollectionidOrderByCreatedDateDesc(int collectionId);

    @Query("""
      SELECT COALESCE(MAX(m.memoryOrder), 0)
      FROM MemoryEntity m
      WHERE m.collectionid = :collectionId
    """)
    Integer findMaxMemoryOrderByCollectionid(@Param("collectionId") int collectionId);
}
