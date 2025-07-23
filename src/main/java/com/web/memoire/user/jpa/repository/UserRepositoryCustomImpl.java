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
    @Override
    public List<Map<String, Object>> findDailyNewUserCounts(String startDateStr, String endDateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate;
        Date endDate;
        try {
            startDate = sdf.parse(startDateStr);
            endDate = sdf.parse(endDateStr);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD.");
        }

        // Oracle의 TO_CHAR 함수를 사용하여 날짜를 YYYY-MM-DD 형식의 문자열로 변환하고 그룹화합니다.
        // JPA/Hibernate가 Date 객체를 JDBC Date로 변환하기 때문에, SQL 함수를 직접 사용하여 날짜 포맷팅을 처리합니다.
        String sql = "SELECT TO_CHAR(ue.REGISTRATION_DATE, 'YYYY-MM-DD') AS registration_day, COUNT(ue.USERID) AS user_count " +
                "FROM TB_USER ue " +
                "WHERE ue.REGISTRATION_DATE BETWEEN :startDate AND :endDate " +
                "GROUP BY TO_CHAR(ue.REGISTRATION_DATE, 'YYYY-MM-DD') " +
                "ORDER BY registration_day ASC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> dailyCounts = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> entry = new LinkedHashMap<>(); // 순서 유지를 위해 LinkedHashMap 사용
            entry.put("date", row[0]); // registration_day (String)
            entry.put("newUsers", ((Number) row[1]).intValue()); // user_count (Number -> int)
            dailyCounts.add(entry);
        }
        return dailyCounts;
    }

}
