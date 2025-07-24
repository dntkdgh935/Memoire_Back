package com.web.memoire.library.jpa.repository;

import com.web.memoire.common.entity.RelationshipEntity;
import com.web.memoire.common.entity.RelationshipId;
import com.web.memoire.common.entity.ReportEntity;
import com.web.memoire.common.entity.ReportId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibReportRepository extends JpaRepository<ReportEntity, ReportId> {
}
