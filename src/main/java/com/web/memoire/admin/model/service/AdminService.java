package com.web.memoire.admin.model.service;


import com.web.memoire.admin.jpa.repository.AdminCollectionLikeRepository;
import com.web.memoire.admin.jpa.repository.AdminCollectionRepository;
import com.web.memoire.admin.jpa.repository.AdminUserRepository;
import com.web.memoire.admin.jpa.repository.AdminMemoryRepository;
import com.web.memoire.admin.jpa.repository.AdminReportRepository;
import com.web.memoire.common.dto.Collection;
import com.web.memoire.common.dto.Memory;
import com.web.memoire.common.dto.Report;
import com.web.memoire.common.entity.ReportEntity;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminCollectionRepository adminCollectionRepository;
    private final AdminCollectionLikeRepository adminCollectionLikeRepository;
    private final AdminReportRepository adminReportRepository;
    private final AdminMemoryRepository adminMemoryRepository;


    /**
     * 특정 기간 동안의 일별 신규 가입자 수 통계를 가져옵니다.
     * @param startDate 조회 시작 날짜 (YYYY-MM-DD)
     * @param endDate 조회 종료 날짜 (YYYY-MM-DD)
     * @return 날짜별 신규 가입자 수 데이터를 담은 Map 리스트
     */
    public List<Map<String, Object>> getDailyNewUserStatistics(String startDate, String endDate) {
        log.info("일별 신규 가입자 통계 조회 요청: startDate={}, endDate={}", startDate, endDate);

        // 주입받은 'adminUserRepository' 인스턴스를 통해 메서드를 호출해야 합니다.
        List<Map<String, Object>> result = adminUserRepository.findDailyNewUserCounts(startDate, endDate);

        log.info("일별 신규 가입자 통계 조회 결과 ({}건): {}", result.size(), result);
        return result;
    }

    /**
     * 특정 기간 동안의 일별 신규 컬렉션 수 통계를 가져옵니다.
     * @param startDate 조회 시작 날짜 (YYYY-MM-DD)
     * @param endDate 조회 종료 날짜 (YYYY-MM-DD)
     * @return 날짜별 신규 컬렉션 수 데이터를 담은 Map 리스트
     */
    public List<Map<String, Object>> getDailyNewCollectionStatistics(String startDate, String endDate) {
        log.info("일별 신규 컬렉션 통계 조회 요청: startDate={}, endDate={}", startDate, endDate);
        List<Map<String, Object>> result = adminCollectionRepository.findDailyNewCollectionCounts(startDate, endDate);
        log.info("일별 신규 컬렉션 통계 조회 결과 ({}건): {}", result.size(), result);
        return result;
    }

    /**
     * 조회수가 가장 높은 컬렉션 목록을 가져옵니다.
     * @return 컬렉션 데이터를 담은 Map 리스트
     */
    public List<Map<String, Object>> getTopViewsCollections() {
        log.info("가장 많이 본 컬렉션 목록 조회 요청");
        // 커스텀 Repository 메서드 호출
        List<Map<String, Object>> result = adminCollectionRepository.findTopViewsCollections();
        log.info("가장 많이 본 컬렉션 목록 조회 결과 ({}건): {}", result.size(), result);
        return result;
    }
    public List<Map<String, Object>> getTopLikedCollections(int limit) {
        log.info("TOP 좋아요 컬렉션 조회 요청, limit={}", limit);

        // 1. 좋아요 리포지토리에서 좋아요 수가 많은 컬렉션 ID와 수를 가져옵니다.
        List<Map<String, Object>> topLikes = adminCollectionLikeRepository.findTopLikedCollectionsByCount(limit);

        List<Map<String, Object>> result = new ArrayList<>();

        // 2. 각 컬렉션 ID에 대해 컬렉션 상세 정보와 작성자 이름을 조회합니다.
        for (Map<String, Object> likeData : topLikes) {
            Integer collectionId = (Integer) likeData.get("collectionId");
            Long likesCount = (Long) likeData.get("likesCount");

            // 컬렉션 정보 조회
            adminCollectionRepository.findById(collectionId).ifPresent(collectionEntity -> {
                String authorId = collectionEntity.getAuthorid();

                // 작성자 이름 조회
                String authorName = adminUserRepository.findById(authorId)
                        .map(userEntity -> userEntity.getName())
                        .orElse("알 수 없음");

                Map<String, Object> collectionData = Map.of(
                        "collectionId", collectionId,
                        "collectionTitle", collectionEntity.getCollectionTitle(),
                        "authorName", authorName,
                        "likesCount", likesCount
                );
                result.add(collectionData);
            });
        }

        log.info("TOP 좋아요 컬렉션 조회 결과 ({}건): {}", result.size(), result);
        return result;
    }


    public User findUserById(String userid) {
        return userRepository.findById(userid)
                .map(UserEntity::toDto)
                .orElseThrow(() -> {
                    log.warn("사용자 조회 실패: userid '{}' 에 해당하는 사용자를 찾을 수 없습니다.", userid);
                    // 적절한 예외를 던집니다. 예를 들어, NoSuchElementException 또는 Custom Exception
                    return new NoSuchElementException("사용자를 찾을 수 없습니다.");
                });
    }

    public long getNumAllUsers() {
        return userRepository.count();
    }

    public long getNumAllReports() {
        return adminReportRepository.count();
    }

    public Page<User> getUsers(String search, Pageable pageable) {
        return userRepository.findByNicknameContainingIgnoreCase(search, pageable).map(UserEntity::toDto);
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserEntity::toDto);
    }

    public Page<Integer> findMemoryidByNumReports(Pageable pageable) {
        return adminReportRepository.findMemoryidByNumReports(pageable);
    }

    public Long getNumReports(int memoryid) {
        return adminReportRepository.getNumReports(memoryid);
    }

    public Memory findByMemoryid(int memoryid) {
        return adminMemoryRepository.findByMemoryid(memoryid).toDto();
    }

    public Collection findByCollectionid(int collectionid) {
        return adminCollectionRepository.findByCollectionid(collectionid).toDto();
    }

    public List<Report> findReportByMemoryid(int memoryid) {
        List<ReportEntity> entityList = adminReportRepository.findByMemoryid(memoryid);
        List<Report> reportList = new ArrayList<>();
        for (ReportEntity entity : entityList) {
            reportList.add(entity.toDto());
        }
        return reportList;
    }

    public int banUser(String userid, String role) {
        UserEntity user = findUserById(userid).toEntity();
        user.setRole(role);
        if (role.equals("BAD")) {
            user.setSanctionCount(user.getSanctionCount() + 1);
        }
        return userRepository.save(user) == null ? 0 : 1;
    }

    public int adminUser(String userid, String role) {
        UserEntity user = findUserById(userid).toEntity();
        user.setRole(role);
        return userRepository.save(user) == null ? 0 : 1;
    }



}