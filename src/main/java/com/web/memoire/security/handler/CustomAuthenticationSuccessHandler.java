package com.web.memoire.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 응답을 사용하지 않으므로 필요 없을 수 있음.
import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.jpa.entity.Token;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.security.oauth2.CustomOAuth2User;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder; // URI 빌더 추가

import java.io.IOException;
import java.net.URLEncoder; // URL 인코딩을 위해 추가
import java.nio.charset.StandardCharsets; // StandardCharsets 추가
// import java.util.HashMap; // JSON 응답을 사용하지 않으므로 필요 없을 수 있음.
// import java.util.Map; // JSON 응답을 사용하지 않으므로 필요 없을 수 있음.

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    // private final ObjectMapper objectMapper = new ObjectMapper(); // ✅ JSON 응답을 직접 보내지 않으므로 필요 없습니다.

    // 프론트엔드의 소셜 로그인 성공 후 리다이렉트될 기본 URL (http://localhost:3000)
    // 실제 배포 시에는 이 값을 properties/yml에서 주입받거나, 환경 변수로 관리하는 것이 좋습니다.
    private String frontendBaseUrl = "http://localhost:3000";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        String userId = oAuth2User.getUserId();
        boolean isNewUser = oAuth2User.isNewUser();
        boolean needsAdditionalInfo = oAuth2User.needsAdditionalInfo();
        String socialType = oAuth2User.getSocialType();
        String socialId = oAuth2User.getSocialId();

        // Attribute 값이 null일 경우에 대한 안전한 처리 (Map.get()은 null 반환 가능)
        String loginId = (String) oAuth2User.getAttributes().get("loginId");
        String name = (String) oAuth2User.getAttributes().get("name");
        String nickname = (String) oAuth2User.getAttributes().get("nickname");

        log.info("Social Login Success! userId: {}, isNewUser: {}, needsAdditionalInfo: {}, socialType: {}",
                userId, isNewUser, needsAdditionalInfo, socialType);

        if (needsAdditionalInfo) {
            // 추가 정보가 필요한 경우
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/user/socialSignUp")
                    .queryParam("userId", URLEncoder.encode(userId, StandardCharsets.UTF_8)) // userId는 항상 존재하므로 인코딩
                    .queryParam("socialType", URLEncoder.encode(socialType, StandardCharsets.UTF_8))
                    .queryParam("socialId", URLEncoder.encode(socialId, StandardCharsets.UTF_8))
                    // null 체크 후 인코딩하여 쿼리 파라미터 추가
                    .queryParamIfPresent("loginId", (loginId != null && !loginId.isEmpty()) ? java.util.Optional.of(URLEncoder.encode(loginId, StandardCharsets.UTF_8)) : java.util.Optional.empty())
                    .queryParamIfPresent("name", (name != null && !name.isEmpty()) ? java.util.Optional.of(URLEncoder.encode(name, StandardCharsets.UTF_8)) : java.util.Optional.empty())
                    .queryParamIfPresent("nickname", (nickname != null && !nickname.isEmpty()) ? java.util.Optional.of(URLEncoder.encode(nickname, StandardCharsets.UTF_8)) : java.util.Optional.empty())
                    .build()
                    .toUriString();

            log.info("Redirecting to SocialSignUp.js for additional info: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } else {
            // 모든 정보가 완비된 경우
            UserEntity user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new ServletException("인증 성공 후 사용자 정보를 찾을 수 없습니다."));

            log.info("Issuing JWT for user: {}", user.getLoginId());

            if(user.getRole().equals("BAD")){
                log.warn("User {} is a BAD user. Denying access.", user.getLoginId());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"error\":\"불량유저로 등록되었습니다. 관리자에게 문의 하세요\"}");
                return;
            }

            if(user.getRole().equals("EXIT")){
                log.warn("User {} is an EXIT user. Denying access.", user.getLoginId());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"error\":\"탈퇴유저로 등록되었습니다. 관리자에게 문의 하세요.\"}");
                return;
            }

            // JWT 토큰 발급
            String accessToken = jwtUtil.generateToken(user.toDto(), "access");
            String refreshToken = jwtUtil.generateToken(user.toDto(), "refresh");

            // 리프레시 토큰 저장
            tokenService.saveRefreshToken(new Token(user.getUserId(), refreshToken));
            log.info("JWT issued and refresh token saved for user: {}", user.getLoginId());

            // ✅ finalRedirectUrl 변수 선언 및 할당
            // ✅ 프론트엔드의 특정 콜백 URL로 토큰을 쿼리 파라미터로 전달하여 리다이렉트
            String finalRedirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/oauth2/callback/success")
                    .queryParam("accessToken", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                    .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                    // UserEntity에서 직접 정보를 가져와 쿼리 파라미터에 추가 (null 체크 포함)
                    .queryParamIfPresent("userId", (user.getUserId() != null && !user.getUserId().isEmpty()) ? java.util.Optional.of(URLEncoder.encode(user.getUserId(), StandardCharsets.UTF_8)) : java.util.Optional.empty())
                    .queryParamIfPresent("name", (user.getName() != null && !user.getName().isEmpty()) ? java.util.Optional.of(URLEncoder.encode(user.getName(), StandardCharsets.UTF_8)) : java.util.Optional.empty())
                    .queryParamIfPresent("role", (user.getRole() != null && !user.getRole().isEmpty()) ? java.util.Optional.of(URLEncoder.encode(user.getRole(), StandardCharsets.UTF_8)) : java.util.Optional.empty())
                    .queryParamIfPresent("autoLoginFlag", (user.getAutoLoginFlag() != null && !user.getAutoLoginFlag().isEmpty()) ? java.util.Optional.of(URLEncoder.encode(user.getAutoLoginFlag(), StandardCharsets.UTF_8)) : java.util.Optional.empty())
                    .queryParamIfPresent("nickname", (user.getNickname() != null && !user.getNickname().isEmpty()) ? java.util.Optional.of(URLEncoder.encode(user.getNickname(), StandardCharsets.UTF_8)) : java.util.Optional.empty())
                    .build()
                    .toUriString();

            log.info("Redirecting to frontend success callback: {}", finalRedirectUrl);
            response.sendRedirect(finalRedirectUrl);
            // ✅ JSON 응답을 보내는 코드는 제거해야 합니다. (동시에 두 가지 응답 불가)
            // objectMapper.writeValue(response.getWriter(), responseBody);
        }
    }
}