package com.web.memoire.user.jpa.repository;

import com.web.memoire.user.jpa.entity.UserEntity;

import java.util.Optional;


public interface UserRepositoryCustom {
    Optional<UserEntity> findByLoginId(String loginId);

}
