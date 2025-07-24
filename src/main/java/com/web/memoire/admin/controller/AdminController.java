package com.web.memoire.admin.controller; // admin 패키지로 변경

import com.web.memoire.admin.model.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 역할 기반 권한 부여를 위해 추가
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin // 필요하다면, 특정 출처만 허용하도록 origin 설정
@RequestMapping("/admin") // 관리자 통계 전용 엔드포인트
public class AdminController {

    private final AdminService adminService;

    /**
     * 일별 신규 가입자 수 통계를 제공하는 API 엔드포인트.
     * @param startDate 조회 시작 날짜 (YYYY-MM-DD), 기본값은 7일 전
     * @param endDate 조회 종료 날짜 (YYYY-MM-DD), 기본값은 오늘
     * @return 날짜별 신규 가입자 수 데이터를 담은 ResponseEntity
     */
    @GetMapping("/new-users-daily")
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 역할만 접근 가능하도록 설정
    public ResponseEntity<List<Map<String, Object>>> getDailyNewUserStatistics(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        // 기본 날짜 설정: 시작일은 오늘부터 7일 전, 종료일은 오늘
        LocalDate defaultEndDate = LocalDate.now();
        LocalDate defaultStartDate = defaultEndDate.minusDays(6); // 7일치 데이터를 위해 오늘 포함 6일 전

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String actualStartDate = (startDate != null && !startDate.isEmpty()) ? startDate : defaultStartDate.format(formatter);
        String actualEndDate = (endDate != null && !endDate.isEmpty()) ? endDate : defaultEndDate.format(formatter);

        log.info("ADMIN: 일별 신규 가입자 통계 요청 수신 - 시작일: {}, 종료일: {}", actualStartDate, actualEndDate);

        try {
            // adminService.getDailyNewUserStatistics 메서드는 실제 데이터를 조회해야 합니다.
            // 여기서는 임시 데이터를 반환합니다.
            List<Map<String, Object>> statistics = adminService.getDailyNewUserStatistics(actualStartDate, actualEndDate);
            return ResponseEntity.ok(statistics);
        } catch (IllegalArgumentException e) {
            log.warn("ADMIN: 유효하지 않은 날짜 형식 - {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("ADMIN: 일별 신규 가입자 통계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 일별 신규 컬렉션 수 통계를 제공하는 API 엔드포인트.
     * @param startDate 조회 시작 날짜 (YYYY-MM-DD), 기본값은 7일 전
     * @param endDate 조회 종료 날짜 (YYYY-MM-DD), 기본값은 오늘
     * @return 날짜별 신규 컬렉션 수 데이터를 담은 ResponseEntity
     */
    @GetMapping("/new-collections-daily")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getDailyNewCollectionStatistics(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        LocalDate defaultEndDate = LocalDate.now();
        LocalDate defaultStartDate = defaultEndDate.minusDays(6);

        // 'yyyy-MM-DD'를 'yyyy-MM-dd'로 수정했습니다.
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String actualStartDate = (startDate != null && !startDate.isEmpty()) ? startDate : defaultStartDate.format(formatter);
        String actualEndDate = (endDate != null && !endDate.isEmpty()) ? endDate : defaultEndDate.format(formatter);

        log.info("ADMIN: 일별 신규 컬렉션 통계 요청 수신 - 시작일: {}, 종료일: {}", actualStartDate, actualEndDate);

        try {
            // adminService.getDailyNewCollectionStatistics 메서드는 실제 데이터를 조회해야 합니다.
            // 여기서는 임시 데이터를 반환합니다.
            List<Map<String, Object>> statistics = adminService.getDailyNewCollectionStatistics(actualStartDate, actualEndDate);
            return ResponseEntity.ok(statistics);
        } catch (IllegalArgumentException e) {
            log.warn("ADMIN: 유효하지 않은 날짜 형식 - {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("ADMIN: 일별 신규 컬렉션 통계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * GET /admin/collections/top-views
     * 관리자 권한으로 조회수가 가장 높은 컬렉션 목록을 가져오는 API
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/collections/top-views")
    public ResponseEntity<List<Map<String, Object>>> getTopViewsCollections() {
        List<Map<String, Object>> topCollections = adminService.getTopViewsCollections();
        return ResponseEntity.ok(topCollections);
    }

    @GetMapping("/top-likes-collections")
    public ResponseEntity<List<Map<String, Object>>> getTopLikedCollections(
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> collections = adminService.getTopLikedCollections(limit); // 서비스 로직으로 전달
        return ResponseEntity.ok(collections);
    }
}