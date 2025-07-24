package com.web.memoire.admin.controller; // admin 패키지로 변경

import com.web.memoire.admin.model.dto.MemoryReports;
import com.web.memoire.admin.model.dto.ReportDetail;
import com.web.memoire.admin.model.service.AdminService;
import com.web.memoire.common.dto.Collection;
import com.web.memoire.common.dto.Memory;
import com.web.memoire.common.dto.Report;
import com.web.memoire.user.model.dto.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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

    @GetMapping("/totalUsers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getNumTotalUsers() {
        log.info("AdminController.getNumTotalUsers...");
        try {
            return ResponseEntity.ok(adminService.getNumAllUsers());
        } catch (Exception e) {
            log.error("AdminController.getNumTotalUsers error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/totalUsers 에러");
        }
    }

    @GetMapping("/reportedPosts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getNumReportedPosts() {
        log.info("AdminController.getNumReportedPosts...");
        try {
            return ResponseEntity.ok(adminService.getNumAllReports());
        } catch (Exception e) {
            log.error("AdminController.getNumReportedPosts error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/getNumReportedPosts 에러");
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsersInformation(@RequestParam(required = false, defaultValue = "") String search, @PageableDefault(size = 50, sort = "loginId", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Map<String, Object> map = new HashMap<>();
            Page<User> page = null;
            if (search == null || search.trim().isEmpty()) {
                page = adminService.getAllUsers(pageable);
            } else {
                page = adminService.getUsers(search, pageable);
            }
            log.info(page.getContent().toString());
            map.put("content", page.getContent());
            map.put("totalPages", page.getTotalPages());
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error("AdminController.getUsersInformation error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/users 에러");

        }
    }

    @PostMapping("/banUser")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> banUser(@RequestParam String userid, @RequestParam String role) {
        log.info("AdminController.banUser...");
        try {
            if (adminService.banUser(userid, role) > 0) {
                return ResponseEntity.ok("저장 성공");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/banUser 에러");

        } catch (Exception e) {
            log.error("AdminController.banUser: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/banUser 에러");
        }
    }

    @PostMapping("/makeAdmin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminUser(@RequestParam String userid, @RequestParam String role) {
        log.info("AdminController.adminUser...");
        try {
            if (adminService.adminUser(userid, role) > 0) {
                return ResponseEntity.ok("저장 성공");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/makeAdmin 에러");

        } catch (Exception e) {
            log.error("AdminController.adminUser: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/makeAdmin 에러");
        }
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getReportsInformation(@PageableDefault(size = 50, direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("AdminController.getReportsInformation...");
        try {
            Map<String, Object> map = new HashMap<>();
            List<MemoryReports> reportsList = new ArrayList<>();
            Page<Integer> list = adminService.findMemoryidByNumReports(pageable);
            for (int memoryid : list) {
                MemoryReports mr = new MemoryReports();
                mr.setMemoryid(memoryid);
                Memory tempOne = adminService.findByMemoryid(memoryid);
                mr.setCollectionid(tempOne.getCollectionid());
                mr.setTitle(tempOne.getTitle());
                mr.setReportcount(adminService.getNumReports(memoryid));
                Collection tempTwo = adminService.findByCollectionid(tempOne.getCollectionid());
                mr.setNickname(adminService.findUserById(tempTwo.getAuthorid()).getNickname());
                reportsList.add(mr);
            }
            map.put("content", reportsList);
            map.put("totalPages", list.getTotalPages());
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error("AdminController.getReportsInformation error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/reports 에러");
        }
    }

    @GetMapping("/reportDetail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getReportDetail(@RequestParam int memoryid) {
        log.info("AdminController.getReportDetail...");
        try {
            List<ReportDetail> reportDetail = new ArrayList<>();
            List<Report> reportList = adminService.findReportByMemoryid(memoryid);
            for (Report report : reportList) {
                ReportDetail rd = new ReportDetail();
                rd.setReportReason(report.getReportReason());
                rd.setNickname(adminService.findUserById(report.getUserid()).getNickname());
                reportDetail.add(rd);
            }
            return ResponseEntity.ok(reportDetail);
        } catch (Exception e) {
            log.error("AdminController.getReportDetail error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/reportDetail 에러");
        }
    }

}