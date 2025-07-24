package com.web.memoire.admin.jpa.repository;

import com.web.memoire.common.entity.ReportEntity;
import com.web.memoire.common.entity.ReportId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminReportRepository extends JpaRepository<ReportEntity, ReportId> {

    // 메모리 신고 수 기준으로 조회
    @Query("SELECT r.memoryid FROM ReportEntity r GROUP BY r.memoryid ORDER BY count(r) DESC")
    Page<Integer> findMemoryidByNumReports(Pageable pageable);

    // 메모리 신고 수 조회
    @Query("SELECT count(r) FROM ReportEntity r WHERE r.memoryid = :memoryid")
    long getNumReports(int memoryid);

    // 메모리 신고 조회
    List<ReportEntity> findByMemoryid(int memoryid);

}
