package com.web.memoire.atelier.jpa.repository;

import com.web.memoire.common.entity.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionRepository extends JpaRepository<CollectionEntity, Integer> {

    // ✅ 필드명 그대로 써야 함 (authorid!)
    List<CollectionEntity> findByAuthorid(String userId);
}