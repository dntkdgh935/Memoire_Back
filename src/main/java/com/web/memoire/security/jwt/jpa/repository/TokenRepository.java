package com.web.memoire.security.jwt.jpa.repository;

import com.web.memoire.security.jwt.jpa.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface TokenRepository extends JpaRepository<Token, String> {

    // userId로 tokenValue 조회
    @Query("SELECT r.tokenId FROM Token r WHERE r.userId = :userId")
    String findTokenValueByUserId(@Param("userId") String userId);

    // userId 와 refreshToken 값으로 ID 조회
    @Query("SELECT r.userId FROM Token r WHERE r.userId = :userId AND r.tokenId = :tokenId")
    String findByUserIdAndTokenId(@Param("userId") String userId, @Param("tokenId") String tokenId);

    // id 로 토큰 갱신 (JPQL 로 update 쿼리 작성시에는 반드시 @Modifying 필수 표기할 것)
    @Modifying  // 필수
    @Query("UPDATE Token r SET r.tokenId = :tokenValue WHERE r.userId = :userId")
    int updateTokenById(@Param("id") String id, @Param("tokenValue") String tokenValue);
}
