package com.web.memoire.user.model.service;

import com.web.memoire.user.jpa.entity.PwdEntity;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.Pwd;
import com.web.memoire.user.model.dto.User;
import com.web.memoire.user.model.service.PwdService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    @Autowired
    private final BCryptPasswordEncoder bcryptPasswordEncoder;


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

    public User findUserByUserId(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("userId " + userId + "에 해당하는 사용자를 찾을 수 없습니다."));
        return userEntity.toDto(); // UserEntity를 User DTO로 변환하여 반환
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
    public UserEntity updateUserPassword(@NotNull String userId, String newRawPassword) { // 파라미터 이름 변경: newRawPassword
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("비밀번호 변경 대상 사용자를 찾을 수 없습니다: " + userId));

        String currentEncodedPassword = userEntity.getPassword(); // 현재 암호화된 비밀번호
        String newEncodedPassword = bcryptPasswordEncoder.encode(newRawPassword); // 새로 입력된 비밀번호 암호화

        // 1. 새 비밀번호와 현재 비밀번호 비교
        if (bcryptPasswordEncoder.matches(newRawPassword, currentEncodedPassword)) {
            log.warn("비밀번호 변경 실패: userId={}, 새 비밀번호가 현재 비밀번호와 동일합니다.", userId);
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        // 2. 새 비밀번호와 과거 비밀번호 이력 비교
        // 최근 3개의 비밀번호를 재사용할 수 없도록 정책 설정 (필요에 따라 숫자 변경)
        int passwordHistoryLimit = 3;
        if (pwdService.hasUsedPreviousPassword(userId, newRawPassword, passwordHistoryLimit)) {
            log.warn("비밀번호 변경 실패: userId={}, 새 비밀번호가 과거에 사용된 비밀번호입니다.", userId);
            throw new IllegalArgumentException("새 비밀번호는 과거에 사용했던 비밀번호와 달라야 합니다.");
        }

        // 3. 비밀번호 변경 이력 저장
        // PwdEntity의 prevPwd는 변경되기 전의 비밀번호, currPwd는 변경될 새 비밀번호를 저장
        PwdEntity pwdHistoryEntity = PwdEntity.builder()
                .userId(userId)
                .chPwd(new Date()) // 변경일은 현재 날짜/시간
                .prevPwd(currentEncodedPassword) // 이전 암호화된 비밀번호
                .currPwd(newEncodedPassword)   // 새로 암호화된 비밀번호
                .build();
        pwdService.savePasswordHistory(pwdHistoryEntity); // PwdEntity를 직접 전달

        // 4. UserEntity의 비밀번호 업데이트
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