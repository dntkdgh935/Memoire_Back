package com.web.memoire.user.model.service;

import com.web.memoire.user.jpa.entity.PwdEntity;
import com.web.memoire.user.jpa.entity.UserEntity; // UserEntity를 가져와 현재 비밀번호를 조회할 수 있음
import com.web.memoire.user.jpa.repository.PwdRepository;
import com.web.memoire.user.jpa.repository.UserRepository; // UserEntity의 현재 비밀번호를 가져오기 위해 필요
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PwdService {

    private final PwdRepository pwdRepository;
    private final UserRepository userRepository; // UserEntity의 현재 비밀번호를 가져오기 위해 필요

    /**
     * 사용자의 현재 비밀번호를 이전 비밀번호 이력으로 저장합니다.
     * 이 메소드는 새로운 비밀번호로 업데이트되기 직전에 호출되어야 합니다.
     *
     * @param userId 비밀번호 이력을 저장할 사용자의 고유 ID
     * @param currentEncodedPassword 현재 사용 중인 암호화된 비밀번호
     * @param newEncodedPassword 새로 설정될 암호화된 비밀번호
     */
    @Transactional
    public void savePasswordHistory(String userId, String currentEncodedPassword, String newEncodedPassword) {
        // PwdEntity의 PK 전략에 따라 로직이 달라질 수 있습니다.
        // 현재 PwdEntity의 PK가 userId이므로, 기존 레코드가 있다면 업데이트, 없다면 새로 생성하는 방식
        // 또는 PwdEntity의 PK를 자동 증가 ID로 변경하여 이력을 계속 추가하는 방식

        // 여기서는 이력을 계속 추가하는 방식 (PwdEntity의 PK가 Long id; 이고 userId는 일반 컬럼이라고 가정)
        // 만약 PwdEntity의 PK가 userId, chPwd 복합키라면, chPwd를 new Date()로 설정하여 새로운 이력을 만듭니다.

        PwdEntity pwdEntity = PwdEntity.builder()
                .userId(userId)
                .prevPwd(currentEncodedPassword) // 이전 비밀번호
                .currPwd(newEncodedPassword)     // 현재 비밀번호 (새로 설정될 비밀번호)
                // .chPwd는 @PrePersist에서 설정되거나, 여기서 new Date()로 명시적으로 설정
                .build();

        pwdRepository.save(pwdEntity);
        log.info("사용자 {}의 비밀번호 이력이 저장되었습니다. 이전 비밀번호: {}, 새 비밀번호: {}",
                userId, currentEncodedPassword, newEncodedPassword);
    }

    /**
     * 특정 사용자에게 특정 이전 비밀번호가 사용된 적이 있는지 확인합니다.
     * (비밀번호 재사용 방지 로직에 사용될 수 있습니다.)
     * @param userId 사용자 ID
     * @param encodedPassword 확인할 암호화된 비밀번호
     * @return 해당 비밀번호가 이전 비밀번호로 존재하면 true, 아니면 false
     */
    public boolean hasUsedPreviousPassword(String userId, String encodedPassword) {
        // PwdRepository에 existsByUserIdAndPrevPwd(String userId, String prevPwd)와 같은 메소드가 필요
        return pwdRepository.existsByUserIdAndPrevPwd(userId, encodedPassword);

    }
}
