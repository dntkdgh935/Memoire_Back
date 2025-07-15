package com.web.memoire.library.jpa.repository;


import com.web.memoire.common.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibMemoryRepository extends JpaRepository<MemoryEntity, Integer> {


    MemoryEntity findByCollectionidAndMemoryOrder(int collectionId, int memoryOrder);

    List<MemoryEntity> findByCollectionid(int collectionid);

    MemoryEntity findByMemoryid(int i);
}