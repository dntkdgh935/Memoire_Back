package com.web.memoire.admin.jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AdminCollectionRepositoryCustomImpl implements AdminCollectionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Map<String, Object>> findDailyNewCollectionCounts(String startDateStr, String endDateStr) {
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
        // CollectionEntity의 createdDate 필드가 Date 타입이므로, 해당 필드를 사용합니다.
        String sql = "SELECT TO_CHAR(ce.CREATED_DATE, 'YYYY-MM-DD') AS creation_day, COUNT(ce.COLLECTIONID) AS collection_count " +
                "FROM TB_COLLECTION ce " +
                "WHERE ce.CREATED_DATE BETWEEN :startDate AND :endDate " +
                "GROUP BY TO_CHAR(ce.CREATED_DATE, 'YYYY-MM-DD') " +
                "ORDER BY creation_day ASC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> dailyCounts = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", row[0]); // creation_day (String)
            entry.put("newCollections", ((Number) row[1]).intValue()); // collection_count (Number -> int)
            dailyCounts.add(entry);
        }
        return dailyCounts;
    }
    @Override
    public List<Map<String, Object>> findTopViewsCollections() {
        // Oracle DB에 맞는 SQL 쿼리로 수정합니다.
        String correctedSql = "SELECT c.COLLECTIONID, c.COLLECTION_TITLE, c.READ_COUNT, c.CREATED_DATE " +
                "FROM TB_COLLECTION c " +
                "ORDER BY c.READ_COUNT DESC " +
                "FETCH FIRST 10 ROWS ONLY";

        Query query = entityManager.createNativeQuery(correctedSql);

        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> topCollections = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", row[0]);
            entry.put("name", row[1]);
            entry.put("views", row[2]);
            entry.put("createdDate", row[3].toString()); // 날짜를 문자열로 변환하여 사용
            topCollections.add(entry);
        }

        return topCollections;
    }
}