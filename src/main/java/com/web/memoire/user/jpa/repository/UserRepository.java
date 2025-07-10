package com.web.memoire.user.jpa.repository;

import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.model.dto.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<UserEntity, String>, UserRepositoryCustom {

    Optional<UserEntity> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    Optional<UserEntity> findByUserId(String UserId);

}
