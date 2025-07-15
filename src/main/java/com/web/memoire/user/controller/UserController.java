package com.web.memoire.user.controller;

import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.jpa.entity.Token; // Token 엔티티가 아닌, DTO나 서비스에서 사용하는 Token 클래스라면 경로가 다를 수 있습니다.
// JWTUtil에서 사용하는 Token 클래스의 정확한 경로를 확인해주세요.
// 보통 com.web.memoire.security.jwt.model.dto.Token 이나 이런 식입니다.
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.user.jpa.entity.UserEntity; // ✅ UserEntity 임포트
import com.web.memoire.user.jpa.repository.UserRepository; // ✅ UserRepository 임포트
import com.web.memoire.user.model.dto.User;
import com.web.memoire.user.model.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository; // ✅ UserRepository 주입 추가

    private final BCryptPasswordEncoder bcryptPasswordEncoder;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final JWTUtil jwtUtil;
    private final TokenService tokenService;


    @PostMapping(value = "/idcheck")
    public ResponseEntity<String>dupCheckId(@RequestParam("loginId") String loginId) {
        log.info("/user/idcheck : " + loginId);

        boolean exists = userService.selectCheckId(loginId);
        return ResponseEntity.ok(exists ? "duplicated" : "ok");
    }

    @PostMapping("/signup")
    public ResponseEntity userInsertMethod(
            @RequestBody User user){
        log.info("/user/signup : " + user);

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            log.error("회원가입 실패: 비밀번호가 null이거나 비어있습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호를 입력해주세요.");
        }

        String newUserId = UUID.randomUUID().toString();
        user.setUserId(newUserId);

        user.setPassword(bcryptPasswordEncoder.encode(user.getPassword()));
        log.info("incoding : " + user.getPassword()+", length : " + user.getPassword().length());

        user.setRole("USER");
        log.info("userInsertMethod : " + user);

        try{
            userService.insertUser(user);
            return ResponseEntity.status(HttpStatus.OK).build();
        }catch(Exception e){
            log.error("회원가입 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }
    @PostMapping("/social")
    public ResponseEntity<?> requestSocialAuthorization(@RequestBody Map<String, String> payload) {
        String socialType = payload.get("socialType");
        if (socialType == null) {
            return ResponseEntity.badRequest().body("{\"error\":\"socialType is required\"}");
        }

        log.info("Requested socialType: {}", socialType);

        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(socialType);
        if (clientRegistration == null) {
            return ResponseEntity.badRequest().body("{\"error\":\"Invalid socialType: " + socialType + "\"}");
        }

        // ✅ 여기를 수정해야 합니다.
        // Spring Security의 기본 인가 엔드포인트는 /oauth2/authorization/{registrationId} 입니다.
        // 현재 애플리케이션이 실행되는 호스트와 포트를 포함하여 절대 경로를 생성합니다.
        // (실제 환경에서는 application.properties 등에서 baseUrl을 가져오는 것이 좋습니다.)
        String baseUrl = "http://localhost:8080"; // ✅ 백엔드 서버의 주소와 포트를 명시
        String authorizationUrl = baseUrl + "/oauth2/authorization/" + socialType;

        log.info("Generated authorization URL for {}: {}", socialType, authorizationUrl);

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("authorizationUrl", authorizationUrl);

        return ResponseEntity.ok(responseBody);
    }


    @PostMapping("/social/complete-signup")
    public ResponseEntity<?> completeSocialSignUp(@RequestBody SocialSignUpRequest request, HttpServletResponse response) {
        log.info("Completing social signup for userId: {}", request.getUserId());
        log.info("Received data: {}", request);

        try {
            // 1. 해당 userId의 UserEntity 조회
            UserEntity userEntity = userRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

            // 2. 받은 정보로 UserEntity 업데이트
            userEntity.setName(request.getName());
            userEntity.setNickname(request.getNickname());
            userEntity.setPhone(request.getPhone());

            // 생년월일 String -> Date 변환
            if (request.getBirthday() != null && !request.getBirthday().isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    userEntity.setBirthday(sdf.parse(request.getBirthday()));
                } catch (ParseException e) {
                    log.error("Failed to parse birthday: {}", request.getBirthday(), e);
                    return ResponseEntity.badRequest().body(Map.of("message", "잘못된 생년월일 형식입니다. YYYY-MM-DD 형식으로 입력해주세요."));
                }
            }

            // (선택 사항) loginId 업데이트
            // userEntity.setLoginId(request.getLoginId()); // 필요하다면 이 라인의 주석을 해제하세요.

            // 3. 업데이트된 UserEntity 저장
            userRepository.save(userEntity); // ✅ 직접 userRepository.save 호출
            log.info("UserEntity updated successfully for userId: {}", userEntity.getUserId());

            // 4. JWT 토큰 발급을 위해 UserEntity를 User DTO로 변환
            User userDto = userEntity.toDto(); // UserEntity에 toDto() 메서드가 구현되어 있어야 합니다.

            // 5. 역할 기반 권한 확인
            if(userDto.getRole().equals("BAD")){
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","불량유저로 등록되었습니다. 관리자에게 문의 하세요"));
            }
            if(userDto.getRole().equals("EXIT")){
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","탈퇴유저로 등록되었습니다. 관리자에게 문의 하세요."));
            }

            String accessToken = jwtUtil.generateToken(userDto, "access");
            String refreshToken = jwtUtil.generateToken(userDto, "refresh");

            // Assuming `Token` is `com.web.memoire.security.jwt.jpa.entity.Token`
            tokenService.saveRefreshToken(new Token(userDto.getUserId(), refreshToken));
            log.info("Tokens generated and refresh token saved for userId: {}", userDto.getUserId());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("accessToken", accessToken);
            responseBody.put("refreshToken", refreshToken);
            responseBody.put("userId", userDto.getUserId());
            responseBody.put("name", userDto.getName());
            responseBody.put("role", userDto.getRole());
            responseBody.put("autoLoginFlag", userDto.getAutoLoginFlag());
            responseBody.put("nickname", userDto.getNickname());

            return ResponseEntity.ok(responseBody);

        } catch (IllegalArgumentException e) {
            log.error("Complete social signup failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("An unexpected error occurred during social signup completion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "회원가입 완료 중 서버 오류가 발생했습니다."));
        }
    }

    @Getter
    @Setter
    public static class SocialSignUpRequest {
        private String userId;
        private String socialType;
        private String socialId;
        private String name;
        private String nickname;
        private String phone;
        private String birthday;
        private String loginId;
    }
}