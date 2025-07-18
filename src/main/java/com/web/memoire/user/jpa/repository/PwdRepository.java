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
    // 특정 사용자의 비밀번호 이력을 최신순으로 정렬하여 N개만 가져오는 메서드
    // JPQL에서 LIMIT 대신 FETCH FIRST ROWS ONLY를 사용 (DB 방언에 따라 다름)
    // Spring Data JPA는 메서드 이름으로도 쿼리 생성이 가능하지만, 명시적 쿼리도 유효합니다.
    // 만약 H2, MySQL 등 다른 DB를 사용한다면 LIMIT 키워드를 사용해야 할 수 있습니다.
    // @Query("SELECT p FROM PwdEntity p WHERE p.userId = :userId ORDER BY p.chPwd DESC LIMIT :limitRecords")
    List<PwdEntity> findTopByUserIdOrderByChPwdDesc(String userId, org.springframework.data.domain.Pageable pageable);

    // findTopRecordsByUserIdOrderByChPwdDesc 메서드는 Pageable을 사용하여 구현하는 것이 더 일반적입니다.
    // Pageable을 사용하면 limitRecords를 직접 전달하는 대신 PageRequest.of(0, limitRecords)를 사용합니다.
    // 여기서는 기존 메서드 시그니처를 유지하면서 JPQL을 사용합니다.
    @Query("SELECT p FROM PwdEntity p WHERE p.userId = :userId ORDER BY p.chPwd DESC FETCH FIRST :limitRecords ROWS ONLY")
    List<PwdEntity> findTopRecordsByUserIdOrderByChPwdDesc(@Param("userId") String userId, @Param("limitRecords") int limitRecords);


    // 필요하다면 특정 사용자의 모든 이력을 가져오는 메서드
    List<PwdEntity> findByUserIdOrderByChPwdDesc(String userId);
}