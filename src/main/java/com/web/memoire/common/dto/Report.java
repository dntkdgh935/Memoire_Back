package com.web.memoire.common.dto;

import com.web.memoire.common.entity.ReportEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
public class Report {

    @NotBlank
    private int memoryid;

    @NotBlank
    private String userid;

    private String reportReason;

    public ReportEntity toEntity() {
        return ReportEntity.builder()
                .memoryid(memoryid)
                .userid(userid)
                .reportReason(reportReason)
                .build();
    }
}
