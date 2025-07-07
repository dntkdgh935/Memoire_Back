package com.web.memoire.user.jpa.repository;

import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.model.dto.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<UserEntity, String>, UserRepositoryCustom {

    Optional<UserEntity> findByUserId(String userId);
}
