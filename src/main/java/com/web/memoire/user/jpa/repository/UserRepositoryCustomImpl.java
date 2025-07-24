package com.web.memoire.user.jpa.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.web.memoire.user.jpa.entity.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.web.memoire.user.jpa.entity.QUserEntity.userEntity;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public Optional<UserEntity> findByLoginId(String loginId) {
        // loginId가 null인 경우 eq(null) 오류를 방지하기 위해 Optional.empty() 반환
        if (loginId == null) {
            return Optional.empty();
        }

        UserEntity result= queryFactory
                .selectFrom(userEntity)
                .where(userEntity.loginId.eq(loginId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<UserEntity> findByUserId(String userId) {
        // userId가 null인 경우 eq(null) 오류를 방지하기 위해 Optional.empty() 반환
        if (userId == null) {
            return Optional.empty();
        }

        UserEntity result= queryFactory
                .selectFrom(userEntity)
                .where(userEntity.userId.eq(userId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

}
