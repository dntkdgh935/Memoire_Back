package com.web.memoire.archive.jpa.repository;

import com.web.memoire.common.entity.RelationshipEntity;
import com.web.memoire.common.entity.RelationshipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchiveRelationshipRepository extends JpaRepository<RelationshipEntity, RelationshipId> {

    // 유저의 팔로잉 조회
    @Query(value = "SELECT r FROM RelationshipEntity r WHERE r.userid = :userid AND r.status = '1'")
    List<RelationshipEntity> findAllUserFollowing(@Param("userid") String userid);

    // 유저의 팔로잉 개수 조회
    @Query(value = "SELECT count(r) FROM RelationshipEntity r WHERE r.userid = :userid AND r.status = '1'")
    int countAllFollowingByUserId(@Param("userid") String userid);

    // 유저의 팔로워 조회
    @Query(value = "SELECT r FROM RelationshipEntity r WHERE r.targetid = :userid AND r.status = '1'")
    List<RelationshipEntity> findAllUserFollower(@Param("userid") String userid);

    // 유저의 팔로워 개수 조회
    @Query(value = "SELECT count(r) FROM RelationshipEntity r WHERE r.targetid = :userid AND r.status = '1'")
    int countAllFollowerByUserId(@Param("userid") String userid);

    // 유저가 보낸 요청 조회
    @Query(value = "SELECT r FROM RelationshipEntity r WHERE r.userid = :userid AND r.status = '0'")
    List<RelationshipEntity> findAllUserRequestFollowing(@Param("userid") String userid);

    // 유저가 받은 요청 조회
    @Query(value = "SELECT r FROM RelationshipEntity r WHERE r.targetid = :userid AND r.status = '0'")
    List<RelationshipEntity> findAllUserRequestFollower(@Param("userid") String userid);

    // 유저가 차단한 유저들 조회
    @Query(value = "SELECT r FROM RelationshipEntity r WHERE r.userid = :userid AND r.status = '2'")
    List<RelationshipEntity> findAllUserBlock(@Param("userid") String userid);
}
