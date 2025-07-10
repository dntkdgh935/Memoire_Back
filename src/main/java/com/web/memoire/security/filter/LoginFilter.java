package com.web.memoire.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.jpa.entity.Token;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.User; // User DTO 임포트 추가
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
import java.io.PrintWriter; // PrintWriter 임포트 추가
import java.util.HashMap; // HashMap 임포트 추가
import java.util.Map;
import java.util.UUID;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    // AuthenticationManager는 부모 클래스에서 관리하므로 필드로 직접 선언할 필요는 없습니다.
    // private final AuthenticationManager authenticationManager;

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 파싱을 위한 ObjectMapper 추가

    // 생성자에서 AuthenticationManager를 부모 클래스에 전달 (변경)
    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, UserRepository userRepository, TokenService tokenService) {
        // 부모 클래스의 생성자를 호출하여 AuthenticationManager를 설정하는 것이 핵심 (수정)
        super(authenticationManager);
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.tokenService = tokenService;

        // 로그인 요청 URL 설정 (SecurityConfig에서 LoginFilter를 addFilterAt 할 때 설정 가능)
        // setFilterProcessesUrl("/login"); // SecurityConfig에서 설정하므로 여기서는 주석 처리
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        String loginId = null;
        String password = null;
        String autoLoginFlag = "N"; // autoLoginFlag 기본값 설정

        // UUID 기반 요청 처리 (기존 로직 유지)
        // 이 로직은 request.getAttribute를 사용하는데, 일반적인 프론트엔드 로그인 요청에서는 사용되지 않습니다.
        // 만약 특정 내부 로직이나 자동 로그인 플로우에서 사용한다면 유지, 아니라면 제거를 고려하세요.
        if (request.getAttribute("loginId") != null && request.getAttribute("password") != null){
            loginId = (String) request.getAttribute("loginId");
            password = (String) request.getAttribute("password");
            log.info("UUID-based Login Request Detected : loginId={}", loginId); // userPwd 로깅 제거 (보안)

            // 비밀번호 검증 건너뛰어도 됨 (이 부분은 CustomUserDetailsService에서 비밀번호를 비교하므로,
            // 여기서는 단순히 토큰을 생성하여 AuthenticationManager에 전달합니다.)
            return this.getAuthenticationManager().authenticate(new UsernamePasswordAuthenticationToken(loginId, password));
        }

        // JSON 요청 본문 파싱 (수정)
        try{
            // Map<String, String> requestBody = mapper.readValue(request.getInputStream(), Map.class);
            // ObjectMapper를 생성자에서 초기화했으므로 필드 objectMapper 사용
            Map<String, String> requestBody = objectMapper.readValue(request.getInputStream(), Map.class);
            log.info("로그인 요청 본문 파싱 성공. 수신된 loginId: {}, password: {}",
                    requestBody.get("loginId"),
                    requestBody.get("password") != null ? "********" : "null");

            loginId = requestBody.get("loginId");
            password = requestBody.get("password");
            autoLoginFlag = requestBody.getOrDefault("autoLoginFlag", "N"); // autoLoginFlag 추가 및 기본값 설정

        } catch (IOException e) {
            log.error("로그인 요청 본문 파싱 실패", e);
            throw new RuntimeException("데이터 확인 불가",e);
        }

        if(loginId == null || password == null || loginId.trim().isEmpty() || password.trim().isEmpty()){ // null 및 빈 문자열 체크 강화
            log.warn("아이디 또는 비밀번호가 전송되지 않았습니다. loginId: {}, password: {}", loginId, password != null ? "******" : "null");
            throw new RuntimeException("아이디 또는 비밀번호가 전송되지 않았습니다.");
        }

        // 토큰 생성 및 반환 (기존 로직 유지)
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(loginId, password);
        // AuthenticationManager를 통해 인증 시도 (this.getAuthenticationManager()는 부모 클래스에서 제공)
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain, Authentication authentication) throws IOException {

        log.info("로그인 로직 실행 - 인증 성공: 사용자 {}", authentication.getName());
        String loginId = authentication.getName();

        // UserEntity 조회 (Optional 처리로 안전하게 변경)
        UserEntity user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> {

            log.error("인증은 성공했으나, DB에서 loginId '{}' 에 해당하는 사용자를 찾을 수 없습니다. (비정상 상태)", loginId);
            return new RuntimeException("인증된 사용자를 찾을 수 없습니다."); // 런타임 예외로 던집니다.
        });

        log.info("DB에서 loginId '{}' 에 해당하는 사용자 {} 를 성공적으로 찾았습니다.", loginId, user.getName());


        if(user.getRole().equals("BAD")){
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json; charset=utf-8"); // Content-Type 설정 추가
            response.getWriter().write("{\"error\":\"불량유저로 등록되었습니다. 관리자에게 문의 하세요\"}");
            return;
        }

        if(user.getRole().equals("EXIT")){
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json; charset=utf-8"); // Content-Type 설정 추가
            response.getWriter().write("{\"error\":\"탈퇴유저로 등록되었습니다. 관리자에게 문의 하세요.\"}");
            return;
        }

        // UserEntity를 DTO로 변환하여 JWTUtil에 전달
        // user.toDto()가 User DTO를 반환한다고 가정합니다.
        String accessToken = jwtUtil.generateToken(user.toDto(),"access");
        String refreshToken = jwtUtil.generateToken(user.toDto(),"refresh");

        // Refresh Token 저장
        // Token 엔티티 생성 시 UUID를 ID로 사용
        tokenService.saveRefreshToken(new Token(UUID.randomUUID().toString(), refreshToken));

        // 응답 본문에 토큰 및 사용자 정보 전송 (JSON 형식)
        Map<String, Object> responseBody = new HashMap<>(); // Map.of는 불변 Map을 생성하므로, 가변 Map인 HashMap 사용
        responseBody.put("accessToken", accessToken);
        responseBody.put("refreshToken", refreshToken);
        responseBody.put("userId", user.getUserId()); // UserEntity에서 userId 가져옴
        responseBody.put("name", user.getName()); // UserEntity에서 Name 가져옴
        responseBody.put("role", user.getRole()); // UserEntity에서 role 가져옴
        responseBody.put("autoLoginFlag", user.getAutoLoginFlag()); // UserEntity에서 autoLoginFlag 가져옴

        response.setContentType("application/json; charset=utf-8");
        // new ObjectMapper().writeValue(response.getWriter(), responseBody); // 필드 objectMapper 사용
        objectMapper.writeValue(response.getWriter(), responseBody); // 필드 objectMapper 사용

        // 체인 계속 진행 (필요한 경우)
        // filterChain.doFilter(request, response); // 로그인 성공 후 일반적으로 필터 체인을 더 이상 진행하지 않습니다.
        // JWTFilter에서 SecurityContextHolder에 인증 정보를 설정하므로,
        // 여기서는 응답을 완료하고 종료하는 것이 일반적입니다.
    }

    // 로그인 실패시 (기존 로직 유지 및 개선)
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        log.warn("LoginFilter - 인증 실패: {}", failed.getMessage(), failed);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=utf-8");

        String errorMessage;
        // Spring Security의 기본 메시지를 기반으로 커스텀 메시지 제공
        if(failed.getMessage().contains("Bad credentials")){
            errorMessage = "아이디 또는 비밀번호가 일치하지 않습니다."; // 메시지 구체화
        } else if(failed.getMessage().contains("UserDetailsService returned null")){ // CustomUserDetailsService에서 null 반환 시
            errorMessage = "사용자를 찾을 수 없습니다.";
        } else if(failed.getMessage().contains("사용자를 찾을 수 없습니다.")){ // CustomUserDetailsService에서 던진 메시지
            errorMessage = "사용자를 찾을 수 없습니다.";
        }
        else {
            errorMessage = "로그인 실패: 알 수 없는 오류가 발생했습니다.";
        }

        // 응답 본문에 에러 메시지 전송
        PrintWriter writer = response.getWriter(); // PrintWriter 얻기
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", errorMessage);
        objectMapper.writeValue(writer, errorResponse); // ObjectMapper 사용
        writer.flush();
    }
}
