package com.web.memoire.library.jpa.repository;


import com.web.memoire.common.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LibMemoryRepository extends JpaRepository<MemoryEntity, Integer> {


    MemoryEntity findByCollectionidAndMemoryOrder(String collectionId, int memoryOrder);

    List<MemoryEntity> findByCollectionid(String collectionid);

    MemoryEntity findByMemoryid(int i);
}