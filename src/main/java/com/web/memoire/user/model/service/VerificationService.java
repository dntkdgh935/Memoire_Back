// src/main/java/com/web/memoire/user/model/service/VerificationService.java
package com.web.memoire.user.model.service;

import com.web.memoire.user.jpa.entity.PhoneVerificationEntity;
import com.web.memoire.user.jpa.repository.PhoneVerificationRepository; // 새로 추가될 Repository
import com.web.memoire.user.util.CodeGenerator; // 새로 추가될 유틸리티 클래스
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date; // Date 타입 사용
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    // UserRepository 대신 PhoneVerificationRepository를 주입받습니다.
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final ImapVerificationService imapVerificationService; // IMAP 서비스 주입

    /**
     * 휴대폰 인증 코드를 생성하고 데이터베이스에 저장/업데이트합니다.
     * @param phoneNumber 인증 코드를 생성할 휴대폰 번호
     * @return 생성된 인증 코드
     */
    @Transactional
    public String generateVerificationCode(String phoneNumber) {
        String code = CodeGenerator.generateRandomCode(4); // 4자리 난수 코드 생성
        // 현재 시간 + 5분 후를 만료 시간으로 설정 (Node.js의 10분에서 5분으로 조정)
        Date expirationTime = new Date(System.currentTimeMillis() + 5 * 60 * 1000);

        // 전화번호를 @Id로 사용하므로, findById로 기존 엔티티를 찾거나 새로 생성
        Optional<PhoneVerificationEntity> existingVerification = phoneVerificationRepository.findById(phoneNumber);
        PhoneVerificationEntity verificationEntity;

        if (existingVerification.isPresent()) {
            // 기존 코드가 있다면 업데이트 (status 필드가 없으므로, 그냥 덮어씌움)
            verificationEntity = existingVerification.get();
            verificationEntity.setVerificationCode(code);
            verificationEntity.setExpirationTimestamp(expirationTime);
            log.info("[Code Updated] Phone: {}, New Code: {}", phoneNumber, code);
        } else {
            // 새로운 코드라면 생성
            verificationEntity = PhoneVerificationEntity.builder()
                    .phone(phoneNumber)
                    .verificationCode(code)
                    .expirationTimestamp(expirationTime)
                    .build();
            log.info("[Code Generated] Phone: {}, Code: {}", phoneNumber, code);
        }

        phoneVerificationRepository.save(verificationEntity); // DB에 저장 또는 업데이트
        return code;
    }

    /**
     * 휴대폰 인증 코드를 검증합니다.
     * 데이터베이스 확인 후, IMAP을 통해 실제 이메일 수신 여부를 확인합니다.
     * @param phoneNumber 사용자 입력 휴대폰 번호
     * @param code 사용자 입력 인증 코드
     * @return 인증 성공 여부 (true/false)
     * @throws Exception IMAP 통신 오류 등 발생 시 예외 throw
     */
    @Transactional
    public boolean verifyCode(String phoneNumber, String code) throws Exception {
        // 1. DB에서 유효하고 만료되지 않은 코드 확인
        Optional<PhoneVerificationEntity> verificationOpt = phoneVerificationRepository
                .findByPhoneAndVerificationCodeAndExpirationTimestampAfter(
                        phoneNumber, code, new Date() // 현재 시간보다 만료 시간이 미래여야 함
                );

        if (verificationOpt.isEmpty()) {
            log.warn("[Verification Failed] No matching valid/unexpired code found in DB. Phone: {}, Code: {}", phoneNumber, code);
            return false; // DB에 일치하는 유효한 코드 없음
        }

        PhoneVerificationEntity foundVerification = verificationOpt.get();

        // 2. IMAP을 통한 실제 이메일 인증 시도
        String verifiedPhoneNumberFromEmail;
        try {
            verifiedPhoneNumberFromEmail = imapVerificationService.verifyCodeViaEmail(code);
        } catch (Exception e) {
            log.error("[IMAP Verification Failed] Phone: {}, Code: {}, IMAP Error: {}", phoneNumber, code, e.getMessage());
            // IMAP 오류는 재시도 가능한 상황일 수 있으므로, RuntimeException으로 변환하여 Controller로 전달
            throw new RuntimeException("IMAP verification failed: " + e.getMessage());
        }

        // 3. 이메일에서 추출된 번호와 사용자 입력 번호 일치 여부 확인
        if (!phoneNumber.equals(verifiedPhoneNumberFromEmail)) {
            log.warn("[Verification Failed] Phone number mismatch. User provided: {}, Email From: {}", phoneNumber, verifiedPhoneNumberFromEmail);
            return false;
        }

        // 4. 모든 검증 성공 시, DB에서 해당 인증 코드 엔티티를 삭제하여 재사용 방지
        phoneVerificationRepository.delete(foundVerification);
        log.info("[Verification Success] Verified Phone: {}, Code: {}", verifiedPhoneNumberFromEmail, code);
        return true;
    }
}