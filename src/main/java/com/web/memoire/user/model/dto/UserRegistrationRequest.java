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
public class UserRegistrationRequest {
    // userId는 컨트롤러에서 생성될 예정이므로 @NotNull 제거
    private String userId;
    private String name;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private Date birthday;
    @NotNull // 회원가입 시 비밀번호는 필수
    private String password; // 평문 비밀번호 (요청 시 사용)
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
    private String loginType;
}
