
package com.web.memoire.user.jpa.repository;

import com.web.memoire.user.jpa.entity.PwdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PwdRepository extends JpaRepository<PwdEntity, String> {
    // userId로 이전 비밀번호 이력을 조회하는 메소드 (가장 최근 비밀번호를 가져올 때 유용)
    List<PwdEntity> findByUserIdOrderByChPwdDesc(String userId);

    // 특정 userId와 prevPwd가 일치하는지 확인하는 메소드 (비밀번호 재사용 방지 시)
    boolean existsByUserIdAndPrevPwd(String userId, String prevPwd);
}