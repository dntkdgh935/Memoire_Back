package com.web.memoire.user.model.service;

import com.web.memoire.user.jpa.entity.PwdEntity;
import com.web.memoire.user.jpa.repository.PwdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PwdService {

    private final PwdRepository pwdRepository;
    private final BCryptPasswordEncoder bcryptPasswordEncoder;

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
}