package com.web.memoire.user.jpa.repository;

import com.web.memoire.user.jpa.entity.UserEntity;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;


public interface UserRepositoryCustom {
    Optional<UserEntity> findByLoginId(String loginId);

    Optional<UserEntity> findByUserId(String userId);


    UserEntity updateUserPassword(@NotNull String userId, String encode);
}
