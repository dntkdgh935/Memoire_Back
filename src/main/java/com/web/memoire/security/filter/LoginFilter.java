package com.web.memoire.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.jpa.entity.Token;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository; // UserRepository 임포트
import com.web.memoire.user.model.dto.User;

import com.web.memoire.user.model.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional; // Optional 임포트 추가


@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository; // UserRepository 주입
    private final TokenService tokenService;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, UserRepository userRepository, TokenService tokenService, UserService userService) {
        super(authenticationManager);
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository; // UserRepository 초기화
        this.tokenService = tokenService;
        this.userService = userService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        final String loginId;
        final String password;
        String autoLoginFlag = "N";

        // UUID 기반 요청 처리 (기존 로직 유지)
        if (request.getAttribute("loginId") != null && request.getAttribute("password") != null){
            loginId = (String) request.getAttribute("loginId");
            password = (String) request.getAttribute("password");
            log.info("UUID-based Login Request Detected : loginId={}", loginId);

            // UUID 기반 로그인 시에도 loginId로 userId를 조회하여 인증에 사용
            Optional<UserEntity> userOptional = userRepository.findByLoginId(loginId);
            if (!userOptional.isPresent()) {
                log.warn("UUID 기반 로그인 시도 - loginId '{}' 에 해당하는 사용자를 찾을 수 없습니다.", loginId);
                throw new RuntimeException("사용자를 찾을 수 없습니다.");
            }
            request.setAttribute("autoLoginFlagFromRequest", "N");
            // AuthenticationManager에는 userId를 전달하여 CustomUserDetailsService가 userId로 조회하도록 함
            return this.getAuthenticationManager().authenticate(new UsernamePasswordAuthenticationToken(userOptional.get().getUserId(), password));
        }

        // JSON 요청 본문 파싱
        try{
            Map<String, String> requestBody = objectMapper.readValue(request.getInputStream(), Map.class);
            log.info("로그인 요청 본문 파싱 성공. 수신된 loginId: {}, password: {}",
                    requestBody.get("loginId"),
                    requestBody.get("password") != null ? "********" : "null");

            loginId = requestBody.get("loginId");
            password = requestBody.get("password");
            autoLoginFlag = requestBody.getOrDefault("autoLoginFlag", "N");

        } catch (IOException e) {
            log.error("로그인 요청 본문 파싱 실패", e);
            throw new RuntimeException("데이터 확인 불가",e);
        }

        if(loginId == null || password == null || loginId.trim().isEmpty() || password.trim().isEmpty()){
            log.warn("아이디 또는 비밀번호가 전송되지 않았습니다. loginId: {}, password: {}", loginId, password != null ? "******" : "null");
            throw new RuntimeException("아이디 또는 비밀번호가 전송되지 않았습니다.");
        }

        request.setAttribute("autoLoginFlagFromRequest", autoLoginFlag);

        // 1. loginId로 UserEntity를 조회하여 userId를 얻습니다.
        UserEntity userToAuthenticate = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> {
                    log.warn("로그인 시도 - loginId '{}' 에 해당하는 사용자를 찾을 수 없습니다.", loginId);
                    return new RuntimeException("아이디 또는 비밀번호가 일치하지 않습니다."); // 사용자에게는 일반적인 메시지
                });

        // 2. AuthenticationManager에는 조회된 userId와 입력된 password를 전달합니다.
        // CustomUserDetailsService는 이제 userId를 받아서 사용자를 조회하고 비밀번호를 검증하게 됩니다.
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(userToAuthenticate.getUserId(), password);
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain, Authentication authentication) throws IOException {

        // authentication.getName()은 이제 userId를 반환합니다.
        log.info("로그인 로직 실행 - 인증 성공: 사용자 {}", authentication.getName());
        String userId = authentication.getName(); // userId를 직접 받음

        // userId로 UserEntity 조회 (CustomUserDetailsService에서 이미 인증된 사용자이므로 다시 조회)
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("인증은 성공했으나, DB에서 userId '{}' 에 해당하는 사용자를 찾을 수 없습니다. (비정상 상태)", userId);
                    return new RuntimeException("인증된 사용자를 찾을 수 없습니다.");
                });

        log.info("DB에서 userId '{}' 에 해당하는 사용자 {} (loginId: {}) 를 성공적으로 찾았습니다.", userId, user.getName(), user.getLoginId());

        // Request 속성에서 autoLoginFlag 가져오기
        String receivedAutoLoginFlag = (String) request.getAttribute("autoLoginFlagFromRequest");
        log.info("successfulAuthentication - 클라이언트로부터 받은 autoLoginFlag: {}", receivedAutoLoginFlag);

        // UserService를 호출하여 autoLoginFlag 업데이트
        userService.updateUserAutoLoginFlag(userId, receivedAutoLoginFlag);


        if(user.getRole().equals("BAD")){
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"error\":\"불량유저로 등록되었습니다. 관리자에게 문의 하세요\"}");
            return;
        }

        if(user.getRole().equals("EXIT")){
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"error\":\"탈퇴유저로 등록되었습니다. 관리자에게 문의 하세요.\"}");
            return;
        }

        String accessToken = jwtUtil.generateToken(user.toDto(),"access");
        String refreshToken = jwtUtil.generateToken(user.toDto(),"refresh");

        tokenService.saveRefreshToken(new Token(user.getUserId(), refreshToken));

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("accessToken", accessToken);
        responseBody.put("refreshToken", refreshToken);
        responseBody.put("userId", user.getUserId());
        responseBody.put("name", user.getName());
        responseBody.put("role", user.getRole());
        responseBody.put("autoLoginFlag", user.getAutoLoginFlag());

        response.setContentType("application/json; charset=utf-8");
        objectMapper.writeValue(response.getWriter(), responseBody);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        log.warn("LoginFilter - 인증 실패: {}", failed.getMessage(), failed);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=utf-8");

        String errorMessage;
        if(failed.getMessage().contains("Bad credentials")){
            errorMessage = "아이디 또는 비밀번호가 일치하지 않습니다.";
        } else if(failed.getMessage().contains("사용자를 찾을 수 없습니다.")){
            errorMessage = "아이디 또는 비밀번호가 일치하지 않습니다."; // 사용자에게는 동일한 메시지
        }
        else {
            errorMessage = "로그인 실패: 알 수 없는 오류가 발생했습니다.";
        }

        PrintWriter writer = response.getWriter();
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", errorMessage);
        objectMapper.writeValue(writer, errorResponse);
        writer.flush();
    }
}
