package com.web.memoire.security.controller;

import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.user.model.dto.User;
import com.web.memoire.user.model.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ReIssueController {

    private final JWTUtil jwtUtil;
    private final UserService userService;
    private final TokenService tokenService;

    @PostMapping("/reissue")
    public ResponseEntity<?> reissueToken(HttpServletRequest request, HttpServletResponse response) {
        log.info("ReissueController 실행");

        String accessTokenHeader = request.getHeader("Authorization");
        String refreshTokenHeader = request.getHeader("RefreshToken");
        String extendLogin = request.getHeader("ExtendLogin"); // "true" or "false" (or null)

        log.info("accessTokenHeader: {} ", accessTokenHeader);
        log.info("refreshTokenHeader: {} ", refreshTokenHeader);
        log.info("extendLogin: {} ", extendLogin);

        // 1. 토큰 값 추출 (Bearer 접두사 제거)
        String accessToken = (accessTokenHeader != null && accessTokenHeader.startsWith("Bearer "))
                ? accessTokenHeader.substring("Bearer ".length()).trim() : null;
        String refreshToken = (refreshTokenHeader != null && refreshTokenHeader.startsWith("Bearer "))
                ? refreshTokenHeader.substring("Bearer ".length()).trim() : null;

        // 2. 토큰 존재 여부 및 유효성 초기 검사
        if (refreshToken == null) { // Refresh Token이 없으면 재발급 불가, 로그인 필요
            log.warn("RefreshToken is not provided. Cannot reissue tokens.");
            // RefreshToken이 없으므로 재로그인을 유도합니다.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Refresh Token이 없습니다. 다시 로그인해주세요."));
        }

        try {
            // 3. Refresh Token 만료 여부 확인
            boolean isRefreshTokenExpired = jwtUtil.isTokenExpired(refreshToken);
            log.info("isRefreshTokenExpired: {}", isRefreshTokenExpired ? "만료됨" : "유효함");

            if (isRefreshTokenExpired) {
                // Refresh Token이 만료된 경우 (로그인 세션 종료)
                String userIdFromRefresh = jwtUtil.getUsername(refreshToken); // 만료되어도 username은 추출 가능
                tokenService.deleteRefreshToken(userIdFromRefresh); // DB에서 해당 userId의 리프레시 토큰 삭제
                log.info("Refresh Token expired. User ID: {}. Login session terminated.", userIdFromRefresh);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인 세션이 만료되었습니다. 다시 로그인해주세요."));
            }

            // 4. Refresh Token이 유효한 경우
            String userIdFromRefresh = jwtUtil.getUsername(refreshToken);
            User user = userService.selectUser(userIdFromRefresh);

            if (user == null) {
                log.warn("User not found for userId extracted from refresh token: {}", userIdFromRefresh);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요."));
            }

            // 불량/탈퇴 유저 체크 (로그인 필터, OAuth2 핸들러와 일관성 유지)
            if(user.getRole().equals("BAD")){
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).body(Map.of("error","불량유저로 등록되었습니다. 관리자에게 문의 하세요"));
            }
            if(user.getRole().equals("EXIT")){
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).body(Map.of("error","탈퇴유저로 등록되었습니다. 관리자에게 문의 하세요."));
            }


            // 5. Access Token 만료 여부 확인 및 처리
            boolean isAccessTokenExpired = (accessToken != null) && jwtUtil.isTokenExpired(accessToken);
            log.info("isAccessTokenExpired: {}", isAccessTokenExpired ? "만료됨" : "유효함");

            // 새로운 Access Token 발급 (Refresh Token이 유효하면 항상 발급)
            String newAccessToken = jwtUtil.generateToken(user, "access");
            response.setHeader("Authorization", "Bearer " + newAccessToken);
            response.setHeader("Access-Control-Expose-Headers", "Authorization, RefreshToken"); // RefreshToken 헤더도 노출하도록 추가

            // 6. ExtendLogin 요청이 있고, Refresh Token 갱신이 필요한 경우
            if ("true".equalsIgnoreCase(extendLogin)) {
                log.info("ExtendLogin requested. Reissuing Refresh Token.");
                // 새로운 Refresh Token 생성
                String newRefreshToken = jwtUtil.generateToken(user, "refresh");

                // DB에 기록된 Refresh Token 값 수정 (기존 토큰 삭제 후 새 토큰 저장)
                // 현재 TokenService의 updateRefreshToken 로직에 따라 유연하게 변경
                // 예를 들어, oldRefreshToken을 찾아서 newRefreshToken으로 업데이트하는 방식
                String id = tokenService.selectId(userIdFromRefresh, refreshToken); // 기존 RefreshToken으로 ID 조회
                if (id != null) {
                    tokenService.updateRefreshToken(id, newRefreshToken); // DB 업데이트
                    log.info("Refresh Token updated in DB for userId: {}", userIdFromRefresh);
                } else {
                    // DB에 기존 Refresh Token이 없는 경우 (예: 서버 재시작으로 인한 데이터 유실 등)
                    // 새 Refresh Token을 저장하거나, 에러 처리
                    log.warn("Existing refresh token not found in DB for userId: {}. Saving new refresh token.", userIdFromRefresh);
                    tokenService.saveRefreshToken(new com.web.memoire.security.jwt.jpa.entity.Token(userIdFromRefresh, newRefreshToken));
                }

                // 응답 헤더에 새로운 Refresh Token 추가
                response.setHeader("RefreshToken", "Bearer " + newRefreshToken);
            }

            // 응답 바디 (Access Token과 Refresh Token 재발급 여부 메시지)
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Tokens reissued successfully");
            responseBody.put("userId", user.getUserId()); // Front-end에서 userId도 필요할 수 있음
            responseBody.put("name", user.getName());
            responseBody.put("nickname", user.getNickname());
            responseBody.put("role", user.getRole());
            responseBody.put("autoLoginFlag", user.getAutoLoginFlag());

            return ResponseEntity.ok(responseBody);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Refresh Token이 만료되었을 때 getUsername에서 발생 가능
            log.error("Expired JWT during reissue: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "만료된 토큰입니다. 다시 로그인해주세요."));
        } catch (Exception e) {
            log.error("/reissue 요청 처리중 오류 발생 : ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "토큰 재발급 중 서버 오류가 발생했습니다."));
        }
    }
}