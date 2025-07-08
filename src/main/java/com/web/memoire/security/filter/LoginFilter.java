package com.web.memoire.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.jpa.entity.Token;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, UserRepository userRepository, TokenService tokenService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        String loginId=null;
        String password=null;

        //UUID 기반 요청
        if (request.getAttribute("loginId") != null && request.getAttribute("password") != null){
            loginId = (String) request.getAttribute("loginId");
            password = (String) request.getAttribute("password");
            log.info("UUID-based Login Request Detected : loginId={}, userPwd={}", loginId, password);

            //비밀번호 검증 건너뛰어도 됨
            return this.getAuthenticationManager().authenticate(new UsernamePasswordAuthenticationToken(loginId, password));
        }

        try{
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> requestBody = mapper.readValue(request.getInputStream(), Map.class);
            log.info("requestBody={}", requestBody);
            loginId = requestBody.get("loginId");
            password = requestBody.get("password");

        } catch (IOException e) {
            throw new RuntimeException("데이터 확인 불가",e);
        }
        if(loginId== null || password==null){
            throw new RuntimeException("아이디 또는 비밀번호가 전당되지 않았습니다.");
        }

        //토큰 생성 및 반환
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(loginId, password);
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain, Authentication authentication) throws IOException {

        log.info("로그인 로직 실행");
        String userId = authentication.getName();

        UserEntity user = userRepository.findByUserid(userId);
        if(user==null){
            throw new RuntimeException("사용자를 찾을 수 없습니다."+userId);

        }

        if(user.getRole().equals("BAD")){
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\" 불량유저로 등록되었습니다. 관리자에게 문의 하세요\"}");
            return;
        }

        if(user.getRole().equals("EXIT")){
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\" 탈퇴유저로 등록되었습니다. 관리자에게 문의 하세요.\"}");
            return;
        }

        String accessToken = jwtUtil.generateToken(user.toDto(),"access");
        String refreshToken = jwtUtil.generateToken(user.toDto(),"refresh");

        tokenService.saveRefreshToken(new Token(UUID.randomUUID().toString(),refreshToken));

        Map<String, Object> responseBody = Map.of(
                "accessToken",accessToken,
                "refreshToken",refreshToken,
                "userId",userId,
                "name", user.getName(),
                "role",user.getRole().equals("ADMIN") ? "ADMIN" : "USER",
                "autoLoginFlag",user.getAutoLoginFlag().equals("Y") ? "Y" : "N"
        );
        response.setContentType("application/json; charset=utf-8");
        new ObjectMapper().writeValue(response.getWriter(), responseBody);
    }
    // 로그인 실패시
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=utf-8");

        String errorMessage;
        if(failed.getMessage().contains("Bad credentials")){
            errorMessage = "아이디와 비밀번호를 확인해주세요.";
        } else if(failed.getMessage().contains("사용자를 찾을 수 없습니다.")){
            errorMessage = "ID 가 없으면 사용자를 찾을 수 없습니다.";
        } else {
            errorMessage = "로그인 실패 : 알수 없는 오류가 발생했습니다.";
        }

        response.getWriter().write(String.format("{\"error\":\"%s\"}",errorMessage));
    }
    //변경적용용


}
