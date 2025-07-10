package com.web.memoire.security.handler;

import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.model.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    private final TokenService tokenService;
    private final JWTUtil jwtUtil;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String accessToken = authorization.substring("Bearer ".length()).trim();

            try{
                String userId = jwtUtil.getUsername(accessToken);
                if (userId != null) {
                    String Token = tokenService.selectToken(userId);

                    tokenService.deleteRefreshToken(tokenService.selectId(userId, Token));

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write("로그아웃 성공");
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try {
                    response.getWriter().write("로그아웃 처리 중 오류 발생");
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try {
                response.getWriter().write("유효하지 않은 요청");
            } catch (Exception e1) {
                e1.printStackTrace();

            }
        }

    }
}
