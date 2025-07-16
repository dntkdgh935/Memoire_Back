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

    @Column(name="PASSWORD", nullable = false, length = 100)
    private String password;

    @Column(name="ROLE", length = 20)
    private String role;

    @Column(name="AUTO_LOGIN_FLAG", length = 2)
    private String autoLoginFlag;

    @Column(name="AUTO_LOGIN_TOKEN", length = 255)
    private String autoLoginToken;

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

    @Column(name="FACE_LOGIN_USE", length = 1)
    private String faceLoginUse;

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
        if (faceLoginUse == null) {
            faceLoginUse = "N"; // Default 값 설정
        }
    }

    public User toDto() {
        return User.builder()
                .userId(userId)
                .name(name)
                .birthday(birthday)
                .password(password)
                .role(role)
                .autoLoginFlag(autoLoginFlag)
                .autoLoginToken(autoLoginToken)
                .registrationDate(registrationDate)
                .loginId(loginId)
                .nickname(nickname)
                .phone(phone)
                .profileImagePath(profileImagePath)
                .sanctionCount(sanctionCount)
                .statusMessage(statusMessage)
                .faceLoginUse(faceLoginUse)
                .build();
    }


}
