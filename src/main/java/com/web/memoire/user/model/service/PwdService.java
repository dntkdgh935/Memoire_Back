package com.web.memoire.user.model.service;

import com.web.memoire.user.jpa.entity.PwdEntity;
import com.web.memoire.user.jpa.repository.PwdRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PwdService {

    private final PwdRepository pwdRepository;
    private final BCryptPasswordEncoder bcryptPasswordEncoder;
    private static final int PASSWORD_HISTORY_LIMIT = 3;

    /**
     * 비밀번호 이력 엔티티를 저장합니다.
     * PwdEntity의 ID가 자동 생성되므로, 이 메서드는 항상 새로운 비밀번호 이력 레코드를 저장합니다.
     *
     * @param pwdEntity 저장할 PwdEntity 객체 (prevPwd, currPwd는 이미 암호화된 상태여야 함)
     */
    @Transactional
    public void savePasswordHistory(PwdEntity pwdEntity) {
        pwdRepository.save(pwdEntity);
        log.info("사용자 {}의 비밀번호 이력이 저장되었습니다. 변경일: {}, 이전 비밀번호: {}, 새 비밀번호: {}",
                pwdEntity.getUserId(), pwdEntity.getChPwd(), pwdEntity.getPrevPwd(), pwdEntity.getCurrPwd());
    }

    /**
     * 특정 사용자에게 특정 (평문) 비밀번호가 이전 비밀번호 이력으로 사용된 적이 있는지 확인합니다.
     *
     * @param userId          사용자 ID
     * @param rawNewPassword  확인할 평문 (새로운) 비밀번호
     * @param limitHistoryCount 확인할 이전 비밀번호 이력 개수 (예: 3이면 최근 3개)
     * @return 해당 평문 비밀번호가 지정된 개수 내의 이전 비밀번호 이력과 일치하면 true, 아니면 false
     */
    public boolean hasUsedPreviousPassword(String userId, String rawNewPassword, int limitHistoryCount) {
        List<PwdEntity> pwdHistory = pwdRepository.findTopRecordsByUserIdOrderByChPwdDesc(userId, limitHistoryCount);

        for (PwdEntity historyEntry : pwdHistory) {
            // 이전 비밀번호 (prevPwd)와 새 비밀번호 (rawNewPassword) 비교
            if (historyEntry.getPrevPwd() != null && bcryptPasswordEncoder.matches(rawNewPassword, historyEntry.getPrevPwd())) {
                log.warn("사용자 {}의 새 비밀번호가 이전 비밀번호 이력 (prevPwd)과 일치합니다.", userId);
                return true;
            }
            // 과거 변경 시 '현재 비밀번호'였던 것 (currPwd)과 새 비밀번호 (rawNewPassword) 비교
            if (historyEntry.getCurrPwd() != null && bcryptPasswordEncoder.matches(rawNewPassword, historyEntry.getCurrPwd())) {
                log.warn("사용자 {}의 새 비밀번호가 과거 변경 시 현재 비밀번호 이력 (currPwd)과 일치합니다.", userId);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void changeUserPassword(String userId, String prevPlainPassword, String newPlainPassword) {
        // 1. 해당 사용자의 최신 비밀번호 이력 조회
        PwdEntity latestPwdEntity = pwdRepository.findLatestByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("소셜로그인 유저는 비밀번호 변경을 할 수 없습니다."));

        String currentEncodedPassword = latestPwdEntity.getCurrPwd(); // 현재 암호화된 비밀번호

        // 2. 사용자가 입력한 이전 비밀번호와 현재 비밀번호 일치 여부 확인
        if (!bcryptPasswordEncoder.matches(prevPlainPassword, currentEncodedPassword)) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 3. 새 비밀번호가 현재 비밀번호와 동일한지 확인
        if (bcryptPasswordEncoder.matches(newPlainPassword, currentEncodedPassword)) {
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 동일할 수 없습니다.");
        }

        // 4. 새 비밀번호가 최근 사용한 비밀번호와 중복되는지 확인
        // (현재 사용 중인 비밀번호를 제외하고 이력 검사)
        checkNewPasswordAgainstHistory(userId, newPlainPassword, currentEncodedPassword);

        // 5. 새 비밀번호 암호화
        String newEncodedPassword = bcryptPasswordEncoder.encode(newPlainPassword);

        // 6. 새로운 비밀번호 이력 엔티티 생성
        PwdEntity newPwdHistory = PwdEntity.builder()
                .userId(userId)
                .chPwd(new Date())
                .prevPwd(currentEncodedPassword) // 현재 비밀번호가 이전 비밀번호가 됨
                .currPwd(newEncodedPassword) // 새롭게 암호화된 비밀번호
                .build();

        // 7. 비밀번호 이력 저장
        savePasswordHistory(newPwdHistory);
        log.info("비밀번호 변경 성공: userId={}", userId);
    }

    @Transactional
    public void resetPassword(@NotNull String userId, @NotNull String newRawPassword) {
        // 1. 새 비밀번호 암호화
        String newEncodedPassword = bcryptPasswordEncoder.encode(newRawPassword);

        // 2. 과거 비밀번호 재사용 정책 확인 (선택 사항, 필요시 추가)
        // resetPassword에서도 과거 비밀번호 재사용을 막는 것이 좋습니다.
        final int HISTORY_CHECK_LIMIT = 3;
        if (hasUsedPreviousPassword(userId, newRawPassword, HISTORY_CHECK_LIMIT)) {
            throw new IllegalArgumentException("새 비밀번호는 최근 " + HISTORY_CHECK_LIMIT + "회 사용했던 비밀번호와 다르게 설정해야 합니다.");
        }

        // 3. 새로운 비밀번호 이력 엔티티 생성 및 저장
        // reset 시에는 이전 비밀번호를 알 수 없으므로, prevPwd는 null로 설정하거나
        // 가장 최근의 currPwd를 가져와 prevPwd로 설정할 수 있습니다.
        // 여기서는 가장 최근의 currPwd를 가져와 prevPwd로 설정하는 것이 이력 관리에 더 유용합니다.
        Optional<PwdEntity> latestPwdOpt = pwdRepository.findLatestByUserId(userId);
        String prevEncodedPasswordForHistory = latestPwdOpt.isPresent() ? latestPwdOpt.get().getCurrPwd() : null;

        PwdEntity newPwdHistory = PwdEntity.builder()
                .userId(userId)
                .chPwd(new Date()) // 변경 시간 기록
                .prevPwd(prevEncodedPasswordForHistory) // 이전 비밀번호가 있다면 기록, 없다면 null
                .currPwd(newEncodedPassword) // 새롭게 암호화된 비밀번호
                .build();

        pwdRepository.save(newPwdHistory);
        log.info("비밀번호 재설정 성공: userId={}", userId);
    }
    /**
     * 새 비밀번호가 최근 사용한 비밀번호와 중복되는지 확인하는 헬퍼 메서드
     * @param userId 사용자 ID
     * @param newPlainPassword 새로 설정하려는 평문 비밀번호
     * @param currentEncodedPassword 현재 사용 중인 암호화된 비밀번호 (중복 검사에서 제외할 목적)
     * @throws IllegalArgumentException 중복된 비밀번호인 경우
     */
    private void checkNewPasswordAgainstHistory(String userId, String newPlainPassword, String currentEncodedPassword) {
        // 최근 PASSWORD_HISTORY_LIMIT 개의 비밀번호 이력 조회
        List<PwdEntity> recentPasswords = pwdRepository.findTopRecordsByUserIdOrderByChPwdDesc(userId, PASSWORD_HISTORY_LIMIT);

        for (PwdEntity pwdEntity : recentPasswords) {
            // 현재 DB에 저장된 비밀번호 (currPwd)와 새 비밀번호를 비교
            // 단, 현재 변경하려는 비밀번호가 (파라미터로 넘어온) '현재 사용 중인 비밀번호'와 동일하다면 이 이력은 건너김
            // '새 비밀번호는 현재 비밀번호와 동일할 수 없다'는 체크는 이미 `changeUserPassword` 및 `resetPassword` 상단에서 이루어졌기 때문
            if (currentEncodedPassword != null && bcryptPasswordEncoder.matches(newPlainPassword, currentEncodedPassword)) {
                // 새 비밀번호가 현재 사용중인 비밀번호와 동일한 경우. 이 케이스는 이 메서드 외부에서 처리되므로 여기서는 무시.
                continue;
            }

            // `pwdEntity.getCurrPwd()`: 과거에 '현재 비밀번호'였던 값들
            if (pwdEntity.getCurrPwd() != null && bcryptPasswordEncoder.matches(newPlainPassword, pwdEntity.getCurrPwd())) {
                throw new IllegalArgumentException("새 비밀번호는 최근 " + PASSWORD_HISTORY_LIMIT + "개 이내에 사용했던 비밀번호와 동일할 수 없습니다.");
            }

            // `pwdEntity.getPrevPwd()`: 과거에 '이전 비밀번호'였던 값들 (선택적으로 검사, 보통 currPwd만으로도 충분)
            // 필요하다면 이 부분의 주석을 해제하여 이전 비밀번호 필드도 검사에 포함할 수 있습니다.
            // if (pwdEntity.getPrevPwd() != null && bcryptPasswordEncoder.matches(newPlainPassword, pwdEntity.getPrevPwd())) {
            //     throw new IllegalArgumentException("새 비밀번호는 최근 " + PASSWORD_HISTORY_LIMIT + "개 이내에 사용했던 비밀번호와 동일할 수 없습니다.");
            // }
        }
    }
}