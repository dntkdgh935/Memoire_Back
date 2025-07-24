package com.web.memoire.user.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.memoire.user.jpa.entity.UserEntity;
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
public class User {
    @NotNull
    private String userId;
    private String name;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private Date birthday;
    private String role;
    private String autoLoginFlag;
    private Date registrationDate;
    @NotNull
    private String loginId;
    private String nickname;
    private String phone;
    private String profileImagePath;
    private Integer sanctionCount;
    private String statusMessage;
    private String loginType; // loginType 필드 추가

    public UserEntity toEntity() {
        return UserEntity.builder()
                .userId(userId)
                .name(name)
                .birthday(birthday)
                .role(role)
                .autoLoginFlag(autoLoginFlag)
                .registrationDate(registrationDate)
                .loginId(loginId)
                .nickname(nickname)
                .phone(phone)
                .profileImagePath(profileImagePath)
                .sanctionCount(sanctionCount)
                .statusMessage(statusMessage)
                .loginType(loginType) // loginType 필드 추가
                .build();
    }
}
