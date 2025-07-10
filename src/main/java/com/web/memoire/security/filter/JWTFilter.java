package com.web.memoire.security.filter;

import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.model.service.CustomUserDetailsService; // CustomUserDetailsService 임포트 추가
// CustomUserDetails 임포트는 CustomUserDetailsService가 Spring Security의 User를 반환하므로 더 이상 필요 없습니다.
// import com.web.memoire.security.model.dto.CustomUserDetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // UsernamePasswordAuthenticationToken 임포트 추가
import org.springframework.security.core.Authentication; // Authentication 임포트 추가
import org.springframework.security.core.context.SecurityContextHolder; // SecurityContextHolder 임포트 추가
import org.springframework.security.core.userdetails.UserDetails; // Spring Security의 UserDetails 임포트 추가
import org.springframework.security.core.userdetails.UsernameNotFoundException; // 사용자 없는 경우 처리 임포트 추가
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService; // CustomUserDetailsService 주입 추가

    // 인증이 필요 없는 경로 목록 (변경)
    private static final List<String> PERMIT_ALL_PATHS_EXACT = Arrays.asList(
            "/", "/login", "/reissue", "/user/signup", "/user/idcheck", "/favicon.ico", "/manifest.json" // /user/idcheck 추가
    );
    private static final List<String> PERMIT_ALL_PATHS_START_WITH = Arrays.asList(
            "/js/", "/css/", "/public/", "/api/", "/upload_files/" // /js/** -> /js/ 로 변경, /api/ 추가
    );


    private boolean isExcludedUrl(String url) {
        // 정확히 일치하는 경로 확인
        if (PERMIT_ALL_PATHS_EXACT.contains(url)) {
            return true;
        }
        // 특정 문자열로 시작하는 경로 확인 (변경)
        for (String prefix : PERMIT_ALL_PATHS_START_WITH) {
            if (url.startsWith(prefix)) {
                return true;
            }
        }
        // .png 파일과 같은 확장자 처리 (기존 로직 유지)
        if (url.endsWith(".png")) {
            return true;
        }
        return false;

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.info("JWTFilter 작동중 - requestURI: {}", requestURI);

        // 토큰 검사 없이 통과시킬 URL인지 확인 (기존 로직 유지)
        if (isExcludedUrl(requestURI)) {
            log.info("토큰 검사없이 통과 : " + requestURI);
            filterChain.doFilter(request, response);
            return; // 다음 필터로 넘기고 종료
        }

        String accessTokenHeader = request.getHeader("Authorization");
        String refreshTokenHeader = request.getHeader("RefreshToken");

        // AccessToken 또는 RefreshToken이 없는 경우 (인증이 필요한 요청인데 토큰이 없음) (변경)
        if (accessTokenHeader == null || !accessTokenHeader.startsWith("Bearer ") ||
                refreshTokenHeader == null || !refreshTokenHeader.startsWith("Bearer ")) {
            log.warn("인증 토큰이 없거나 형식이 올바르지 않습니다. URI: {}", requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
            response.getWriter().write("{\"error\":\"Missing or invalid authentication tokens\"}");
            return;
        }

        try {
            String accessToken = accessTokenHeader.substring("Bearer ".length());
            String refreshToken = refreshTokenHeader.substring("Bearer ".length());

            boolean isAccessTokenExpired = jwtUtil.isTokenExpired(accessToken);
            boolean isRefreshTokenExpired = jwtUtil.isTokenExpired(refreshToken);

            // RefreshToken 만료, AccessToken 유효 (이 경우는 사실상 RefreshToken이 만료되었으므로 재로그인 필요) (기존 로직 유지)
            if (!isAccessTokenExpired && isRefreshTokenExpired) {
                log.warn("RefreshToken이 만료되었습니다. AccessToken은 유효하지만 재로그인이 필요합니다. URI: {}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.setHeader("token-expired", "RefreshToken"); // 프론트엔드에 RefreshToken 만료 알림
                response.getWriter().write("{\"error\":\"RefreshToken expired, please re-login\"}");
                return;
            }

            // AccessToken 만료, RefreshToken 유효 (재발급 필요) (기존 로직 유지)
            if (isAccessTokenExpired && !isRefreshTokenExpired) {
                log.warn("AccessToken이 만료되었습니다. RefreshToken은 유효합니다. 재발급 로직을 확인하세요. URI: {}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.setHeader("token-expired", "AccessToken"); // 프론트엔드에 AccessToken 만료 알림
                response.getWriter().write("{\"error\":\"AccessToken expired, try reissue\"}");
                return;
            }

            // 두 토큰 모두 만료된 경우 (기존 로직 유지)
            if (isAccessTokenExpired && isRefreshTokenExpired) {
                log.warn("AccessToken과 RefreshToken 모두 만료되었습니다. 재로그인이 필요합니다. URI: {}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.getWriter().write("{\"error\":\"Both tokens expired, please re-login\"}");
                return;
            }

            // 두 토큰 모두 유효한 경우 또는 AccessToken만 유효한 경우 (Refresh는 위에서 처리됨)
            // JWT 토큰에서 사용자 이름 추출 (기존 로직 유지)
            String username = jwtUtil.getUsername(accessToken);

            // CustomUserDetailsService를 통해 UserDetails 객체 로드 (변경)
            UserDetails userDetails = null;
            try {
                userDetails = userDetailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) { // UsernameNotFoundException 처리 추가
                log.warn("JWT 토큰의 사용자({})를 DB에서 찾을 수 없습니다. URI: {}", username, requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.getWriter().write("{\"error\":\"User not found or invalid token\"}");
                return;
            }

            // Authentication 객체 생성 (변경)
            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null, // 비밀번호는 인증 후에는 필요 없으므로 null
                    userDetails.getAuthorities() // UserDetails에서 가져온 권한 설정 (변경)
            );

            // SecurityContextHolder에 Authentication 객체 설정 (추가)
            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.info("JWT 인증 성공: 사용자 {}", username);

            // 다음 필터로 요청 전달 (기존 로직 유지)
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWTFilter 에서 토큰 검사 중 에러 발생함: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 토큰 파싱 오류 등은 401로 처리
            response.getWriter().write("{\"error\":\"Invalid token or internal server error\"}");
        }
    }
}
