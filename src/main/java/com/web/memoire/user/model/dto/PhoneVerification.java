package com.web.memoire.user.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.memoire.user.jpa.entity.PhoneVerificationEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PhoneVerification {
    private String phone;
    private String verificationCode;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date expirationTimestamp;

    public PhoneVerificationEntity toEntity() {
             return PhoneVerificationEntity.builder()
                 .phone(this.phone)
                 .verificationCode(this.verificationCode)
                 .expirationTimestamp(this.expirationTimestamp)
                 .build();
         }
}
