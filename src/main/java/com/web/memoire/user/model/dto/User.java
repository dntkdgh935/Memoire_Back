package com.web.memoire.user.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @NotNull
    private String password;
    private String role;
    private String autoLoginFlag;
    private String autoLoginToken;
    private Date registrationDate;
    @NotNull
    private String loginId;
    private String nickname;
    private String phone;
    private String profileImagePath;
    private Integer sanctionCount;
    private String statusMessage;
    private String faceLoginUse;

    public User toEntity() {
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
