package com.web.memoire.user.controller;

import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.jpa.entity.Token;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.User; // User DTO 임포트
import com.web.memoire.user.model.dto.Pwd; // Pwd DTO 임포트
import com.web.memoire.user.model.service.UserService;
import com.web.memoire.user.util.GeneratePassword;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Date;
import java.util.NoSuchElementException; // NoSuchElementException 임포트 추가

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
    public ResponseEntity<String> dupCheckId(@RequestParam("loginId") String loginId) {
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

    @PostMapping("/findid")
    public ResponseEntity<?> findLoginIdByNameAndPhone(
            @RequestParam("name") String name,
            @RequestParam("phone") String phone) {

        if (name == null || name.trim().isEmpty()) {
            log.error("이름 입력 필요: name={}", name);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이름을 입력해주세요.");
        }

        if (phone == null || phone.trim().isEmpty()) {
            log.error("전화번호 입력 필요: phone={}", phone);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("전화번호를 입력해주세요.");
        }

        try {
            String loginId = userService.findLoginIdByNameAndPhone(name, phone);

            if (loginId != null) {
                log.info("사용자 ID 찾기 성공: name={}, phone={}, loginId={}", name, phone, loginId);
                return ResponseEntity.status(HttpStatus.OK).body(loginId);
            } else {
                log.warn("사용자 ID를 찾을 수 없음: name={}, phone={}", name, phone);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("입력하신 정보와 일치하는 사용자 ID를 찾을 수 없습니다.");
            }
        } catch (NoSuchElementException e) { // NoSuchElementException을 명시적으로 처리
            log.warn("사용자 ID를 찾을 수 없음: name={}, phone={}", name, phone, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("입력하신 정보와 일치하는 사용자 ID를 찾을 수 없습니다.");
        } catch (Exception e) {
            log.error("아이디 찾기 오류 발생: name={}, phone={}", name, phone, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("사용자 ID를 찾는 중 오류가 발생했습니다.");
        }
    }

    // UserController.java (findpwdUpdatePassword 메서드 부분)

    @PostMapping("/findpwd")
    public ResponseEntity<?> findpwdUpdatePassword(@RequestBody User user) {
        if (user.getLoginId() == null || user.getLoginId().isEmpty()) {
            log.error("아이디가 없습니다.: loginId={}", user.getLoginId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("아이디를 다시 확인해주세요.");
        }
        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            log.error("전화번호가 없습니다.: phone={}", user.getPhone());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("전화번호를 입력해주세요.");
        }

        try {
            User foundUser = userService.findUserByLoginIdAndPhone(user.getLoginId(), user.getPhone());

            if (foundUser != null) {
                String temporaryPassword = GeneratePassword.generateRandomPassword(8, 16);
                // String encodedTemporaryPassword = bcryptPasswordEncoder.encode(temporaryPassword); // 이 줄은 제거

                log.info("Generated temporary password (plaintext): {}", temporaryPassword);
                // log.info("Encoded temporary password (for DB storage): {}", encodedTemporaryPassword); // 이 줄도 제거

                // UserService.updateUserPassword는 이제 평문 비밀번호를 받습니다.
                userService.updateUserPassword(foundUser.getUserId(), temporaryPassword); // 변경: encodedTemporaryPassword -> temporaryPassword

                log.info("비밀번호 업데이트 성공: loginId={}", foundUser.getLoginId());

                Map<String, String> responseBody = new HashMap<>();
                responseBody.put("message", "임시 비밀번호가 발급되었습니다. 자동으로 로그인합니다.");
                responseBody.put("temporaryPassword", temporaryPassword);
                return ResponseEntity.status(HttpStatus.OK).body(responseBody);

            } else {
                log.warn("사용자를 찾을 수 없음: loginId={}, phone={}", user.getLoginId(), user.getPhone());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("입력하신 정보와 일치하는 사용자 ID를 찾을 수 없습니다.");
            }
        } catch (NoSuchElementException e) { // NoSuchElementException을 명시적으로 처리
            log.warn("비밀번호 찾기 실패: 사용자를 찾을 수 없음: loginId={}, phone={}", user.getLoginId(), user.getPhone(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("입력하신 정보와 일치하는 사용자 ID를 찾을 수 없습니다.");
        } catch (IllegalArgumentException e) { // UserService에서 던질 수 있는 예외 처리 (비밀번호 정책 위반 등)
            log.error("비밀번호 업데이트 중 오류 발생 (정책 위반): loginId={}, phone={}, error={}", user.getLoginId(), user.getPhone(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage()); // 사용자에게 정책 위반 메시지 전달
        } catch (Exception e) {
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
            UserEntity userEntity = userRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

            userEntity.setName(request.getName());
            userEntity.setNickname(request.getNickname());
            userEntity.setPhone(request.getPhone());

            if (request.getBirthday() != null && !request.getBirthday().isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    userEntity.setBirthday(sdf.parse(request.getBirthday()));
                } catch (ParseException e) {
                    log.error("Failed to parse birthday: {}", request.getBirthday(), e);
                    return ResponseEntity.badRequest().body(Map.of("message", "잘못된 생년월일 형식입니다. YYYY-MM-DD 형식으로 입력해주세요."));
                }
            }

            userRepository.save(userEntity);
            log.info("UserEntity updated successfully for userId: {}", userEntity.getUserId());

            User userDto = userEntity.toDto();

            if (userDto.getRole().equals("BAD")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "불량유저로 등록되었습니다. 관리자에게 문의 하세요"));
            }
            if (userDto.getRole().equals("EXIT")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "탈퇴유저로 등록되었습니다. 관리자에게 문의 하세요."));
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
    @GetMapping("/myinfo-detail") // 새로운 API 엔드포인트
    public ResponseEntity<?> getMyInfoDetail(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername(); // JWT 토큰에서 추출된 userId

        log.info("내 상세 정보 조회 요청: userId={}", userId);

        try {
            User userDto = userService.findUserByUserId(userId); // UserService에 새로운 메서드 필요

            if (userDto != null) {
                // 생년월일 Date 객체를 YYYY-MM-DD 문자열로 변환하여 DTO에 설정 (필요하다면)
                // 또는 DTO에 Date 필드를 직접 사용하고 프론트에서 포매팅
                // 여기서는 User DTO에 birthday가 Date 타입이라고 가정하고, 프론트에서 파싱한다고 가정.
                // 만약 User DTO에 @JsonFormat("yyyy-MM-dd")가 있다면 자동으로 처리됩니다.
                log.info("내 상세 정보 조회 성공: userId={}", userId);
                return ResponseEntity.ok(userDto);
            } else {
                log.warn("내 상세 정보 조회 실패: 사용자를 찾을 수 없음 userId={}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "사용자 정보를 찾을 수 없습니다."));
            }
        } catch (Exception e) {
            log.error("내 상세 정보 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "사용자 정보를 불러오는 중 오류가 발생했습니다."));
        }
    }

    @PatchMapping("/myinfo")
    public ResponseEntity<?> updateMyInfo(@RequestBody MyInfoUpdateRequest request) {
        log.info("내 정보 변경 요청: userId={}", request.getUserId());

        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            log.error("내 정보 변경 실패: userId가 비어있습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("사용자 ID는 필수입니다.");
        }

        try {
            UserEntity updatedUser = userService.updateMyInfo(
                    request.getUserId(),
                    request.getNickname(),
                    request.getPhone(),
                    request.getBirthday(),
                    request.getProfileImagePath(),
                    request.getStatusMessage()
            );

            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "내 정보가 성공적으로 변경되었습니다.");
            responseBody.put("nickname", updatedUser.getNickname());
            responseBody.put("phone", updatedUser.getPhone());

            log.info("내 정보 변경 성공: userId={}, updated fields", request.getUserId());
            return ResponseEntity.ok().body(responseBody);

        } catch (IllegalArgumentException e) {
            log.error("내 정보 변경 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ParseException e) {
            log.error("생년월일 파싱 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 생년월일 형식입니다. YYYY-MM-DD 형식으로 입력해주세요.");
        } catch (Exception e) {
            log.error("내 정보 변경 중 예상치 못한 오류 발생: userId={}, error={}", request.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("내 정보 변경 중 오류가 발생했습니다.");
        }
    }

    @PatchMapping("/update/password")
    public ResponseEntity<?> updatePassword(@RequestBody Pwd pwdRequest) {
        log.info("비밀번호 변경 요청: userId={}", pwdRequest.getUserId());

        if (pwdRequest.getUserId() == null || pwdRequest.getUserId().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "사용자 ID는 필수입니다."));
        }
        if (pwdRequest.getPrevPwd() == null || pwdRequest.getPrevPwd().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "현재 비밀번호는 필수입니다."));
        }
        if (pwdRequest.getCurrPwd() == null || pwdRequest.getCurrPwd().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "새 비밀번호는 필수입니다."));
        }

        try {
            UserEntity userEntity = userRepository.findByUserId(pwdRequest.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다.")); // IllegalArgumentException 대신 NoSuchElementException 사용

            // 현재 비밀번호 확인
            if (!bcryptPasswordEncoder.matches(pwdRequest.getPrevPwd(), userEntity.getPassword())) {
                log.warn("비밀번호 변경 실패: userId={}, oldPassword 불일치", pwdRequest.getUserId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "현재 비밀번호가 일치하지 않습니다."));
            }

            userService.updateUserPassword(pwdRequest.getUserId(), pwdRequest.getCurrPwd());

            log.info("비밀번호 변경 성공: userId={}", pwdRequest.getUserId());
            return ResponseEntity.ok().body(Map.of("message", "비밀번호가 성공적으로 변경되었습니다.")); // 성공 메시지도 JSON으로 통일
        } catch (NoSuchElementException e) { // 사용자 정보 없음
            log.error("비밀번호 변경 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) { // UserService에서 던지는 정책 위반 예외
            log.error("비밀번호 변경 실패 (정책 위반): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) { // 그 외 예상치 못한 모든 예외
            log.error("비밀번호 변경 중 예상치 못한 오류 발생: userId={}, error={}", pwdRequest.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "비밀번호 변경 중 서버 오류가 발생했습니다."));
        }
    }

    @Getter
    @Setter
    public static class MyInfoUpdateRequest {
        private String userId;
        private String nickname;
        private String phone;
        private String birthday; // YYYY-MM-DD 형식의 문자열
        private String profileImagePath;
        private String statusMessage;
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