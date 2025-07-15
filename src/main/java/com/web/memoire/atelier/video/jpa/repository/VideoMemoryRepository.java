package com.web.memoire.atelier.video.jpa.repository;

import com.web.memoire.common.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface VideoMemoryRepository extends JpaRepository<MemoryEntity, String> {
    List<MemoryEntity> findByCollectionidOrderByCreatedDateDesc(int collectionId);

    Collection<Object> findByCollectionid(int collectionId);
}