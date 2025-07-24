package com.web.memoire.user.jpa.repository;

import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.model.dto.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<UserEntity, String>, UserRepositoryCustom {

    Optional<UserEntity> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    Optional<UserEntity> findByUserId(String userId);

    Optional<UserEntity> findLoginIdByNameAndPhone(String name, String phone);

    User findUserByLoginIdAndPhone(@NotNull String loginId, String phone);

    Optional<UserEntity> findPasswordByUserId(@NotNull String password);


    UserEntity updateUserPassword(@NotNull String userId, String encode);

    Optional<UserEntity> findByLoginIdAndPhone(@NotNull String loginId, String phone);
    
    boolean existsByPhone(String phone);

    Page<UserEntity> findByNicknameContainingIgnoreCase(String search, Pageable pageable);
}
