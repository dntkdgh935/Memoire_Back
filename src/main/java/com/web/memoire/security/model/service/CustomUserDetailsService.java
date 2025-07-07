package com.web.memoire.security.model.service;

import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
        // 1. DB에서 사용자 조회
        // 2. UserDetails 타입으로 반환

        UserEntity userEntity = userRepository.findByUserId(name)
                .orElseThrow(() -> {
                    System.out.println("사용자를 찾을 수 없습니다: " + name);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + name);
                });

        log.info("DB에서 조회된 사용자: " + userEntity);

        return User.builder()
                .username(userEntity.getUserId())
                .password(userEntity.getPassword())
                .roles(userEntity.getRole().equals("ADMIN") ? "ADMIN" : "USER")
                .build();
    }
}
