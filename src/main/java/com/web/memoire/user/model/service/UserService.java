package com.web.memoire.user.model.service;

import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Autowired
    private final UserRepository userRepository;

    public boolean selectCheckId(String loginId) {
        return userRepository.existsById(loginId);
    }

    public User selectUser(String loginId){
        return userRepository.findByLoginId(loginId)
                .map(UserEntity::toDto) // UserEntity가 존재하면 toDto() 호출하여 User DTO로 매핑
                .orElseThrow(() -> {
                    log.warn("사용자 조회 실패: loginId '{}' 에 해당하는 사용자를 찾을 수 없습니다.", loginId);
                    // 적절한 예외를 던집니다. 예를 들어, NoSuchElementException 또는 Custom Exception
                    return new NoSuchElementException("사용자를 찾을 수 없습니다.");
                });
    }

    @Transactional
    public void insertUser(User user){
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            String newUserId = UUID.randomUUID().toString();
            user.setUserId(newUserId);
            log.info("새로운 userId (UUID) 생성 및 할당: " + newUserId);
        }
        userRepository.save(user.toEntity());
    }
    @Transactional
    public void updateUserAutoLoginFlag(String userId, String autoLoginFlag) {
        // userId로 사용자 조회
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("자동 로그인 설정 변경 대상 사용자를 찾을 수 없습니다: " + userId));

        // autoLoginFlag 업데이트
        if (userEntity.getAutoLoginFlag() == null || !userEntity.getAutoLoginFlag().equals(autoLoginFlag)) {
            userEntity.setAutoLoginFlag(autoLoginFlag);
            userRepository.save(userEntity); // 변경사항 저장
            log.info("사용자 {} (userId: {})의 autoLoginFlag가 {}로 업데이트되었습니다.", userEntity.getLoginId(), userId, autoLoginFlag);
        } else {
            log.debug("사용자 {} (userId: {})의 autoLoginFlag가 이미 {}이므로 업데이트하지 않습니다.", userEntity.getLoginId(), userId, autoLoginFlag);
        }
    }
}
