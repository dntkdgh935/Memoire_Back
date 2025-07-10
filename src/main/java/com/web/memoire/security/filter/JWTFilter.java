package com.web.memoire.security.filter;

import com.web.memoire.security.jwt.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;


    private boolean isExcludedUrl(String url){
        return url.equals("/")
                || url.equals("/login")
                || url.equals("/js/**")
                || url.equals("/sign")
                || url.startsWith("/upload_files/")     // ✅ 이미지 경로 예외처리
                || url.startsWith("/api/"); // ✅ 여기에 추가
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.info("JWTFilter 작동중 - requestURI: {}", requestURI);

        // 그냥 통과시킬 url
        if(isExcludedUrl(requestURI)){
            log.info("토큰 검사없이 통과 : "+requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String accessTokenHeader = request.getHeader("Authorization");
        String refreshTokenHeader = request.getHeader("RefreshToken");

        if(accessTokenHeader == null || accessTokenHeader.isEmpty()){
            log.warn("Authorization header is empty");
        }
        if (refreshTokenHeader == null || refreshTokenHeader.isEmpty()){
            log.warn("Refresh header is empty");
        }

        try{
            if ((accessTokenHeader!=null && refreshTokenHeader != null)){
                String accessToken = accessTokenHeader.substring("Bearer ".length());
                String refreshToken = refreshTokenHeader.substring("Bearer ".length());

                //Refresh 만료, Access 유효
                if(!jwtUtil.isTokenExpired(accessToken) && jwtUtil.isTokenExpired(refreshToken)){
                    log.warn("RefreshToken 유효, AccessToken 만료.");
                    // 요청 에러에 대한 스트림 열어서 에러 정보를 클라이언트에게 보냄
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setHeader("token-expired", "AccessToken");
                    response.getWriter().write("{\"error\":\"AccessToken expired\"}");
                    return;
                }
            } else {
                // 둘 다 null 이면
                log.warn("RefreshToken Null, AccessToken Null.");
                // 요청 에러에 대한 스트림 열어서 에러 정보를 클라이언트에게 보냄
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"missing or invalid tokens\"}");
                return;
            }

            //두 토큰 정상이면 통과
            filterChain.doFilter(request, response);

        }catch (Exception e){
            log.error("JWTFilter 에서 토큰 검사 중 에러 발생함", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal Server Error\"}");
        }

    }
}
