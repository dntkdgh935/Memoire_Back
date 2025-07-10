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
}
