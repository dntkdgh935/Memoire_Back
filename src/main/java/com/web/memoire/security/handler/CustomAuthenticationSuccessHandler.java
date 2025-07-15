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
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser; // DefaultOidcUser import 추가
import org.springframework.security.oauth2.core.user.OAuth2User; // OAuth2User import 추가
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler; // SimpleUrlAuthenticationSuccessHandler import 추가
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder; // URI 빌더 추가

import java.io.IOException;
import java.net.URLEncoder; // URL 인코딩을 위해 추가
import java.nio.charset.StandardCharsets; // StandardCharsets 추가
import java.util.Optional; // Optional import 추가

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    // 프론트엔드의 소셜 로그인 성공 후 리다이렉트될 기본 URL (http://localhost:3000)
    // 실제 배포 시에는 이 값을 properties/yml에서 주입받거나, 환경 변수로 관리하는 것이 좋습니다.
    private String frontendBaseUrl = "http://localhost:3000";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 인증된 Principal 객체를 가져옵니다.
        Object principal = authentication.getPrincipal();

        CustomOAuth2User customOAuth2User = null;

        // Principal이 CustomOAuth2User 타입인지 안전하게 확인합니다.
        if (principal instanceof CustomOAuth2User) {
            customOAuth2User = (CustomOAuth2User) principal;
        } else if (principal instanceof DefaultOidcUser) {
            // DefaultOidcUser가 들어오는 경우 (DevTools 또는 설정 문제)
            DefaultOidcUser oidcUser = (DefaultOidcUser) principal;
            log.warn("Principal is DefaultOidcUser, not CustomOAuth2User. This might indicate a Spring Boot DevTools classloader issue or a misconfiguration in SecurityConfig. Attempting to proceed with available OIDC user info.");

            // DefaultOidcUser의 정보를 기반으로 CustomOAuth2User를 생성하거나,
            // 최소한의 정보를 사용하여 처리 로직을 이어갈 수 있도록 합니다.
            // 여기서는 CustomOAuth2User가 아닌 다른 타입이 들어왔으므로,
            // CustomOAuth2User의 커스텀 필드(isNewUser, needsAdditionalInfo 등)에 직접 접근할 수 없습니다.
            // 따라서, 이 경우의 처리 로직은 애플리케이션의 요구사항에 따라 달라질 수 있습니다.
            // 예를 들어, 오류 페이지로 리다이렉트하거나, 기본 로그인 성공 처리로 넘어갈 수 있습니다.
            // 현재 코드에서는 CustomOAuth2User가 아니면 이후 로직이 실행되지 않도록 return 합니다.
            // 만약 DefaultOidcUser도 처리하고 싶다면, 이 블록 내에서 적절한 리다이렉션 또는 오류 처리를 해야 합니다.

            // 현재는 CustomOAuth2User가 아니면 아래 로직을 실행하지 않고,
            // 기본 성공 핸들러의 동작을 따르거나, 특정 오류 페이지로 리다이렉트하도록 처리할 수 있습니다.
            // 여기서는 간단히 오류 메시지를 반환하고 종료합니다.
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"error\":\"인증된 사용자 객체 타입 불일치. 관리자에게 문의하세요.\"}");
            return;

        } else if (principal instanceof OAuth2User) {
            // 다른 일반 OAuth2User 구현체인 경우
            OAuth2User oauth2User = (OAuth2User) principal;
            log.warn("Principal is a generic OAuth2User (e.g., DefaultOAuth2User), not CustomOAuth2User or DefaultOidcUser. Cannot access CustomOAuth2User specific fields.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"error\":\"인증된 사용자 객체 타입 불일치. 관리자에게 문의하세요.\"}");
            return;
        } else {
            // 예상치 못한 Principal 타입
            log.error("Unexpected principal type in CustomAuthenticationSuccessHandler: {}", principal.getClass().getName());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"error\":\"예상치 못한 인증 Principal 타입입니다. 관리자에게 문의하세요.\"}");
            return;
        }

        // customOAuth2User가 null인 경우 (위에서 처리되지 않은 경우)
        if (customOAuth2User == null) {
            log.error("CustomOAuth2User is null after principal check. This should not happen if previous checks are exhaustive.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"error\":\"인증 처리 중 오류가 발생했습니다. 관리자에게 문의하세요.\"}");
            return;
        }

        // 이제 customOAuth2User 객체를 안전하게 사용할 수 있습니다.
        String userId = customOAuth2User.getUserId();
        boolean isNewUser = customOAuth2User.isNewUser();
        boolean needsAdditionalInfo = customOAuth2User.needsAdditionalInfo();
        String socialType = customOAuth2User.getSocialType();
        String socialId = customOAuth2User.getSocialId();

        // Attribute 값이 null일 경우에 대한 안전한 처리 (Map.get()은 null 반환 가능)
        String loginId = (String) customOAuth2User.getAttributes().get("loginId");
        String name = (String) customOAuth2User.getAttributes().get("name");
        String nickname = (String) customOAuth2User.getAttributes().get("nickname");

        log.info("Social Login Success! userId: {}, isNewUser: {}, needsAdditionalInfo: {}, socialType: {}",
                userId, isNewUser, needsAdditionalInfo, socialType);

        if (needsAdditionalInfo) {
            // 추가 정보가 필요한 경우
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/user/socialSignUp")
                    .queryParam("userId", URLEncoder.encode(userId, StandardCharsets.UTF_8)) // userId는 항상 존재하므로 인코딩
                    .queryParam("socialType", URLEncoder.encode(socialType, StandardCharsets.UTF_8))
                    .queryParam("socialId", URLEncoder.encode(socialId, StandardCharsets.UTF_8))
                    // null 체크 후 인코딩하여 쿼리 파라미터 추가
                    .queryParamIfPresent("loginId", (loginId != null && !loginId.isEmpty()) ? Optional.of(URLEncoder.encode(loginId, StandardCharsets.UTF_8)) : Optional.empty())
                    .queryParamIfPresent("name", (name != null && !name.isEmpty()) ? Optional.of(URLEncoder.encode(name, StandardCharsets.UTF_8)) : Optional.empty())
                    .queryParamIfPresent("nickname", (nickname != null && !nickname.isEmpty()) ? Optional.of(URLEncoder.encode(nickname, StandardCharsets.UTF_8)) : Optional.empty())
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

            // 프론트엔드의 특정 콜백 URL로 토큰을 쿼리 파라미터로 전달하여 리다이렉트
            String finalRedirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/oauth2/callback/success")
                    .queryParam("accessToken", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                    .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                    // UserEntity에서 직접 정보를 가져와 쿼리 파라미터에 추가 (null 체크 포함)
                    .queryParamIfPresent("userId", (user.getUserId() != null && !user.getUserId().isEmpty()) ? Optional.of(URLEncoder.encode(user.getUserId(), StandardCharsets.UTF_8)) : Optional.empty())
                    .queryParamIfPresent("name", (user.getName() != null && !user.getName().isEmpty()) ? Optional.of(URLEncoder.encode(user.getName(), StandardCharsets.UTF_8)) : Optional.empty())
                    .queryParamIfPresent("role", (user.getRole() != null && !user.getRole().isEmpty()) ? Optional.of(URLEncoder.encode(user.getRole(), StandardCharsets.UTF_8)) : Optional.empty())
                    .queryParamIfPresent("autoLoginFlag", (user.getAutoLoginFlag() != null && !user.getAutoLoginFlag().isEmpty()) ? Optional.of(URLEncoder.encode(user.getAutoLoginFlag(), StandardCharsets.UTF_8)) : Optional.empty())
                    .queryParamIfPresent("nickname", (user.getNickname() != null && !user.getNickname().isEmpty()) ? Optional.of(URLEncoder.encode(user.getNickname(), StandardCharsets.UTF_8)) : Optional.empty())
                    .build()
                    .toUriString();

            log.info("Redirecting to frontend success callback: {}", finalRedirectUrl);
            response.sendRedirect(finalRedirectUrl);
        }
    }
}
