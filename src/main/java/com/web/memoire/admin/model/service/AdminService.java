package com.web.memoire.admin.model.service; // admin 패키지로 변경

import com.web.memoire.user.jpa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository; // UserRepositoryCustomImpl을 통해 커스텀 메서드 사용

    /**
     * 특정 기간 동안의 일별 신규 가입자 수 통계를 가져옵니다.
     * @param startDate 조회 시작 날짜 (YYYY-MM-DD)
     * @param endDate 조회 종료 날짜 (YYYY-MM-DD)
     * @return 날짜별 신규 가입자 수 데이터를 담은 Map 리스트
     */
    public List<Map<String, Object>> getDailyNewUserStatistics(String startDate, String endDate) {
        log.info("일별 신규 가입자 통계 조회 요청: startDate={}, endDate={}", startDate, endDate);
        List<Map<String, Object>> result = userRepository.findDailyNewUserCounts(startDate, endDate);
        log.info("일별 신규 가입자 통계 조회 결과 ({}건): {}", result.size(), result);
        return result;
    }
}