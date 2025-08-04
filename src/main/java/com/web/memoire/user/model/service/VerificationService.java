// src/main/java/com/web/memoire/user/model/service/VerificationService.java
package com.web.memoire.user.model.service;

import com.web.memoire.user.jpa.entity.PhoneVerificationEntity;
import com.web.memoire.user.jpa.repository.PhoneVerificationRepository;
import com.web.memoire.user.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final ImapVerificationService imapVerificationService; // IMAP 서비스 주입

    /**
     * 휴대폰 인증 코드를 생성하고 데이터베이스에 저장/업데이트합니다.
     * @param phoneNumber 인증 코드를 생성할 휴대폰 번호
     * @return 생성된 인증 코드
     */
    @Transactional
    public String generateVerificationCode(String phoneNumber) {
        String code = CodeGenerator.generateRandomCode(4);
        Date expirationTime = new Date(System.currentTimeMillis() + 5 * 60 * 1000);

        Optional<PhoneVerificationEntity> existingVerification = phoneVerificationRepository.findById(phoneNumber);
        PhoneVerificationEntity verificationEntity;

        if (existingVerification.isPresent()) {
            verificationEntity = existingVerification.get();
            verificationEntity.setVerificationCode(code);
            verificationEntity.setExpirationTimestamp(expirationTime);
            log.info("[Code Updated] Phone: {}, New Code: {}", phoneNumber, code);
        } else {
            verificationEntity = PhoneVerificationEntity.builder()
                    .phone(phoneNumber)
                    .verificationCode(code)
                    .expirationTimestamp(expirationTime)
                    .build();
            log.info("[Code Generated] Phone: {}, Code: {}", phoneNumber, code);
        }

        phoneVerificationRepository.save(verificationEntity);
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
                        phoneNumber, code, new Date()
                );

        if (verificationOpt.isEmpty()) {
            log.warn("[Verification Failed] No matching valid/unexpired code found in DB. Phone: {}, Code: {}", phoneNumber, code);
            return false; // DB에 일치하는 유효한 코드 없음
        }

        PhoneVerificationEntity foundVerification = verificationOpt.get();

        // 2. IMAP을 통한 실제 이메일 인증 시도
        // IMAP 서비스에서 이미 전화번호 일치 여부를 검증하므로, 여기서는 반환 값 확인만 합니다.
        String imapVerificationResult;
        try {
            imapVerificationResult = imapVerificationService.verifyCodeViaEmail(code, phoneNumber);
        } catch (Exception e) {
            log.error("[IMAP Verification Failed] Phone: {}, Code: {}, IMAP Error: {}", phoneNumber, code, e.getMessage());
            // IMAP 오류는 재시도 가능한 상황일 수 있으므로, RuntimeException으로 변환하여 Controller로 전달
            throw new RuntimeException("IMAP verification failed: " + e.getMessage());
        }

        // 3. IMAP 인증 결과 확인
        // imapVerificationService.verifyCodeViaEmail()은 성공 시 "success"를 반환하고,
        // 실패 시 예외를 던지므로, 여기서는 예외가 발생하지 않았다면 성공으로 간주하고,
        // 추가적으로 반환 값이 "success"인지 확인하여 명확성을 높입니다.
        if (!"success".equals(imapVerificationResult)) {
            // 이 경우는 imapVerificationService 내부에서 예외를 던지지 않았지만,
            // "success"가 아닌 다른 값을 반환했을 때 (예상치 못한 상황)
            log.warn("[Verification Failed] IMAP verification returned unexpected result: {}", imapVerificationResult);
            return false;
        }

        // 4. 모든 검증 성공 시, DB에서 해당 인증 코드 엔티티를 삭제하여 재사용 방지
        phoneVerificationRepository.delete(foundVerification);
        log.info("[Verification Success] Verified Phone: {}, Code: {}", phoneNumber, code); // imap에서 추출된 번호 대신 사용자가 입력한 번호 로깅
        return true;
    }
}