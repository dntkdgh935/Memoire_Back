package com.web.memoire.user.model.service;

import com.web.memoire.user.jpa.entity.PwdEntity;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.PwdRepository;
import com.web.memoire.user.jpa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // BCryptPasswordEncoder 임포트
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List; // List 임포트
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PwdService {

    private final PwdRepository pwdRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bcryptPasswordEncoder; // BCryptPasswordEncoder 주입

    /**
     * 비밀번호 이력 엔티티를 저장합니다.
     * 이 메서드는 UserService에서 PwdEntity 객체를 직접 받아 저장하는 데 사용됩니다.
     *
     * @param pwdEntity 저장할 PwdEntity 객체
     */
    @Transactional
    public void savePasswordHistory(PwdEntity pwdEntity) {
        pwdRepository.save(pwdEntity);
        log.info("사용자 {}의 비밀번호 이력이 저장되었습니다. 변경일: {}, 이전 비밀번호: {}, 새 비밀번호: {}",
                pwdEntity.getUserId(), pwdEntity.getChPwd(), pwdEntity.getPrevPwd(), pwdEntity.getCurrPwd());
    }

    /**
     * 특정 사용자에게 특정 (암호화된) 비밀번호가 이전 비밀번호 이력으로 사용된 적이 있는지 확인합니다.
     * 이 메서드는 새로운 비밀번호를 설정하기 전, 비밀번호 재사용을 방지하기 위해 호출될 수 있습니다.
     * 예를 들어, 최근 3개의 비밀번호를 재사용할 수 없도록 하는 정책을 가정합니다.
     *
     * @param userId             사용자 ID
     * @param encodedNewPassword 확인할 암호화된 (새로운) 비밀번호
     * @param limitHistoryCount  확인할 이전 비밀번호 이력 개수 (예: 3이면 최근 3개)
     * @return 해당 암호화된 비밀번호가 지정된 개수 내의 이전 비밀번호 이력과 일치하면 true, 아니면 false
     */
    public boolean hasUsedPreviousPassword(String userId, String encodedNewPassword, int limitHistoryCount) {
        // 특정 사용자의 비밀번호 이력을 최신순으로 limitHistoryCount 만큼 가져옵니다.
        // PwdRepository에 findTopNByUserIdOrderByChPwdDesc 메서드가 필요합니다.
        // (만약 없다면 List<PwdEntity> findByUserIdOrderByChPwdDesc(String userId)를 사용 후, Java 코드에서 subList 처리)
        List<PwdEntity> pwdHistory = pwdRepository.findTopRecordsByUserIdOrderByChPwdDesc(userId, limitHistoryCount);

        for (PwdEntity historyEntry : pwdHistory) {
            // 이전 비밀번호 (prevPwd)와 새 비밀번호 (encodedNewPassword) 비교
            // matches() 메서드를 사용하여 평문 비밀번호와 암호화된 비밀번호를 비교합니다.
            if (historyEntry.getPrevPwd() != null && bcryptPasswordEncoder.matches(encodedNewPassword, historyEntry.getPrevPwd())) {
                log.info("사용자 {}의 새 비밀번호가 이전 비밀번호 이력 (prevPwd)과 일치합니다.", userId);
                return true;
            }
            // 현재 비밀번호 (currPwd)와 새 비밀번호 (encodedNewPassword) 비교
            // 이전에 '현재 비밀번호'였던 것이 현재 '새 비밀번호'와 같은 경우를 방지
            if (historyEntry.getCurrPwd() != null && bcryptPasswordEncoder.matches(encodedNewPassword, historyEntry.getCurrPwd())) {
                log.info("사용자 {}의 새 비밀번호가 과거 변경 시 현재 비밀번호 이력 (currPwd)과 일치합니다.", userId);
                return true;
            }
        }
        return false;
    }

    // 다른 Pwd 관련 서비스 메서드들...
}