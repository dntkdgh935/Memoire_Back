package com.web.memoire.library.jpa.repository;

import com.web.memoire.common.entity.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LibCollectionRepository extends JpaRepository<CollectionEntity, String> {

    List<CollectionEntity> findByVisibility(int visibility);


    CollectionEntity findByCollectionid(String collectionid);
}