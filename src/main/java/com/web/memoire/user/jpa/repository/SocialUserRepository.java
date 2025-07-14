package com.web.memoire.user.jpa.repository;

import com.web.memoire.user.jpa.entity.SocialUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SocialUserRepository extends JpaRepository<SocialUserEntity, String> {
    Optional<SocialUserEntity> findBySocialIdAndSocialType(String socialId, String socialType);
    Optional<SocialUserEntity> findByUserId(String userId); // userId로도 조회 가능하도록 추가
}