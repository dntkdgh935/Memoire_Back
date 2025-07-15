package com.web.memoire.security.handler;

import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.jpa.entity.Token;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.entity.SocialUserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.jpa.repository.SocialUserRepository;
import com.web.memoire.user.model.dto.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final SocialUserRepository socialUserRepository;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String socialType = oauthToken.getAuthorizedClientRegistrationId();
        String socialId = null;
        String name = null;
        String nickname = null;

        // 소셜 타입별로 socialId, name, nickname 추출 로직 강화
        if ("google".equals(socialType)) {
            socialId = (String) attributes.get("sub");
            name = (String) attributes.get("name");
            nickname = (String) attributes.get("given_name");
        } else if ("kakao".equals(socialType)) {
            Object kakaoId = attributes.get("id");
            if (kakaoId != null) {
                socialId = String.valueOf(kakaoId);
            }
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount != null) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    nickname = (String) profile.get("nickname");
                }
            }
            name = nickname;
        } else if ("naver".equals(socialType)) {
            Map<String, Object> responseMap = (Map<String, Object>) attributes.get("response");
            if (responseMap != null) {
                socialId = (String) responseMap.get("id");
                name = (String) responseMap.get("name");
                nickname = (String) responseMap.get("nickname");
            }
        }

        if (socialId == null) {
            socialId = oAuth2User.getName();
            log.warn("[CustomAuthenticationSuccessHandler] socialId was null after specific parsing, falling back to oAuth2User.getName(): {}", socialId);
        }

        log.info("[CustomAuthenticationSuccessHandler] Authentication successful.");
        log.info("[CustomAuthenticationSuccessHandler] OAuth2User attributes received: {}", attributes);
        log.info("[CustomAuthenticationSuccessHandler] Derived socialType: {}, socialId: {}, name: {}, nickname: {}", socialType, socialId, name, nickname);

        if (socialId == null) {
            log.error("[CustomAuthenticationSuccessHandler] CRITICAL ERROR: socialId is null. Cannot proceed with user lookup.");
            throw new ServletException("소셜 ID를 찾을 수 없습니다. (비정상 상태)");
        }

        Optional<SocialUserEntity> socialUserOpt = socialUserRepository.findBySocialIdAndSocialType(socialId, socialType);

        UserEntity userEntity;
        boolean needsSignupCompletion;

        if (socialUserOpt.isPresent()) {
            // 1. 기존 SocialUserEntity가 존재하는 경우
            SocialUserEntity socialUser = socialUserOpt.get();
            userEntity = userRepository.findByUserId(socialUser.getUserId())
                    .orElseThrow(() -> {
                        log.error("[CustomAuthenticationSuccessHandler] CRITICAL ERROR: SocialUserEntity found, but UserEntity not found for userId: {}. Data inconsistency.", socialUser.getUserId());
                        return new ServletException("연결된 사용자 정보를 찾을 수 없습니다. (비정상 상태 - SocialUserEntity 존재)");
                    });
            // ✅ needsSignupCompletion 판단 기준 변경: phone 또는 birthday가 null이면 추가 정보 입력 필요
            needsSignupCompletion = userEntity.getPhone() == null || userEntity.getBirthday() == null;
            log.info("[CustomAuthenticationSuccessHandler] Existing social user found. userId={}, needsSignupCompletion={}", userEntity.getUserId(), needsSignupCompletion);

        } else {
            // 2. 신규 소셜 사용자인 경우 (SocialUserEntity가 존재하지 않음)
            log.info("[CustomAuthenticationSuccessHandler] New social user detected. Creating initial UserEntity and SocialUserEntity.");

            String newUserId = UUID.randomUUID().toString();
            userEntity = UserEntity.builder()
                    .userId(newUserId)
                    .loginId(null) // ✅ loginId는 null로 유지 (소셜 전용 계정)
                    .password(null) // ✅ password는 null로 유지 (소셜 전용 계정)
                    .name(name)
                    .nickname(nickname)
                    .phone(null) // ✅ 초기에는 phone null
                    .birthday(null) // ✅ 초기에는 birthday null
                    .role("USER")
                    .autoLoginFlag("N")
                    .build();
            userRepository.save(userEntity);
            userRepository.flush();

            SocialUserEntity newSocialUser = SocialUserEntity.builder()
                    .socialUserId(UUID.randomUUID().toString())
                    .userId(newUserId)
                    .socialId(socialId)
                    .socialType(socialType)
                    .build();
            socialUserRepository.save(newSocialUser);
            socialUserRepository.flush();

            needsSignupCompletion = true; // 신규 사용자이므로 추가 정보 입력 필요
            log.info("[CustomAuthenticationSuccessHandler] NEW USER CREATED: userId={}, socialId={}, socialType={}, needsSignupCompletion={}",
                    newUserId, socialId, socialType, needsSignupCompletion);
        }

        // 사용자 상태에 따른 리다이렉트
        if (needsSignupCompletion) {
            log.info("[CustomAuthenticationSuccessHandler] Redirecting to social signup page.");
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/social-signup")
                    .queryParam("userId", URLEncoder.encode(userEntity.getUserId(), StandardCharsets.UTF_8))
                    .queryParam("socialType", URLEncoder.encode(socialType, StandardCharsets.UTF_8))
                    .queryParam("socialId", URLEncoder.encode(socialId, StandardCharsets.UTF_8))
                    .queryParamIfPresent("name", Optional.ofNullable(userEntity.getName()).map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8)))
                    .queryParamIfPresent("nickname", Optional.ofNullable(userEntity.getNickname()).map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8)))
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

        // 기존 사용자 또는 회원가입 완료된 사용자 (JWT 발급)
        User userDto = userEntity.toDto(); // 최종 UserEntity를 DTO로 변환

        log.info("[CustomAuthenticationSuccessHandler] Final UserDto for appUserId: {}. LoginId={}, Password={}",
                userDto.getUserId(), userDto.getLoginId(), userDto.getPassword() != null ? "******" : "null");

        if("BAD".equals(userDto.getRole())){
            log.warn("[CustomAuthenticationSuccessHandler] User {} is a BAD user. Denying access.", userDto.getLoginId());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"error\":\"불량유저로 등록되었습니다. 관리자에게 문의 하세요\"}");
            return;
        }

        if("EXIT".equals(userDto.getRole())){
            log.warn("[CustomAuthenticationSuccessHandler] User {} is an EXIT user. Denying access.", userDto.getLoginId());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"error\":\"탈퇴유저로 등록되었습니다. 관리자에게 문의 하세요.\"}");
            return;
        }

        String accessToken = jwtUtil.generateToken(userDto, "access");
        String refreshToken = jwtUtil.generateToken(userDto, "refresh");

        tokenService.saveRefreshToken(new Token(userDto.getUserId(), refreshToken));
        log.info("[CustomAuthenticationSuccessHandler] JWT issued and refresh token saved for user: {}", userDto.getLoginId());

        String finalRedirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/oauth2/callback/success")
                .queryParam("accessToken", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                .queryParamIfPresent("userId", Optional.ofNullable(userDto.getUserId()).map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8)))
                .queryParamIfPresent("name", Optional.ofNullable(userDto.getName()).map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8)))
                .queryParamIfPresent("role", Optional.ofNullable(userDto.getRole()).map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8)))
                .queryParamIfPresent("autoLoginFlag", Optional.ofNullable(userDto.getAutoLoginFlag()).map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8)))
                .queryParamIfPresent("nickname", Optional.ofNullable(userDto.getNickname()).map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8)))
                .build()
                .toUriString();

        log.info("[CustomAuthenticationSuccessHandler] Redirecting to frontend success callback: {}", finalRedirectUrl);
        getRedirectStrategy().sendRedirect(request, response, finalRedirectUrl);
    }
}
