package com.web.memoire.atelier.text.jpa.repository;

import com.web.memoire.common.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemoryRepository extends JpaRepository<MemoryEntity, Integer> {

    // 🔍 특정 컬렉션에 속한 메모리 목록 조회
    List<MemoryEntity> findByCollectionid(int collectionid);

    // 🆔 MemoryEntity 전체의 최대 ID 조회 (필요하다면 유지)
    @Query("SELECT COALESCE(MAX(m.memoryid), 0) FROM MemoryEntity m")
    int findMaxMemoryId();

    // 📊 해당 컬렉션의 메모리 순서 중 최대값 조회 (새 메모리 order 계산용)
    @Query("SELECT COALESCE(MAX(m.memoryOrder), 0) FROM MemoryEntity m WHERE m.collectionid = :collectionId")
    Integer findMaxMemoryOrderByCollectionId(@Param("collectionId") int collectionId);
}