package com.web.memoire.user.jpa.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.web.memoire.user.jpa.entity.UserEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.web.memoire.user.jpa.entity.QUserEntity.userEntity;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public Optional<UserEntity> findByLoginId(String loginId) {

        UserEntity result= queryFactory
                .selectFrom(userEntity)
                .where(userEntity.loginId.eq(loginId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<UserEntity> findByUserId(String UserId) {
        UserEntity result= queryFactory
                .selectFrom(userEntity)
                .where(userEntity.loginId.eq(UserId))
                .fetchOne();
        return Optional.ofNullable(result);
    }
}
