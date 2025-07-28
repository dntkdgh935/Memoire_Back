package com.web.memoire.security.filter;

import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.model.service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    // ✅ 인증이 필요 없는 정확한 경로 목록 (수정: user//face-login -> user/face-login)
    private static final List<String> PERMIT_ALL_PATHS_EXACT = Arrays.asList(
            "/", "/login", "/reissue", "/user/signup", "/user/idcheck","/user/findid","/user/findpwd", "/favicon.ico", "/manifest.json",
            "/user/social", "/user/socialSignUp","/user/check-phone","/api/verification/generate-code","/api/verification/verify-code",
            "/user/social/complete-signup",
            "/user/face-login", // 이중 슬래시 제거
            "/oauth2/authorization/", "/oauth2/callback/success", "/api/library/top5tags"
    );
    // ✅ 인증이 필요 없는 특정 문자열로 시작하는 경로 목록
    private static final List<String> PERMIT_ALL_PATHS_START_WITH = Arrays.asList(
            "/js/", "/css/", "/public/", /*"/api/",*/ "/upload_files/", "/auth/", "/chat/", "/api/library/discover/guest", "/api/library/collection/guest"
            , "/api/library/collection/memories"// /auth/ 경로도 추가 (SecurityConfig 참고)
    );


    private boolean isExcludedUrl(String url) {
        log.info("isExcludedUrl 호출됨 - 검사할 URL: '{}'", url);
        // 정확히 일치하는 경로 확인
        if (PERMIT_ALL_PATHS_EXACT.contains(url)) {
            log.info("isExcludedUrl: 정확히 일치하는 PermitAll 경로 발견: '{}'", url);
            return true;
        }
        log.info("isExcludedUrl: 정확히 일치하는 경로 없음. PERMIT_ALL_PATHS_EXACT: {}", PERMIT_ALL_PATHS_EXACT);

        // 특정 문자열로 시작하는 경로 확인
        for (String prefix : PERMIT_ALL_PATHS_START_WITH) {
            if (url.startsWith(prefix)) {
                log.info("isExcludedUrl: 시작 부분이 일치하는 PermitAll 경로 발견: '{}' (프리픽스: '{}')", url, prefix);
                return true;
            }
        }
        log.info("isExcludedUrl: 시작 부분이 일치하는 경로 없음. PERMIT_ALL_PATHS_START_WITH: {}", PERMIT_ALL_PATHS_START_WITH);


        // ✅ 재열
        if (url.startsWith("/atelier/")) {
            log.info("isExcludedUrl: /atelier/ 시작 부분이 일치하는 PermitAll 경로 발견: '{}'", url);
            return true;
        }


        // .png, .jpg 파일과 같은 확장자 처리 (기존 로직 유지)
        if (url.endsWith(".png") || url.endsWith(".jpg")) { // .jpg도 추가
            log.info("isExcludedUrl: 이미지 파일 확장자 PermitAll 경로 발견: '{}'", url);
            return true;
        }

        log.info("isExcludedUrl: '{}'는 PermitAll 경로에 해당하지 않습니다.", url);
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.info("JWTFilter 작동중 - requestURI: {}", requestURI);

        // 토큰 검사 없이 통과시킬 URL인지 확인
        if (isExcludedUrl(requestURI)) {
            log.info("JWTFilter: 토큰 검사없이 통과 (PermitAll 경로): {}", requestURI);
            filterChain.doFilter(request, response);
            return; // 다음 필터로 넘기고 종료
        }

        String accessTokenHeader = request.getHeader("Authorization");
        String refreshTokenHeader = request.getHeader("RefreshToken");

        // AccessToken 또는 RefreshToken이 없는 경우 (인증이 필요한 요청인데 토큰이 없음)
        // 이 조건은 AccessToken 또는 RefreshToken 중 하나라도 없거나 형식이 틀리면 401을 반환합니다.
        // AuthProvider.secureApiRequest가 항상 두 토큰을 보내므로, 이 검사가 유효합니다.
        if (accessTokenHeader == null || !accessTokenHeader.startsWith("Bearer ") ||
                refreshTokenHeader == null || !refreshTokenHeader.startsWith("Bearer ")) {
            log.warn("인증 토큰이 없거나 형식이 올바르지 않습니다. URI: {}", requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"error\":\"Missing or invalid authentication tokens\"}");
            return;
        }

        try {
            String accessToken = accessTokenHeader.substring("Bearer ".length()).trim();
            String refreshToken = refreshTokenHeader.substring("Bearer ".length()).trim();

            boolean isAccessTokenExpired = jwtUtil.isTokenExpired(accessToken);
            boolean isRefreshTokenExpired = jwtUtil.isTokenExpired(refreshToken);

            // RefreshToken 만료, AccessToken 유효 (이 경우는 RefreshToken이 만료되었으므로 재로그인 필요)
            if (!isAccessTokenExpired && isRefreshTokenExpired) {
                log.warn("RefreshToken이 만료되었습니다. AccessToken은 유효하지만 재로그인이 필요합니다. URI: {}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.setHeader("token-expired", "RefreshToken"); // 프론트엔드에 RefreshToken 만료 알림
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"error\":\"RefreshToken expired, please re-login\"}");
                return;
            }

            // AccessToken 만료, RefreshToken 유효 (재발급 필요)
            if (isAccessTokenExpired && !isRefreshTokenExpired) {
                log.warn("AccessToken이 만료되었습니다. RefreshToken은 유효합니다. 재발급 로직을 확인하세요. URI: {}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.setHeader("token-expired", "AccessToken"); // 프론트엔드에 AccessToken 만료 알림
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"error\":\"AccessToken expired, try reissue\"}");
                return;
            }

            // 두 토큰 모두 만료된 경우
            if (isAccessTokenExpired && isRefreshTokenExpired) {
                log.warn("AccessToken과 RefreshToken 모두 만료되었습니다. 재로그인이 필요합니다. URI: {}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"error\":\"Both tokens expired, please re-login\"}");
                return;
            }

            // 두 토큰 모두 유효한 경우 (또는 AccessToken만 유효한 경우 - Refresh는 위에서 처리됨)
            String userId = jwtUtil.getUsername(accessToken);

            UserDetails userDetails = null;
            try {
                userDetails = userDetailsService.loadUserByUsername(userId);
            } catch (UsernameNotFoundException e) {
                log.warn("JWT 토큰의 사용자({})를 DB에서 찾을 수 없습니다. URI: {}", userId, requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"error\":\"User not found or invalid token\"}");
                return;
            }

            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.info("JWT 인증 성공: 사용자 {}", userId);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // 이 예외는 isTokenExpired()에서 이미 처리되었어야 하지만, 혹시 모를 경우를 대비
            log.error("JWTFilter: 만료된 JWT 토큰 예외 처리. URI: {}: {}", requestURI, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("token-expired", "true"); // 만료되었음을 알림
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"error\":\"Expired JWT token.\"}");
        } catch (Exception e) {
            log.error("JWTFilter 에서 토큰 검사 중 예상치 못한 에러 발생함. URI: {}: {}", requestURI, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"error\":\"Invalid token or internal server error\"}");
        }
    }
}
