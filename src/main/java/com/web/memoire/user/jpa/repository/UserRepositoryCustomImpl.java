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
    public UserEntity findByUserid(String userId) {

        return queryFactory
                .selectFrom(userEntity)
                .where(userEntity.userId.eq(userId))
                .fetchOne();
    }
}
