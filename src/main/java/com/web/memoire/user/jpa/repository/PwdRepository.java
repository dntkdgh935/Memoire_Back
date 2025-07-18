
package com.web.memoire.user.jpa.repository;

import com.web.memoire.user.jpa.entity.PwdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PwdRepository extends JpaRepository<PwdEntity, String> {
    // 특정 사용자의 비밀번호 이력을 최신순으로 정렬하여 N개만 가져오는 메서드
    @Query("SELECT p FROM PwdEntity p WHERE p.userId = :userId ORDER BY p.chPwd DESC FETCH FIRST :limitRecords ROWS ONLY")
    List<PwdEntity> findTopRecordsByUserIdOrderByChPwdDesc(String userId, int limitRecords);

    // 필요하다면 특정 사용자의 모든 이력을 가져오는 메서드
    List<PwdEntity> findByUserIdOrderByChPwdDesc(String userId);
}