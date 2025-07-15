package com.web.memoire.atelier.ImTIm.jpa.repository;

import com.web.memoire.common.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImTImMemoryRepository extends JpaRepository<MemoryEntity, Integer> {
    List<MemoryEntity> findByCollectionidOrderByCreatedDateDesc(int collectionId);

    int findMaxMemoryOrderByCollectionid(int collectionId);
}
