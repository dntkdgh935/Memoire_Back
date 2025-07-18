package com.web.memoire.user.model.service;

import com.web.memoire.user.jpa.entity.PwdEntity;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.Pwd;
import com.web.memoire.user.model.dto.User;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final PwdService pwdService;

    public boolean selectCheckId(String loginId) {
        return userRepository.existsById(loginId);
    }

    public User selectUser(String loginId){
        return userRepository.findByLoginId(loginId)
                .map(UserEntity::toDto)
                .orElseThrow(() -> {
                    log.warn("사용자 조회 실패: loginId '{}' 에 해당하는 사용자를 찾을 수 없습니다.", loginId);
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
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("자동 로그인 설정 변경 대상 사용자를 찾을 수 없습니다: " + userId));

        if (userEntity.getAutoLoginFlag() == null || !userEntity.getAutoLoginFlag().equals(autoLoginFlag)) {
            userEntity.setAutoLoginFlag(autoLoginFlag);
            userRepository.save(userEntity);
            log.info("사용자 {} (userId: {})의 autoLoginFlag가 {}로 업데이트되었습니다.", userEntity.getLoginId(), userId, autoLoginFlag);
        } else {
            log.debug("사용자 {} (userId: {})의 autoLoginFlag가 이미 {}이므로 업데이트하지 않습니다.", userEntity.getLoginId(), userId, autoLoginFlag);
        }
    }

    public String findLoginIdByNameAndPhone(String name, String phone) {
        UserEntity userEntity = userRepository.findLoginIdByNameAndPhone(name, phone)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + name));
        return userEntity.getLoginId();
    }

    @Transactional
    public UserEntity updateUserPassword(@NotNull String userId, String newEncodedPassword) {
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("비밀번호 변경 대상 사용자를 찾을 수 없습니다: " + userId));

        String currentEncodedPassword = userEntity.getPassword();
        // 비밀번호 변경 이력을 저장할 때 Pwd DTO의 toEntity()를 사용하려면 Pwd 객체를 생성하여 넘겨야 합니다.
        // 현재 PwdService의 savePasswordHistory 메서드가 어떻게 구현되어 있는지 모르지만
        // 일반적으로는 여기서는 userId, 이전 비밀번호, 새 비밀번호를 직접 전달하여 Entity를 만들게 됩니다.
        // PwdService.savePasswordHistory(userId, currentEncodedPassword, newEncodedPassword);
        // 만약 PwdService가 Pwd DTO를 받도록 되어 있다면, 다음과 같이 Pwd 객체를 생성해야 합니다.
        Pwd pwdHistory = Pwd.builder()
                .userId(userId)
                .chPwd(new Date()) // 변경일은 현재 날짜
                .prevPwd(currentEncodedPassword)
                .currPwd(newEncodedPassword)
                .build();
        pwdService.savePasswordHistory(pwdHistory.toEntity());


        userEntity.setPassword(newEncodedPassword);
        return userRepository.save(userEntity);
    }

    @Transactional
    public UserEntity updateMyInfo(
            @NotNull String userId,
            String nickname,
            String phone,
            String birthday, // YYYY-MM-DD 형식의 문자열
            String profileImagePath,
            String statusMessage
    ) throws ParseException {
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다: " + userId));

        // 각 필드가 null이 아니면 업데이트
        if (nickname != null) {
            userEntity.setNickname(nickname);
        }
        if (phone != null) {
            userEntity.setPhone(phone);
        }
        if (birthday != null && !birthday.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            userEntity.setBirthday(sdf.parse(birthday));
        }
        if (profileImagePath != null) {
            userEntity.setProfileImagePath(profileImagePath);
        }
        if (statusMessage != null) {
            userEntity.setStatusMessage(statusMessage);
        }

        return userRepository.save(userEntity);
    }

    public User findUserByLoginIdAndPhone(@NotNull String loginId, String phone) {
        Optional<UserEntity> userEntityOptional = userRepository.findByLoginIdAndPhone(loginId, phone);

        if (userEntityOptional.isPresent()) {
            return userEntityOptional.get().toDto();
        } else {
            log.warn("findUserByLoginIdAndPhone: loginId '{}', phone '{}' 에 해당하는 사용자를 찾을 수 없습니다.", loginId, phone);
            return null;
        }
    }
}