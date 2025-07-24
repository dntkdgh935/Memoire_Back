package com.web.memoire.user.jpa.repository;

import com.web.memoire.user.jpa.entity.PwdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // @Param 임포트 추가
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PwdRepository extends JpaRepository<PwdEntity, Long> { // ID 타입을 Long으로 변경!

     //특정 사용자의 최신 비밀번호 이력(가장 최근에 변경된 비밀번호)을 조회합니다.

    @Query(value = "SELECT * FROM TB_PWD_HISTORY WHERE USERID = :userId ORDER BY CH_PWD DESC FETCH FIRST 1 ROW ONLY", nativeQuery = true)
    Optional<PwdEntity> findLatestByUserId(@Param("userId") String userId);


     // 특정 사용자의 최근 N개의 비밀번호 이력을 변경일자 내림차순으로 조회합니다.

    @Query("SELECT p FROM PwdEntity p WHERE p.userId = :userId ORDER BY p.chPwd DESC FETCH FIRST :limitRecords ROWS ONLY")
    List<PwdEntity> findTopRecordsByUserIdOrderByChPwdDesc(@Param("userId") String userId, @Param("limitRecords") int limitRecords);


    //특정 사용자의 모든 비밀번호 이력을 변경일자 내림차순으로 조회합니다.

    List<PwdEntity> findByUserIdOrderByChPwdDesc(String userId);
}