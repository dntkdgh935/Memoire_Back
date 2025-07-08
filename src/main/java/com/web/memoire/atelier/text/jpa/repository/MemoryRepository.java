package com.web.memoire.atelier.text.jpa.repository;

import com.web.memoire.atelier.text.jpa.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemoryRepository extends JpaRepository<MemoryEntity, Long> {

    // 특정 컬렉션에 속한 메모리 목록 조회
    List<MemoryEntity> findByCollectionId(String collectionId);

    // 특정 사용자의 메모리 목록 조회
    List<MemoryEntity> findByUserId(String userId);
}