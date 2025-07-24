package com.web.memoire.user.jpa.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.memoire.user.model.dto.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.GregorianCalendar;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "TB_USER")
@Entity
public class UserEntity {
    @Id
    @Column(name="USERID", length = 50)
    private String userId;

    @Column(name="NAME", length = 20)
    private String name;

    @Column(name="BIRTHDAY")
    private Date birthday;

    // PASSWORD 컬럼 제거됨

    @Column(name="ROLE", length = 20)
    private String role;

    @Column(name="AUTO_LOGIN_FLAG", length = 2)
    private String autoLoginFlag;

    // AUTO_LOGIN_TOKEN 컬럼 제거됨

    @Column(name="REGISTRATION_DATE", nullable = false)
    private Date registrationDate;

    @Column(name="LOGINID", nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(name="NICKNAME", length = 50)
    private String nickname;

    @Column(name="PHONE", length = 100)
    private String phone;

    @Column(name="PROFILE_IMAGE_PATH", length = 200)
    private String profileImagePath;

    @Column(name="SANCTION_COUNT", nullable = false)
    private Integer sanctionCount;

    @Column(name="STATUS_MESSAGE", length = 200)
    private String statusMessage;

    // FACE_LOGIN_USE 컬럼 제거됨

    @Column(name="LOGIN_TYPE", length = 20) // LOGIN_TYPE 컬럼 추가
    private String loginType;

    @PrePersist
    public void prePersist() {
        if (registrationDate == null) {
            registrationDate = new Date(); // 현재 시간으로 설정
        }
        if (autoLoginFlag == null) {
            autoLoginFlag = "N"; // Default 값 설정
        }
        if (sanctionCount == null) {
            sanctionCount = 0; // Default 값 설정
        }
        // faceLoginUse 관련 로직 제거됨
        // login_type은 SQL에서 기본값이 없으므로 여기서는 별도 설정하지 않음
    }

    public User toDto() {
        return User.builder()
                .userId(userId)
                .name(name)
                .birthday(birthday)
                // .password(password) // password 필드 제거됨
                .role(role)
                .autoLoginFlag(autoLoginFlag)
                // .autoLoginToken(autoLoginToken) // autoLoginToken 필드 제거됨
                .registrationDate(registrationDate)
                .loginId(loginId)
                .nickname(nickname)
                .phone(phone)
                .profileImagePath(profileImagePath)
                .sanctionCount(sanctionCount)
                .statusMessage(statusMessage)
                // .faceLoginUse(faceLoginUse) // faceLoginUse 필드 제거됨
                .loginType(loginType) // loginType 필드 추가
                .build();
    }


}
