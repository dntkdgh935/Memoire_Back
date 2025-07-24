package com.web.memoire.admin.jpa.repository;

import java.util.List;
import java.util.Map;

public interface AdminCollectionRepositoryCustom {
    List<Map<String, Object>> findDailyNewCollectionCounts(String startDate, String endDate);
    List<Map<String, Object>> findTopViewsCollections();
}