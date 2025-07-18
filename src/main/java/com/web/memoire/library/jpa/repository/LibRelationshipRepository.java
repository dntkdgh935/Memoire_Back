package com.web.memoire.library.jpa.repository;

import com.web.memoire.common.entity.RelationshipEntity;
import com.web.memoire.common.entity.RelationshipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface LibRelationshipRepository extends JpaRepository<RelationshipEntity, RelationshipId> {

    // 유저의 팔로잉 조회 (from ArchiveRepository)
    @Query(value = "SELECT r FROM RelationshipEntity r WHERE r.userid = :userid AND r.status = '1'")
    List<RelationshipEntity> findAllUserFollowing(@Param("userid") String userid);

    Optional<RelationshipEntity> findByUseridAndTargetid(String userId, String authorid);

    List<RelationshipEntity> findByTargetidAndStatus(String userid, String number);
}
