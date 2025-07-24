package com.web.memoire.admin.jpa.repository;

import com.web.memoire.common.entity.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminCollectionRepository extends JpaRepository<CollectionEntity, Integer>, AdminCollectionRepositoryCustom {

    // 컬렉션 조회
    CollectionEntity findByCollectionid(int collectionid);

}
