package com.web.memoire.common.entity;

import com.web.memoire.common.dto.Report;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="TB_REPORT")
@Entity
@IdClass(ReportId.class)
public class ReportEntity {

    @Id
    @Column(name="MEMORYID", nullable = false)
    private int memoryid;

    @Id
    @Column(name="USERID", nullable = false)
    private String userid;

    @Column(name="REPORT_REASON", length = 200)
    private String reportReason;

    public Report toDto() {
        return Report.builder()
                .memoryid(memoryid)
                .userid(userid)
                .reportReason(reportReason)
                .build();
    }
}
