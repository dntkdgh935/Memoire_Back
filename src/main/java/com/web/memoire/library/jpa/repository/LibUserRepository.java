package com.web.memoire.library.jpa.repository;

import com.web.memoire.user.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LibUserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUserId(String userId);

//    String findUserNameByUserId(String userId);
}
