package com.web.memoire.security.controller;


import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.jpa.repository.TokenRepository;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.user.jpa.repository.UserRepository;
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

@Slf4j
@RestController
@RequiredArgsConstructor
public class ReIssueController {

    private final JWTUtil jwtUtil;
    private final UserService userService;
    private final TokenService tokenService;

    @PostMapping("/reissue")
    public ResponseEntity<?>reissueToken(HttpServletRequest request, HttpServletResponse response) {
        log.info("ReissueController 실행");

        // 해더 값 꺼내기
        String accessTokenHeader = request.getHeader("Authorization");
        String refreshTokenHeader = request.getHeader("RefreshToken");
        // 로그인 연장 요청 정보도 추출함
        String extendLogin = request.getHeader("ExtendLogin");

        log.info("accessTokenHeader: {} ", accessTokenHeader);
        log.info("refreshTokenHeader: {} ", refreshTokenHeader);
        log.info("extendLogin: {} ", extendLogin);

        try {
            // 추출된 해더 정보에서 토큰값만 추출하기 : 앞에 붙은 "Bearer " 제거함
            String accessToken = accessTokenHeader != null && accessTokenHeader.startsWith("Bearer ")
                    ? accessTokenHeader.substring("Bearer ".length()).trim() : null;
            String refreshToken = refreshTokenHeader != null && refreshTokenHeader.startsWith("Bearer ")
                    ? refreshTokenHeader.substring("Bearer ".length()).trim() : null;

            if (accessToken != null || refreshToken != null) {
                log.warn("RefreshToken or AccessToken 이 제공되지 않습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid tokens");
            }

            // accessToken 이 expiration 인지 확인
            boolean isAccessTokenExpired = jwtUtil.isTokenExpired(accessToken);
            log.info("isAccessTokenExpired: {}", isAccessTokenExpired ? "만료됨" : "유효함");
            // refreshToken 이 expiration 인지 확인
            boolean isRefreshTokenExpired = jwtUtil.isTokenExpired(refreshToken);
            log.info("isRefreshTokenExpired: {}", isRefreshTokenExpired ? "만료됨" : "유효함");

            // accessToken 이 유효하고 RefreshToken 이 만료된 경우  --------------------------------
            if (!isAccessTokenExpired && isRefreshTokenExpired) {
                // 로그인 연장을 요청하였다면
                if ("true".equalsIgnoreCase(extendLogin)) {
                    String userId = jwtUtil.getUseridFromToken(accessToken);
                    // db 에서 회원정보 조회해 옴
                    User user = userService.selectUser(userId);

                    // 새로운 리프레시 토큰을 생성함
                    String newRefreshToken = jwtUtil.generateToken(user, "refresh");
                    // db 에 기록된 리프레시토큰 값 수정 처리
                    String id = tokenService.selectId(userId, refreshToken);  // id 조회
                    tokenService.updateRefreshToken(id, newRefreshToken);  // 리프레시토큰 변경

                    // 응답 객체에 기록해서 응답 처리함
                    response.setHeader("Refresh-Token", "Bearer " + newRefreshToken);
                    response.setHeader("Access-Control-Expose-Headers", "Refresh-Token");
                    return ResponseEntity.ok("RefreshToken Reissued");
                }  // 로그인 연장 요청이면
            }  // access 유효, refresh 만료된 경우

            // 둘 다 만료(true) 된 경우 (로그인 세션 종료함)  --------------------------------------
            if(isRefreshTokenExpired && isAccessTokenExpired) {
                String userId = jwtUtil.getUseridFromToken(accessToken);
                // userId, refreshToken 으로 id 조회해 옴
                String id = tokenService.selectId(userId, refreshToken);
                // 리프레시토큰 저장 테이블에서 토큰 삭제함
                tokenService.deleteRefreshToken(userId);

                // 로그아웃되게 응답 처리함
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Losgin session expired");
            }

            // access 만료, refresh 유효
            if (isAccessTokenExpired && !isRefreshTokenExpired) {
                // 리프레시 토큰에서 userId 추출
                String userId = jwtUtil.getUseridFromToken(refreshToken);
                User user = userService.selectUser(userId);

                // 새로운 accessToken 발급함
                String newAccessToken = jwtUtil.generateToken(user, "access");

                // 응답 처리함
                response.setHeader("Authorization", "Bearer " + newAccessToken);
                response.setHeader("Access-Control-Expose-Headers", "Authorization");
                return ResponseEntity.ok("Access Token Reissued");
            }  // access 만료, refresh 유효

        } catch (Exception e) {
            log.error("/reissue 요청 처리중 오류 발생 : ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to Reissue Token");
        }

        return ResponseEntity.ok("Reissue Token");
    }
}
