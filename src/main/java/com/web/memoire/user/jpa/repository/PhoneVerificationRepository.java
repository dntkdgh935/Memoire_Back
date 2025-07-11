// src/main/java/com/web/memoire/user/jpa/repository/PhoneVerificationRepository.java
package com.web.memoire.user.jpa.repository;

import com.web.memoire.user.jpa.entity.PhoneVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date; // Date 타입 사용
import java.util.Optional;

@Repository
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerificationEntity, String> {

    /**
     * 주어진 전화번호, 인증 코드, 그리고 만료 시간 이후인 인증 정보를 조회합니다.
     * (status 필드가 없으므로, 만료 시간으로만 유효성 판단)
     * @param phone 휴대폰 번호
     * @param verificationCode 인증 코드
     * @param expirationTimestamp 기준 만료 시간 (이 시간보다 미래여야 함)
     * @return 일치하는 PhoneVerificationEntity (Optional)
     */
    Optional<PhoneVerificationEntity> findByPhoneAndVerificationCodeAndExpirationTimestampAfter(
            String phone, String verificationCode, Date expirationTimestamp);
}