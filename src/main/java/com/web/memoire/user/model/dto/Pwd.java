package com.web.memoire.user.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.memoire.user.jpa.entity.PwdEntity;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Pwd {
    @NotNull
    private String userId;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private Date chPwd;
    private String prevPwd;
    @NotNull
    private String currPwd;

    public PwdEntity toEntity() {
             return PwdEntity.builder()
                 .userId(this.userId)
                 .chPwd(this.chPwd)
                 .prevPwd(this.prevPwd)
                 .currPwd(this.currPwd)
                 .build();
         }
}
