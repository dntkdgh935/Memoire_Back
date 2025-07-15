package com.web.memoire.security.model.service;

import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.entity.SocialUserEntity; // SocialUserEntity 임포트
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

    public CustomUserDetailsService(UserRepository userRepository, SocialUserRepository socialUserRepository) {
        this.userRepository = userRepository;
        this.socialUserRepository = socialUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // 'identifier'는 로그인 시 Spring Security가 전달하는 사용자 식별자입니다.
        // 이는 일반 로그인 시에는 loginId가 될 수 있고, 소셜 로그인 성공 후에는 우리 서비스의 userId가 될 수 있습니다.

        UserEntity userEntity = null;

        // 1. 먼저 UserRepository에서 직접 userId로 UserEntity를 찾아봅니다.
        //    (이는 소셜 로그인 후 CustomAuthenticationSuccessHandler에서 JWT를 발급할 때,
        //     UserEntity의 userId를 Spring Security의 username으로 사용하기 때문입니다.)
        Optional<UserEntity> userOpt = userRepository.findByUserId(identifier);

        if (userOpt.isPresent()) {
            userEntity = userOpt.get();
            log.info("DB에서 조회된 사용자 (userId): {}", userEntity.getUserId());
        } else {
            // 2. UserRepository에서 찾지 못했다면, SocialUserRepository를 통해 UserEntity를 찾아봅니다.
            //    (이 경우는 identifier가 SocialUserEntity의 userId와 매핑되는 경우를 처리합니다.)
            //    SocialUserRepository의 findByUserId는 SocialUserEntity를 반환하므로,
            //    여기서 SocialUserEntity를 찾은 후 다시 UserEntity를 조회해야 합니다.
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
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + identifier);
        }

        // UserDetails 객체 반환
        // 소셜 로그인 사용자의 경우 password 필드가 null일 수 있으므로 빈 문자열로 처리
        String password = userEntity.getPassword() != null ? userEntity.getPassword() : "";

        return User.builder()
                .username(userEntity.getUserId()) // Spring Security UserDetails의 username은 우리 서비스의 userId
                .password(password)
                .roles(userEntity.getRole()) // role은 UserEntity에 직접 Role이 있다면 String으로 반환 (ADMIN, USER 등)
                .build();
    }
}