package com.web.memoire.user.jpa.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.entity.PwdEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.web.memoire.user.jpa.entity.QUserEntity.userEntity;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager; // EntityManager는 직접 사용하지 않는다면 제거해도 됩니다.

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

    @Override
    public UserEntity updateUserPassword(String userId, String encode) {
        long affectedRows = queryFactory
                .update(userEntity) // userEntity (TB_USER 테이블)를 업데이트합니다.
                .set(userEntity.password, encode) // password 필드를 새롭게 인코딩된 비밀번호로 설정합니다.
                .where(userEntity.userId.eq(userId)) // userId가 주어진 값과 같은 조건을 설정합니다.
                .execute(); // 업데이트 쿼리를 실행하고 영향을 받은 행의 수를 반환합니다.

        // 업데이트된 행이 있다면, 업데이트된 UserEntity를 다시 조회하여 반환합니다.
        if (affectedRows > 0) {
            // findByUserId 메소드를 사용하여 업데이트된 UserEntity를 조회합니다.
            return findByUserId(userId).orElse(null);
        }
        // 업데이트된 행이 없거나 사용자를 찾을 수 없는 경우 null을 반환합니다.
        return null;
    }

}
