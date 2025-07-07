package com.web.memoire.security.jwt;

import com.web.memoire.user.model.dto.User;
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

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access_expiration}")
    private String accessExpiration;

    @Value("${jwt.refresh_expiration}")
    private String refreshExpiration;

    public String generateToken(User user, String category) {
        return Jwts.builder()
                .setSubject(user.getUserId())
                .claim("category",category)
                .claim("name", user.getName())
                .claim("role", user.getRole().equals("ADMIN")? "ADMIN" : "USER")
                .setExpiration(new Date(
                        System.currentTimeMillis() + (category.equals("access")? accessExpiration : refreshExpiration)))
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        if(token ==null || token.trim().isEmpty()){
            log.error("token is empty");
            throw new IllegalArgumentException("token is empty");
        }

        try{
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token.trim())
                    .getBody();
        } catch (ExpiredJwtException e){
            log.error("token expired");
            return e.getClaims();
        } catch (Exception e){
            log.error("token error");
            throw e;
        }
    }

    public boolean isTokenExpired(String token) {
        boolean isExpired = getClaimsFromToken(token).getExpiration().before(new Date());
        return isExpired;
    }
    // 토큰에서 사용자 ID 추출
    public String getUseridFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    // 토큰에서 role 정보 추출
    public String getAuthorityFromToken(String token) {
        return getClaimsFromToken(token).get("role", String.class);
    }

    // 토큰에서 카테고리 정보 추출
    public String getCategoryFromToken(String token) {
        return getClaimsFromToken(token).get("category", String.class);
    }

}
