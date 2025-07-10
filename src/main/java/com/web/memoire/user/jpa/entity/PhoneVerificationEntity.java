package com.web.memoire.user.jpa.entity;

import com.web.memoire.user.model.dto.PhoneVerification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "TB_PHONE_VERIFICATION")
@Entity
public class PhoneVerificationEntity {
    @Id
    @Column(name="PHONE", length = 100)
    private String phone;

    @Column(name="VERIFICATION_CODE", nullable = false, length = 10)
    private String verificationCode;

    @Column(name="EXPIRATION_TIMESTAMP", nullable = false)
    private Date expirationTimestamp;

    public PhoneVerification toEntity() {
        return PhoneVerification.builder()
                .phone(this.phone)
                .verificationCode(this.verificationCode)
                .expirationTimestamp(this.expirationTimestamp)
                .build();
    }
}
