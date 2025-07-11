// src/main/java/com/web/memoire/user/controller/VerificationController.java
package com.web.memoire.user.controller;

import com.web.memoire.user.model.dto.PhoneVerification; // PhoneVerification DTO 사용
import com.web.memoire.user.model.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*") // 개발용: 실제 서비스에서는 특정 도메인으로 제한
@RequestMapping("/api/verification")
public class VerificationController {

    private final VerificationService verificationService;

    /**
     * 휴대폰 인증 코드를 생성하고 클라이언트에 반환합니다.
     * @param request (PhoneVerification) phoneNumber 필드만 포함하는 요청 DTO
     * @return (PhoneVerification) 생성된 코드와 메시지를 포함하는 응답 DTO
     */
    @PostMapping("/generate-code")
    public ResponseEntity<PhoneVerification> generateCode(@RequestBody PhoneVerification request) {
        String phoneNumber = request.getPhone(); // phone 필드 사용
        if (phoneNumber == null || !phoneNumber.matches("^01[016789][0-9]{7,8}$")) {
            return ResponseEntity.badRequest().body(PhoneVerification.builder()
                    .phone(phoneNumber)
                    .verificationCode(null) // 코드 없음
                    .expirationTimestamp(null) // 타임스탬프 없음
                    // message 필드가 없으므로, 필요하다면 PhoneVerification DTO에 추가하거나 에러 핸들링
                    .build());
        }

        try {
            String generatedCode = verificationService.generateVerificationCode(phoneNumber);
            // 성공 응답: phone과 생성된 코드만 포함
            return ResponseEntity.ok(PhoneVerification.builder()
                    .phone(phoneNumber)
                    .verificationCode(generatedCode)
                    .expirationTimestamp(null) // 클라이언트에게는 이 정보를 보내지 않음
                    .build());
        } catch (Exception e) {
            log.error("Error generating verification code for phone {}: {}", phoneNumber, e.getMessage());
            // 실패 응답: phone만 포함하고 코드 및 타임스탬프는 null
            return ResponseEntity.internalServerError().body(PhoneVerification.builder()
                    .phone(phoneNumber)
                    .verificationCode(null)
                    .expirationTimestamp(null)
                    .build());
        }
    }

    /**
     * 휴대폰 인증 코드를 검증합니다.
     * @param request (PhoneVerification) phoneNumber와 verificationCode 필드를 포함하는 요청 DTO
     * @return (PhoneVerification) 인증 성공/실패 여부를 포함하는 응답 DTO
     */
    @PostMapping("/verify-code")
    public ResponseEntity<PhoneVerification> verifyCode(@RequestBody PhoneVerification request) {
        String phoneNumber = request.getPhone(); // phone 필드 사용
        String code = request.getVerificationCode(); // verificationCode 필드 사용

        if (phoneNumber == null || !phoneNumber.matches("^01[016789][0-9]{7,8}$") || code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().body(PhoneVerification.builder()
                    .phone(phoneNumber)
                    .verificationCode(null)
                    .expirationTimestamp(null)
                    .build());
        }

        try {
            boolean verified = verificationService.verifyCode(phoneNumber, code);
            if (verified) {
                // 성공 응답: phone만 포함 (verifiedPhoneNumber 역할)
                return ResponseEntity.ok(PhoneVerification.builder()
                        .phone(phoneNumber) // 인증된 전화번호
                        .verificationCode("success") // 성공 메시지 역할
                        .expirationTimestamp(null)
                        .build());
            } else {
                // 실패 응답: phone만 포함 (실패 메시지 역할)
                return ResponseEntity.ok(PhoneVerification.builder()
                        .phone(phoneNumber)
                        .verificationCode("fail") // 실패 메시지 역할
                        .expirationTimestamp(null)
                        .build());
            }
        } catch (RuntimeException e) {
            log.error("Verification failed for phone {} with code {}: {}", phoneNumber, code, e.getMessage());
            // IMAP 관련 오류는 'wait' 상태로 전달
            if (e.getMessage().contains("IMAP verification failed")) {
                return ResponseEntity.status(200).body(PhoneVerification.builder()
                        .phone(phoneNumber)
                        .verificationCode("wait") // 'wait' 메시지 역할
                        .expirationTimestamp(null)
                        .build());
            }
            return ResponseEntity.internalServerError().body(PhoneVerification.builder()
                    .phone(phoneNumber)
                    .verificationCode("error") // 일반 에러 메시지 역할
                    .expirationTimestamp(null)
                    .build());
        } catch (Exception e) {
            log.error("Unexpected error during verification for phone {} with code {}: {}", phoneNumber, code, e.getMessage());
            return ResponseEntity.internalServerError().body(PhoneVerification.builder()
                    .phone(phoneNumber)
                    .verificationCode("error")
                    .expirationTimestamp(null)
                    .build());
        }
    }
}