package com.web.memoire.user.controller;

import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.jpa.entity.Token;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.User;
import com.web.memoire.user.model.dto.Pwd;
import com.web.memoire.user.model.dto.UserRegistrationRequest;
import com.web.memoire.user.model.service.PwdService;
import com.web.memoire.user.model.service.UserService;
import com.web.memoire.user.model.service.ImageService;
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
import org.springframework.web.multipart.MultipartFile;
import com.web.memoire.user.model.service.FaceRecognitionService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Date;
import java.util.NoSuchElementException;
import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final PwdService pwdService;
    private final BCryptPasswordEncoder bcryptPasswordEncoder;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final JWTUtil jwtUtil;
    private final TokenService tokenService;
    private final ImageService imageService;
    private final FaceRecognitionService faceRecognitionService;

    @PostMapping(value = "/idcheck")
    public ResponseEntity<String> dupCheckId(@RequestParam("loginId") String loginId) {
        log.info("ID 중복 검사 요청: loginId={}", loginId);
        boolean exists = userService.selectCheckId(loginId);
        log.info("ID 중복 검사 결과: loginId={}, exists={}", loginId, exists);
        return ResponseEntity.ok(exists ? "duplicated" : "ok");
    }

    @GetMapping("/check-phone")
    public ResponseEntity<?> checkPhoneExists(@RequestParam("phone") String phone) {
        log.info("전화번호 존재 여부 확인 요청: phone={}", phone);
        boolean exists = userService.isPhoneExists(phone); // 전화번호 존재 여부 확인 로직
        log.info("전화번호 존재 여부 확인 결과: phone={}, exists={}", phone, exists);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> userInsertMethod(@RequestBody UserRegistrationRequest request){
        log.info("회원가입 요청: loginId={}, name={}", request.getLoginId(), request.getName());

        // 1. 비밀번호 유효성 검사
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            log.warn("회원가입 실패: 비밀번호 누락");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호를 입력해주세요.");
        }

        // 2. userId 생성 및 기본값 설정
        String newUserId = UUID.randomUUID().toString();
        // request DTO에 userId를 설정하여 Pwd DTO 생성 시 사용
        request.setUserId(newUserId);

        // 3. 비밀번호 암호화 및 Pwd DTO 생성
        String encodedPassword = bcryptPasswordEncoder.encode(request.getPassword());
        Pwd pwd = Pwd.builder()
                .userId(newUserId) // 새로 생성된 userId 사용
                .currPwd(encodedPassword)
                // prevPwd는 회원가입 시에는 없으므로 설정하지 않음
                .build();

        // 4. User DTO 생성 (비밀번호 필드 제외)
        // User DTO는 UserEntity와 동일하게 비밀번호 필드가 없습니다.
        User user = User.builder()
                .userId(newUserId)
                .name(request.getName())
                .birthday(request.getBirthday())
                .role(request.getRole() != null ? request.getRole() : "USER") // 역할 설정 (기본값 USER)
                .autoLoginFlag(request.getAutoLoginFlag() != null ? request.getAutoLoginFlag() : "N") // 기본값 N
                .registrationDate(new Date()) // 현재 시간으로 설정
                .loginId(request.getLoginId())
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .profileImagePath(request.getProfileImagePath())
                .sanctionCount(request.getSanctionCount() != null ? request.getSanctionCount() : 0) // 기본값 0
                .statusMessage(request.getStatusMessage())
                .loginType(request.getLoginType() != null ? request.getLoginType() : "original") // 기본값 original
                .build();

        try{
            // 5. 사용자 정보 저장
            userService.insertUser(user);
            // 6. 비밀번호 이력 저장
            pwdService.savePasswordHistory(pwd.toEntity()); // PwdService의 savePasswordHistory 메서드를 호출합니다.

            log.info("회원가입 성공: loginId={}", user.getLoginId());
            return ResponseEntity.status(HttpStatus.OK).build();
        }catch(Exception e){
            log.error("회원가입 실패: loginId={}, 오류: {}", user.getLoginId(), e.getMessage());
            // TODO: 비밀번호 저장 실패 시 사용자 정보 롤백 또는 트랜잭션 처리 고려
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/findid")
    public ResponseEntity<?> findLoginIdByNameAndPhone(@RequestParam("name") String name, @RequestParam("phone") String phone) {
        log.info("아이디 찾기 요청: name={}, phone={}", name, phone);
        if (name == null || name.trim().isEmpty() || phone == null || phone.trim().isEmpty()) {
            log.warn("아이디 찾기 실패: 이름 또는 전화번호 누락");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이름과 전화번호를 입력해주세요.");
        }

        try {
            String loginId = userService.findLoginIdByNameAndPhone(name, phone);
            if (loginId != null) {
                log.info("아이디 찾기 성공: name={}, phone={}, loginId={}", name, phone, loginId);
                return ResponseEntity.status(HttpStatus.OK).body(loginId);
            } else {
                log.info("아이디 찾기 실패: 일치하는 사용자 없음 (name={}, phone={})", name, phone);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("입력하신 정보와 일치하는 사용자 ID를 찾을 수 없습니다.");
            }
        } catch (NoSuchElementException e) {
            log.warn("아이디 찾기 실패 (NoSuchElementException): name={}, phone={}, 오류: {}", name, phone, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("입력하신 정보와 일치하는 사용자 ID를 찾을 수 없습니다.");
        } catch (Exception e) {
            log.error("아이디 찾기 중 서버 오류: name={}, phone={}, 오류: {}", name, phone, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("사용자 ID를 찾는 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/findpwd")
    public ResponseEntity<?> findpwdUpdatePassword(@RequestBody User user) {
        log.info("비밀번호 찾기 (임시 비밀번호 발급) 요청: loginId={}, phone={}", user.getLoginId(), user.getPhone());
        if (user.getLoginId() == null || user.getLoginId().isEmpty() || user.getPhone() == null || user.getPhone().isEmpty()) {
            log.warn("비밀번호 찾기 실패: 아이디 또는 전화번호 누락");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("아이디와 전화번호를 입력해주세요.");
        }

        try {
            User foundUser = userService.findUserByLoginIdAndPhone(user.getLoginId(), user.getPhone());

            if (foundUser != null) {
                String temporaryPassword = GeneratePassword.generateRandomPassword(8, 16);
                userService.resetUserPassword(foundUser.getUserId(), temporaryPassword);

                Map<String, String> responseBody = new HashMap<>();
                responseBody.put("message", "임시 비밀번호가 발급되었습니다. 자동으로 로그인합니다.");
                responseBody.put("temporaryPassword", temporaryPassword);
                log.info("비밀번호 찾기 성공: loginId={}, 임시 비밀번호 발급", user.getLoginId());
                return ResponseEntity.status(HttpStatus.OK).body(responseBody);
            } else {
                log.info("비밀번호 찾기 실패: 일치하는 사용자 없음 (loginId={}, phone={})", user.getLoginId(), user.getPhone());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("입력하신 정보와 일치하는 사용자 ID를 찾을 수 없습니다.");
            }
        } catch (NoSuchElementException e) {
            log.warn("비밀번호 찾기 실패 (NoSuchElementException): loginId={}, phone={}, 오류: {}", user.getLoginId(), user.getPhone(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("입력하신 정보와 일치하는 사용자 ID를 찾을 수 없습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("비밀번호 찾기 실패 (IllegalArgumentException): loginId={}, 오류: {}", user.getLoginId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("비밀번호 업데이트 중 서버 오류: loginId={}, 오류: {}", user.getLoginId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("비밀번호 업데이트 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/social")
    public ResponseEntity<?> requestSocialAuthorization(@RequestBody Map<String, String> payload) {
        String socialType = payload.get("socialType");
        log.info("소셜 로그인 인증 요청: socialType={}", socialType);
        if (socialType == null) {
            log.warn("소셜 로그인 인증 실패: socialType 누락");
            return ResponseEntity.badRequest().body("{\"error\":\"socialType is required\"}");
        }

        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(socialType);
        if (clientRegistration == null) {
            log.warn("소셜 로그인 인증 실패: 유효하지 않은 socialType={}", socialType);
            return ResponseEntity.badRequest().body("{\"error\":\"Invalid socialType: " + socialType + "\"}");
        }

        String baseUrl = "http://localhost:8080";
        String authorizationUrl = baseUrl + "/oauth2/authorization/" + socialType;

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("authorizationUrl", authorizationUrl);

        log.info("소셜 로그인 인증 URL 반환: socialType={}, url={}", socialType, authorizationUrl);
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/social/complete-signup")
    public ResponseEntity<?> completeSocialSignUp(@RequestBody SocialSignUpRequest request, HttpServletResponse response) {
        log.info("소셜 회원가입 완료 요청: userId={}", request.getUserId());
        try {
            UserEntity userEntity = userRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

            userEntity.setName(request.getName());
            userEntity.setNickname(request.getNickname());
            userEntity.setPhone(request.getPhone());

            if (request.getBirthday() != null && !request.getBirthday().isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                userEntity.setBirthday(sdf.parse(request.getBirthday()));
            }

            userRepository.save(userEntity);

            User userDto = userEntity.toDto();

            if (userDto.getRole().equals("BAD") || userDto.getRole().equals("EXIT")) {
                log.warn("소셜 회원가입 완료 실패: 접근 제한된 사용자 (userId={})", request.getUserId());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "접근이 제한된 사용자입니다."));
            }

            String accessToken = jwtUtil.generateToken(userDto, "access");
            String refreshToken = jwtUtil.generateToken(userDto, "refresh");

            tokenService.saveRefreshToken(new Token(userDto.getUserId(), refreshToken));

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("accessToken", accessToken);
            responseBody.put("refreshToken", refreshToken);
            responseBody.put("userId", userDto.getUserId());
            responseBody.put("name", userDto.getName());
            responseBody.put("role", userDto.getRole());
            responseBody.put("autoLoginFlag", userDto.getAutoLoginFlag());
            responseBody.put("nickname", userDto.getNickname());

            log.info("소셜 회원가입 완료 및 로그인 성공: userId={}", userDto.getUserId());
            return ResponseEntity.ok(responseBody);
        } catch (IllegalArgumentException | ParseException e) {
            log.warn("소셜 회원가입 완료 실패 (잘못된 요청): userId={}, 오류: {}", request.getUserId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("소셜 회원가입 완료 중 서버 오류: userId={}, 오류: {}", request.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "회원가입 완료 중 서버 오류가 발생했습니다."));
        }
    }

    @GetMapping("/myinfo-detail")
    public ResponseEntity<?> getMyInfoDetail(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        log.info("내 정보 상세 조회 요청: userId={}", userId);

        try {
            User userDto = userService.findUserByUserId(userId);
            if (userDto != null) {
                log.info("내 정보 상세 조회 성공: userId={}", userId);
                return ResponseEntity.ok(userDto);
            } else {
                log.warn("내 정보 상세 조회 실패: 사용자 정보 없음 (userId={})", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "사용자 정보를 찾을 수 없습니다."));
            }
        } catch (Exception e) {
            log.error("내 정보 상세 조회 중 서버 오류: userId={}, 오류: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "사용자 정보를 불러오는 중 오류가 발생했습니다."));
        }
    }

    @PatchMapping("/myinfo")
    public ResponseEntity<?> updateMyInfo(@RequestBody MyInfoUpdateRequest request) {
        log.info("내 정보 업데이트 요청: userId={}, nickname={}, phone={}", request.getUserId(), request.getNickname(), request.getPhone());
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            log.warn("내 정보 업데이트 실패: 사용자 ID 누락");
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

            log.info("내 정보 업데이트 성공: userId={}", request.getUserId());
            return ResponseEntity.ok().body(responseBody);
        } catch (IllegalArgumentException e) {
            log.warn("내 정보 업데이트 실패 (IllegalArgumentException): userId={}, 오류: {}", request.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ParseException e) {
            log.warn("내 정보 업데이트 실패 (잘못된 생년월일 형식): userId={}, 오류: {}", request.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 생년월일 형식입니다. YYYY-MM-DD 형식으로 입력해주세요.");
        } catch (Exception e) {
            log.error("내 정보 업데이트 중 서버 오류: userId={}, 오류: {}", request.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("내 정보 변경 중 오류가 발생했습니다.");
        }
    }

    /**
     * 비밀번호 변경 (사용자가 현재 비밀번호를 알고 있을 때 사용)
     * - 로그인된 사용자가 마이페이지 등에서 비밀번호를 변경하는 시나리오에 적합합니다.
     * - 요청 시 userId, prevPwd (현재 비밀번호), currPwd (새 비밀번호)가 필요합니다.
     */
    @PatchMapping("/update/password")
    public ResponseEntity<?> updatePassword(@RequestBody Pwd pwdRequest) {
        log.info("비밀번호 변경 요청: userId={}", pwdRequest.getUserId());

        // 필수 요청 정보 누락 검사: userId, prevPwd, currPwd 모두 필요
        if (pwdRequest.getUserId() == null || pwdRequest.getUserId().trim().isEmpty() ||
                pwdRequest.getPrevPwd() == null || pwdRequest.getPrevPwd().isEmpty() ||
                pwdRequest.getCurrPwd() == null || pwdRequest.getCurrPwd().isEmpty()) {
            log.warn("비밀번호 변경 실패: 필수 정보 누락 (userId={})", pwdRequest.getUserId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "필수 정보가 누락되었습니다. (사용자 ID, 현재 비밀번호, 새 비밀번호)"));
        }

        try {
            // PwdService를 통해 비밀번호 변경 로직 수행
            // 이 서비스 메서드 내에서 이전 비밀번호 확인, 새 비밀번호 암호화 및 이력 저장, 비밀번호 중복 확인까지 처리합니다.
            pwdService.changeUserPassword(pwdRequest.getUserId(), pwdRequest.getPrevPwd(), pwdRequest.getCurrPwd());

            log.info("비밀번호 변경 성공: userId={}", pwdRequest.getUserId());
            return ResponseEntity.ok().body(Map.of("message", "비밀번호가 성공적으로 변경되었습니다."));

        } catch (NoSuchElementException e) {
            // 사용자 ID를 찾을 수 없거나 소셜 로그인 유저 등 비밀번호 변경 불가 상황
            log.warn("비밀번호 변경 실패 (NoSuchElementException): userId={}, 오류: {}", pwdRequest.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // 이전 비밀번호 불일치, 새 비밀번호 유효성 검사 실패, 과거 비밀번호 재사용 등 비즈니스 로직 오류
            log.warn("비밀번호 변경 실패 (IllegalArgumentException): userId={}, 오류: {}", pwdRequest.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // 그 외 예상치 못한 서버 오류
            log.error("비밀번호 변경 중 서버 오류: userId={}, 오류: {}", pwdRequest.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "비밀번호 변경 중 서버 오류가 발생했습니다."));
        }
    }

    @PostMapping("/images/check-safety")
    public ResponseEntity<Map<String, Object>> checkImageSafety(@RequestParam("image") MultipartFile file) {
        log.info("[Server] 이미지 안전성 검사 요청 수신: fileName={}, size={} bytes", file.getOriginalFilename(), file.getSize()); // 요청 수신 로그 추가
        if (file.isEmpty()) {
            log.warn("[Server] 이미지 안전성 검사 실패: 파일 없음");
            return ResponseEntity.badRequest().body(Map.of("safe", false, "message", "이미지 파일이 없습니다."));
        }
        boolean isSafe = imageService.isImageSafe(file);
        log.info("[Server] 이미지 안전성 검사 결과 전송: fileName={}, isSafe={}", file.getOriginalFilename(), isSafe); // 응답 전 로그 추가
        return ResponseEntity.ok(Map.of("safe", isSafe));
    }

    @PostMapping("/{userId}/profile-image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(@PathVariable String userId, @RequestParam("image") MultipartFile file) {
        log.info("[Server] 프로필 이미지 업로드 요청 수신: userId={}, fileName={}, size={} bytes", userId, file.getOriginalFilename(), file.getSize()); // 요청 수신 로그 추가
        if (file.isEmpty()) {
            log.warn("[Server] 프로필 이미지 업로드 실패: 파일 없음 (userId={})", userId);
            return ResponseEntity.badRequest().body(Map.of("message", "이미지 파일이 없습니다."));
        }
        try {
            if (!imageService.isImageSafe(file)) {
                log.warn("[Server] 프로필 이미지 업로드 실패: 유해성 감지 (userId={}, fileName={})", userId, file.getOriginalFilename());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "유해성이 감지된 이미지는 저장할 수 없습니다."));
            }

            String filePath = imageService.storeProfileImage(userId, file);
            log.info("[Server] 프로필 이미지 업로드 성공 및 경로 전송: userId={}, filePath={}", userId, filePath); // 응답 전 로그 추가
            return ResponseEntity.ok(Map.of("message", "프로필 이미지가 성공적으로 업로드되었습니다.", "filePath", filePath));
        } catch (RuntimeException e) {
            log.error("[Server] 프로필 이미지 업로드 실패 (RuntimeException): userId={}, fileName={}, 오류: {}", userId, file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("[Server] 프로필 이미지 업로드 중 서버 오류: userId={}, fileName={}, 오류: {}", userId, file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "이미지 업로드 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 특정 사용자의 얼굴 임베딩을 등록/업데이트합니다.
     * 클라이언트로부터 받은 이미지 파일을 FastAPI로 전송하여 임베딩을 추출하고,
     * 추출된 임베딩을 Spring Boot DB에 저장합니다.
     * @param userId 임베딩을 등록할 사용자 ID
     * @param file 웹캠에서 캡처한 이미지 (MultipartFile)
     * @return 등록 성공 여부 메시지
     */
    @PostMapping("/{userId}/face-embedding")
    public ResponseEntity<Map<String, String>> registerFaceEmbedding(@PathVariable String userId, @RequestParam("file") MultipartFile file) {
        log.info("얼굴 임베딩 등록 요청: userId={}, fileName={}", userId, file.getOriginalFilename());
        if (file.isEmpty()) {
            log.warn("얼굴 임베딩 등록 실패: 파일 없음 (userId={})", userId);
            return ResponseEntity.badRequest().body(Map.of("message", "이미지 파일이 없습니다."));
        }
        try {
            byte[] imageData = file.getBytes();
            boolean success = userService.saveUserFaceEmbedding(userId, imageData);
            if (success) {
                log.info("얼굴 임베딩 등록 성공: userId={}", userId);
                return ResponseEntity.ok(Map.of("message", userId + "의 얼굴 임베딩이 성공적으로 등록/업데이트되었습니다."));
            } else {
                log.warn("얼굴 임베딩 등록 실패: 처리 오류 (userId={})", userId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", userId + "의 얼굴 임베딩 등록/업데이트 실패."));
            }
        } catch (NoSuchElementException e) {
            log.error("얼굴 임베딩 등록 실패 (사용자 ID를 찾을 수 없음): userId={}, 오류: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "사용자 정보를 찾을 수 없습니다."));
        } catch (IllegalArgumentException e) {
            log.error("얼굴 임베딩 등록 실패 (추출 오류): userId={}, 오류: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            log.error("얼굴 임베딩 등록 중 파일 처리 또는 통신 오류: userId={}, 오류: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "얼굴 임베딩 등록 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 얼굴 임베딩을 사용하여 사용자를 인증합니다.
     * 클라이언트로부터 받은 이미지 파일을 FastAPI로 전송하여 임베딩을 추출하고,
     * 추출된 임베딩을 DB의 저장된 임베딩들과 비교하여 사용자를 식별합니다.
     * @param file 현재 웹캠에서 캡처한 이미지 (MultipartFile)
     * @param response HttpServletResponse (JWT 토큰 발급용)
     * @return 인증 성공 시 사용자 정보 및 토큰, 실패 시 오류 메시지
     */
    @PostMapping("/face-login")
    public ResponseEntity<?> loginByFace(@RequestParam("file") MultipartFile file, HttpServletResponse response) {
        log.info("얼굴 로그인 요청: fileName={}", file.getOriginalFilename());
        if (file.isEmpty()) {
            log.warn("얼굴 로그인 실패: 파일 없음");
            return ResponseEntity.badRequest().body(Map.of("message", "이미지 파일이 없습니다."));
        }
        try {
            byte[] imageData = file.getBytes();
            String authenticatedUserId = userService.authenticateUserByFace(imageData);

            if (authenticatedUserId != null) {
                // 인증 성공: JWT 토큰 발급 및 사용자 정보 반환
                User userDto = userService.findUserByUserId(authenticatedUserId);
                if (userDto == null) {
                    log.warn("얼굴 로그인 성공 후 사용자 정보 찾기 실패: userId={}", authenticatedUserId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "인증된 사용자 정보를 찾을 수 없습니다."));
                }

                String accessToken = jwtUtil.generateToken(userDto, "access");
                String refreshToken = jwtUtil.generateToken(userDto, "refresh");

                tokenService.saveRefreshToken(new Token(userDto.getUserId(), refreshToken));

                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("accessToken", accessToken);
                responseBody.put("refreshToken", refreshToken);
                responseBody.put("userId", userDto.getUserId());
                responseBody.put("name", userDto.getName());
                responseBody.put("role", userDto.getRole());
                responseBody.put("autoLoginFlag", userDto.getAutoLoginFlag());
                responseBody.put("nickname", userDto.getNickname());

                log.info("얼굴 로그인 성공: userId={}", userDto.getUserId());
                return ResponseEntity.ok(responseBody);
            } else {
                // 인증 실패
                log.info("얼굴 로그인 실패: 일치하는 사용자 없음");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "얼굴 인식에 실패했거나 일치하는 사용자가 없습니다."));
            }
        } catch (IllegalArgumentException e) {
            log.error("얼굴 임베딩 추출 오류 (로그인 시도): 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            log.error("얼굴 로그인 중 파일 처리 또는 통신 오류: 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "얼굴 로그인 중 서버 오류가 발생했습니다."));
        } catch (Exception e) {
            log.error("얼굴 로그인 처리 중 예기치 않은 오류: 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "얼굴 로그인 처리 중 오류가 발생했습니다."));
        }
    }
    @PatchMapping("/exit") // PATCH 매핑으로 변경
    public ResponseEntity<?> exitUser(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        log.info("회원 역할 'EXIT'로 변경 요청: userId={}", userId);

        try {
            userService.updateUserRoleToExit(userId); // 새로운 서비스 메서드 호출
            log.info("회원 역할 'EXIT'로 변경 성공: userId={}", userId);
            return ResponseEntity.ok().body(Map.of("message", "회원 탈퇴(역할 변경)가 성공적으로 처리되었습니다."));
        } catch (NoSuchElementException e) {
            log.warn("회원 역할 'EXIT' 변경 실패: 사용자를 찾을 수 없음 (userId={}), 오류: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "역할을 변경할 사용자 정보를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("회원 역할 'EXIT' 변경 중 서버 오류: userId={}, 오류: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "회원 역할 변경 중 오류가 발생했습니다."));
        }
    }

    @Getter
    @Setter
    public static class MyInfoUpdateRequest {
        private String userId;
        private String nickname;
        private String phone;
        private String birthday;
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