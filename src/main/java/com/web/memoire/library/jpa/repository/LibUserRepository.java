package com.web.memoire.library.jpa.repository;

import com.web.memoire.user.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LibUserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUserId(String userId);

    // UserEntity에서 nickname만 가져오는 메소드
    @Query("SELECT u.nickname FROM UserEntity u WHERE u.userId = :userId")
    String findNicknameByUserId(@Param("userId") String userId);

    @Query("SELECT u.profileImagePath FROM UserEntity u WHERE u.userId = :userId")
    String findProfileImagePathByUserId(@Param("userId") String userId);


    List<UserEntity> findByNicknameContaining(String query);


//    String findUserNameByUserId(String userId);
}
