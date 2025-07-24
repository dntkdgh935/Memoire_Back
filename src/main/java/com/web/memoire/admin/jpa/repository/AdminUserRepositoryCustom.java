package com.web.memoire.admin.jpa.repository;

import java.util.List;
import java.util.Map;

public interface AdminUserRepositoryCustom {
    List<Map<String, Object>> findDailyNewUserCounts(String startDateStr, String endDateStr);
}
