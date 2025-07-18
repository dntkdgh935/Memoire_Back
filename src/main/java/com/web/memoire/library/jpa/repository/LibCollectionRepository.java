package com.web.memoire.library.jpa.repository;

import com.web.memoire.common.entity.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibCollectionRepository extends JpaRepository<CollectionEntity, Integer> {

    List<CollectionEntity> findByVisibility(int visibility);

    CollectionEntity findByCollectionid(int collectionid);
    List<CollectionEntity> findByAuthoridOrderByCreatedDateDesc(String targetid);

    List<CollectionEntity> findByCollectionTitleContaining(String query);


    List<CollectionEntity> findByVisibilityIn(List<String> list);

    List<CollectionEntity> findByAuthorid(String ownerid);

    List<CollectionEntity> findByAuthoridAndVisibilityIn(String ownerid, List<String> list);
}