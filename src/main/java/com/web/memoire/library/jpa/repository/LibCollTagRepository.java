package com.web.memoire.library.jpa.repository;


import com.web.memoire.common.entity.CollectionTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibCollTagRepository extends JpaRepository<CollectionTagEntity, Integer>{
    List<CollectionTagEntity> findByCollectionid(int id);
}
