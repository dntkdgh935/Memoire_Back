package com.web.memoire.security.model.service;

import com.web.memoire.user.jpa.entity.PwdEntity;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.entity.SocialUserEntity; // SocialUserEntity 임포트
import com.web.memoire.user.jpa.repository.PwdRepository;
import com.web.memoire.user.jpa.repository.SocialUserRepository;
import com.web.memoire.user.jpa.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SocialUserRepository socialUserRepository;
    private final PwdRepository pwdRepository;

    public CustomUserDetailsService(UserRepository userRepository, SocialUserRepository socialUserRepository, PwdRepository pwdRepository) {
        this.userRepository = userRepository;
        this.socialUserRepository = socialUserRepository;
        this.pwdRepository = pwdRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        UserEntity userEntity = null;

        // 1. 먼저 UserRepository에서 직접 userId로 UserEntity를 찾아봅니다.
        Optional<UserEntity> userOpt = userRepository.findByUserId(identifier);

        if (userOpt.isPresent()) {
            userEntity = userOpt.get();
            log.info("DB에서 조회된 사용자 (userId): {}", userEntity.getUserId());
        } else {
            // 2. UserRepository에서 찾지 못했다면, SocialUserRepository를 통해 UserEntity를 찾아봅니다.
            Optional<SocialUserEntity> socialUserOpt = socialUserRepository.findByUserId(identifier);

            if (socialUserOpt.isPresent()) {
                SocialUserEntity socialUser = socialUserOpt.get();
                // SocialUserEntity에서 우리 서비스의 userId를 얻어 다시 UserRepository에서 UserEntity를 조회
                userEntity = userRepository.findByUserId(socialUser.getUserId())
                        .orElseThrow(() -> {
                            log.warn("SocialUserEntity는 찾았으나 연결된 UserEntity를 찾을 수 없습니다: {}", socialUser.getUserId());
                            return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + identifier + " (연결된 UserEntity 없음)");
                        });
                log.info("DB에서 조회된 소셜 사용자 (userId): {}", userEntity.getUserId());
            }
        }

        // 3. 최종적으로 UserEntity를 찾지 못했다면 예외 발생
        if (userEntity == null) {
            log.warn("사용자를 찾을 수 없습니다: {}", identifier);
            throw new UsernameNotFoundException("아이디 또는 비밀번호를 찾을 수 없습니다.");
        }

        // 4. 비밀번호 조회 (TB_PWD_HISTORY에서 최신 비밀번호 가져오기)
        String password = "";
        Optional<PwdEntity> latestPwdOpt = pwdRepository.findLatestByUserId(userEntity.getUserId());

        if (latestPwdOpt.isPresent()) {
            password = latestPwdOpt.get().getCurrPwd();
            log.info("비밀번호 이력에서 비밀번호 조회 성공: userId={}", userEntity.getUserId());
        } else {
            log.warn("사용자 {}의 비밀번호 이력을 찾을 수 없습니다. (소셜 로그인 사용자일 가능성)", userEntity.getUserId());
        }

        // UserDetails 객체 반환
        return User.builder()
                .username(userEntity.getUserId())
                .password(password)
                .roles(userEntity.getRole())
                .build();
    }
}