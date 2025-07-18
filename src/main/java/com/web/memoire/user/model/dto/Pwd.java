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
    private Long id; // 추가: PwdEntity의 ID에 맞춤
    @NotNull
    private String userId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul") // 시간 정보 포함
    private Date chPwd;
    private String prevPwd;
    @NotNull
    private String currPwd;

    public PwdEntity toEntity() {
        return PwdEntity.builder()
                .id(this.id) // ID도 매핑 (새로운 이력 생성 시에는 null로 전달)
                .userId(this.userId)
                .chPwd(this.chPwd)
                .prevPwd(this.prevPwd)
                .currPwd(this.currPwd)
                .build();
    }
}