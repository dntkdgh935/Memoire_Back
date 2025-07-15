package com.web.memoire.atelier.ImTIm.jpa.repository;

import com.web.memoire.common.entity.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImTImCollectionRepository extends JpaRepository<CollectionEntity, Integer> {

    List<CollectionEntity> findByAuthorid(String userId);
}
