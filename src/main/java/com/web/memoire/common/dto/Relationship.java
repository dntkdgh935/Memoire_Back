package com.web.memoire.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.memoire.common.entity.RelationshipEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
public class Relationship {

    @NotBlank
    private String userid;

    @NotBlank
    private String targetid;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date followDate;

    @NotBlank
    private String status;

    public RelationshipEntity toEntity() {
        return RelationshipEntity.builder()
                .userid(userid)
                .targetid(targetid)
                .followDate(followDate)
                .status(status)
                .build();
    }


}
