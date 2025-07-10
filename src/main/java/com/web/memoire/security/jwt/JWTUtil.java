package com.web.memoire.security.jwt;

import com.web.memoire.user.model.dto.User; // User DTO 임포트
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class JWTUtil {

    // application.properties 에서 설정된 JWT 비밀 키를 주입받습니다.
    @Value("${jwt.secret}")
    private String secretKey;

    // application.properties 에서 설정된 Access Token 만료 시간을 주입받습니다. (밀리초 단위)
    @Value("${jwt.access_expiration}")
    private Long accessExpiration; // Long 타입으로 변경됨

    // application.properties 에서 설정된 Refresh Token 만료 시간을 주입받습니다. (밀리초 단위)
    @Value("${jwt.refresh_expiration}")
    private Long refreshExpiration; // Long 타입으로 변경됨

    /**
     * JWT 토큰을 생성합니다.
     * @param user 토큰에 포함될 사용자 정보 (User DTO)
     * @param category 토큰의 종류 ("access" 또는 "refresh")
     * @return 생성된 JWT 문자열
     */
    public String generateToken(User user, String category) {
        // 토큰 만료 시간 설정: category에 따라 accessExpiration 또는 refreshExpiration 적용
        long expirationTime = category.equals("access") ? accessExpiration : refreshExpiration;

        return Jwts.builder()
                .setSubject(user.getUserId()) // 토큰의 주체 (여기서는 사용자 ID)
                .claim("category", category) // 토큰의 카테고리 (access/refresh)
                .claim("name", user.getName()) // 사용자 이름
                .claim("role", user.getRole() != null && user.getRole().equals("ADMIN") ? "ADMIN" : "USER") // 사용자 권한
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // 현재 시간 + 만료 시간
                .signWith(SignatureAlgorithm.HS512, secretKey.getBytes()) // HS512 알고리즘과 비밀 키로 서명 (비밀 키를 바이트 배열로 변환)
                .compact(); // JWT 생성
    }

    /**
     * JWT 토큰에서 클레임(Claims) 정보를 추출합니다.
     * 토큰이 만료되었을 경우에도 ExpiredJwtException을 캐치하여 클레임을 반환합니다.
     * @param token JWT 문자열
     * @return 토큰의 클레임 정보
     * @throws IllegalArgumentException 토큰이 null이거나 비어있을 경우
     * @throws Exception 토큰 파싱 중 다른 오류가 발생할 경우
     */
    public Claims getClaimsFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.error("토큰이 비어있습니다.");
            throw new IllegalArgumentException("token is empty");
        }

        try {
            return Jwts.parser() // 최신 JJWT 버전에서 권장되는 파서 빌더 사용
                    .setSigningKey(secretKey.getBytes()) // 서명 키 설정 (비밀 키를 바이트 배열로 변환)
                    .build()
                    .parseClaimsJws(token.trim()) // 토큰 파싱
                    .getBody(); // 클레임 본문 반환
        } catch (ExpiredJwtException e) {
            log.warn("토큰이 만료되었습니다. 클레임 정보를 반환합니다."); // 만료된 토큰도 클레임은 가져올 수 있음
            return e.getClaims();
        } catch (Exception e) {
            log.error("토큰 파싱 중 오류 발생: {}", e.getMessage(), e); // 에러 스택 트레이스 포함 로깅
            throw e;
        }
    }

    /**
     * JWT 토큰의 만료 여부를 확인합니다.
     * @param token JWT 문자열
     * @return 토큰이 만료되었으면 true, 아니면 false
     */
    public boolean isTokenExpired(String token) {
        // getClaimsFromToken 내부에서 ExpiredJwtException을 처리하므로, 여기서는 바로 만료 시간을 비교합니다.
        return getClaimsFromToken(token).getExpiration().before(new Date());
    }

    /**
     * JWT 토큰에서 사용자 ID (subject)를 추출합니다.
     * @param token JWT 문자열
     * @return 사용자 ID
     */
    public String getUsername(String token) { // JWTFilter에서 사용하는 메서드 이름과 일치하도록 변경됨
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * JWT 토큰에서 사용자 권한 (role)을 추출합니다.
     * @param token JWT 문자열
     * @return 사용자 권한 (예: "USER", "ADMIN")
     */
    public String getRole(String token) { // JWTFilter에서 사용하는 메서드 이름과 일치하도록 변경됨
        return getClaimsFromToken(token).get("role", String.class);
    }

    /**
     * JWT 토큰에서 카테고리 (category)를 추출합니다.
     * @param token JWT 문자열
     * @return 토큰 카테고리 (예: "access", "refresh")
     */
    public String getCategoryFromToken(String token) {
        return getClaimsFromToken(token).get("category", String.class);
    }
}
