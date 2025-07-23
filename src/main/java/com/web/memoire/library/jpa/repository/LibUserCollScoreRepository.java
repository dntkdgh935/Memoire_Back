package com.web.memoire.library.jpa.repository;

import com.web.memoire.common.entity.TagEntity;
import com.web.memoire.common.entity.UserCollScoreEntity;
import com.web.memoire.common.entity.UserCollScoreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

//import java.lang.ScopedValue;

public interface LibUserCollScoreRepository extends JpaRepository<UserCollScoreEntity, UserCollScoreId> {
    @Query("SELECT u FROM UserCollScoreEntity u WHERE u.userid = :userid AND u.collectionid = :collectionid")
    UserCollScoreEntity findByUserAndCollection(@Param("userid") String userid, @Param("collectionid") int collectionid);

    List<UserCollScoreEntity> findByUseridOrderByScoreDesc(String userid);
}
