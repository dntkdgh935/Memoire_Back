package com.web.memoire.admin.jpa.repository;

import com.web.memoire.common.entity.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminCollectionRepository extends JpaRepository<CollectionEntity, Integer>, AdminCollectionRepositoryCustom {


}
