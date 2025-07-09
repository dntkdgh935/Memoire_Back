package com.web.memoire.atelier.text.jpa.repository;

import com.web.memoire.atelier.text.jpa.entity.AtelierMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemoryRepository extends JpaRepository<AtelierMemoryEntity, Long> {

    // 특정 컬렉션에 속한 메모리 목록 조회
    List<AtelierMemoryEntity> findByCollectionId(String collectionId);

    // 특정 사용자의 메모리 목록 조회 (메모리에는 user id없어서 불가. collection 통해서 가져와야 함)
    //List<AtelierMemoryEntity> findByUserId(String userId);
}