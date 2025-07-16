package com.web.memoire.user.controller;

import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.jpa.entity.Token;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.User;
import com.web.memoire.user.model.service.UserService;
import com.web.memoire.user.util.GeneratePassword; // GeneratePassword 유틸리티 클래스 임포트
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
    private final UserRepository userRepository;

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
    public ResponseEntity<?> userInsertMethod(
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
    @PostMapping("/findid") // POST 요청을 /findid 경로로 매핑합니다.
    public ResponseEntity<?> findLoginIdByNameAndPhone(
            @RequestParam("name") String name,   // 'name'이라는 요청 파라미터를 String name 변수에 바인딩합니다.
            @RequestParam("phone") String phone) { // 'phone'이라는 요청 파라미터를 String phone 변수에 바인딩합니다.

        // 입력 유효성 검사: 이름이 null이거나 비어있는지 확인합니다.
        if (name == null || name.trim().isEmpty()) {
            log.error("이름 입력 필요: name={}", name); // 오류 로깅
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이름을 입력해주세요.");
        }

        // 입력 유효성 검사: 전화번호가 null이거나 비어있는지 확인합니다.
        if (phone == null || phone.trim().isEmpty()) {
            log.error("전화번호 입력 필요: phone={}", phone); // 오류 로깅
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("전화번호를 입력해주세요.");
        }

        try {
            // UserService를 통해 이름과 전화번호로 사용자 ID를 찾습니다.
            String loginId = userService.findLoginIdByNameAndPhone(name, phone);

            // 사용자 ID를 찾았는지 확인합니다.
            if (loginId != null) {
                log.info("사용자 ID 찾기 성공: name={}, phone={}, loginId={}", name, phone, loginId); // 성공 로깅
                // 사용자 ID를 응답 본문에 담아 200 OK 상태로 반환합니다.
                return ResponseEntity.status(HttpStatus.OK).body(loginId);
            } else {
                log.warn("사용자 ID를 찾을 수 없음: name={}, phone={}", name, phone); // 경고 로깅
                // 사용자 ID를 찾지 못한 경우 404 Not Found 상태로 응답합니다.
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("입력하신 정보와 일치하는 사용자 ID를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            // 아이디 찾기 과정에서 예외가 발생한 경우 오류를 로깅합니다.
            log.error("아이디 찾기 오류 발생: name={}, phone={}", name, phone, e);
            // 500 Internal Server Error 상태로 응답합니다.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("사용자 ID를 찾는 중 오류가 발생했습니다.");
        }
    }
    @PostMapping("/findpwd")
    public ResponseEntity<?> updatePassword(@RequestBody User user) {
        // 입력 유효성 검사: loginId가 null이거나 비어있는지 확인합니다.
        if (user.getLoginId() == null || user.getLoginId().isEmpty()) {
            log.error("아이디가 없습니다.: loginId={}", user.getLoginId()); // 오류 로깅
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("아이디를 다시 확인해주세요.");
        }
        // 입력 유효성 검사: phone이 null이거나 비어있는지 확인합니다.
        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            log.error("전화번호가 없습니다.: phone={}", user.getPhone()); // 오류 로깅
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("전화번호를 입력해주세요.");
        }

        try {
            // 1. 아이디와 전화번호로 사용자 조회
            User foundUser = userService.findUserByLoginIdAndPhone(user.getLoginId(), user.getPhone());

            if (foundUser != null) {
                String temporaryPassword = GeneratePassword.generateRandomPassword(8, 16);
                String encodedTemporaryPassword = bcryptPasswordEncoder.encode(temporaryPassword); // 암호화된 비밀번호

                // --- 추가된 로깅 ---
                log.info("Generated temporary password (plaintext): {}", temporaryPassword);
                log.info("Encoded temporary password (for DB storage): {}", encodedTemporaryPassword);
                // --- 끝 ---

                // 4. 사용자 비밀번호 업데이트 (BCryptPasswordEncoder를 사용하여 암호화)
                userService.updateUserPassword(foundUser.getUserId(), encodedTemporaryPassword);

                log.info("비밀번호 업데이트 성공: loginId={}", foundUser.getLoginId());

                // 성공 응답: 임시 비밀번호와 함께 메시지를 JSON 객체로 반환
                Map<String, String> responseBody = new HashMap<>();
                responseBody.put("message", "임시 비밀번호가 발급되었습니다. 자동으로 로그인합니다.");
                responseBody.put("temporaryPassword", temporaryPassword); // <-- 임시 비밀번호 추가
                return ResponseEntity.status(HttpStatus.OK).body(responseBody);

            } else {
                // 6. 해당 정보로 사용자를 찾을 수 없는 경우
                log.warn("사용자를 찾을 수 없음: loginId={}, phone={}", user.getLoginId(), user.getPhone()); // 경고 로깅
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("입력하신 정보와 일치하는 사용자를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            // 예외 발생 시 오류 로깅 및 500 Internal Server Error 응답
            log.error("비밀번호 업데이트 중 오류 발생: loginId={}, phone={}", user.getLoginId(), user.getPhone(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("비밀번호 업데이트 중 오류가 발생했습니다.");
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

        String baseUrl = "http://localhost:8080";
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
            // ✅ loginId와 password 업데이트 로직 제거
            // userEntity.setLoginId(request.getLoginId());
            // userEntity.setPassword(bcryptPasswordEncoder.encode(request.getPassword()));


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

            // 3. 업데이트된 UserEntity 저장
            userRepository.save(userEntity);
            log.info("UserEntity updated successfully for userId: {}", userEntity.getUserId());

            // 4. JWT 토큰 발급을 위해 UserEntity를 User DTO로 변환
            User userDto = userEntity.toDto();

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
    }
}
