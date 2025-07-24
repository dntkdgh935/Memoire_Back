package com.web.memoire.admin.jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap; // 순서 유지를 위해 LinkedHashMap 사용

@Repository
public class AdminUserRepositoryCustomImpl implements AdminUserRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

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
