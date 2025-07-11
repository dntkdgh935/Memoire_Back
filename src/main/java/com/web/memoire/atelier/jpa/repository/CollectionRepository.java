package com.web.memoire.atelier.jpa.repository;

import com.web.memoire.common.entity.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<CollectionEntity, Integer> {
}