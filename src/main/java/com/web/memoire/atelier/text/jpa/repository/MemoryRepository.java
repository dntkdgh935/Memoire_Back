package com.web.memoire.atelier.text.jpa.repository;

import com.web.memoire.common.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemoryRepository extends JpaRepository<MemoryEntity, Integer> {

    // 🔍 특정 컬렉션에 속한 메모리 목록 조회
    List<MemoryEntity> findByCollectionid(String collectionid);

    // 🆔 MEMORYID의 최대값 조회 → 신규 메모리 ID 수동 설정용
    @Query("SELECT COALESCE(MAX(m.memoryid), 0) FROM MemoryEntity m")
    int findMaxMemoryId();
}