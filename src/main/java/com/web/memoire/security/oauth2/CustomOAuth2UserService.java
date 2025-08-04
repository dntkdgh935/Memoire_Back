package com.web.memoire.security.oauth2;

import com.web.memoire.user.jpa.entity.SocialUserEntity;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.SocialUserRepository;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.User; // User DTO는 직접 사용되지 않아 제거될 수 있지만, 현재는 그대로 둡니다.
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final SocialUserRepository socialUserRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        log.info("[CustomOAuth2UserService] Social Login Type: {}, userNameAttributeName: {}", registrationId, userNameAttributeName);
        log.info("[CustomOAuth2UserService] OAuth2User attributes: {}", oAuth2User.getAttributes());

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());

        String socialId = null; // 초기화
        String name = null;
        String nickname = null;

        // 소셜 타입별로 사용자 정보 및 socialId 추출 로직 강화
        if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            if (response != null) {
                socialId = (String) response.get("id"); // 네이버의 고유 ID
                name = (String) response.get("name");
                nickname = (String) response.get("nickname");
            }
        } else if ("kakao".equals(registrationId)) {
            socialId = String.valueOf(attributes.get("id")); // 카카오의 고유 ID (최상위 'id')
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount != null) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    nickname = (String) profile.get("nickname");
                }
            }
            name = nickname; // 카카오는 'name' 필드가 없으므로 'nickname'으로 대체
        } else if ("google".equals(registrationId)) {
            socialId = (String) attributes.get("sub"); // 구글의 고유 ID ('sub' 클레임)
            name = (String) attributes.get("name");
            nickname = (String) attributes.get("given_name");
        }

        // socialId가 여전히 null이면 oAuth2User.getName()을 사용 (최후의 수단)
        if (socialId == null) {
            socialId = oAuth2User.getName();
            log.warn("[CustomOAuth2UserService] socialId was null after specific parsing, falling back to oAuth2User.getName(): {}", socialId);
        }

        // 사용자 저장 또는 업데이트
        UserEntity userEntity = saveOrUpdate(socialId, registrationId, name, nickname);

        // OAuth2User attributes에 우리 서비스의 추가 정보 포함
        attributes.put("appUserId", userEntity.getUserId());
        // UserEntity에 password 필드가 없으므로, loginId가 null인 경우 회원가입 완료가 필요하다고 판단
        boolean needsSignupCompletion = userEntity.getLoginId() == null;
        attributes.put("needsSignupCompletion", needsSignupCompletion);
        attributes.put("socialType", registrationId);
        attributes.put("socialId", socialId); // 추출된 socialId를 다시 attributes에 넣어 CustomAuthenticationSuccessHandler로 전달
        attributes.put("name", userEntity.getName());
        attributes.put("nickname", userEntity.getNickname());

        log.info("[CustomOAuth2UserService] Returning OAuth2User with appUserId: {}, needsSignupCompletion: {}", userEntity.getUserId(), needsSignupCompletion);
        log.info("[CustomOAuth2UserService] UserEntity details: loginId={}", userEntity.getLoginId());


        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(userEntity.getRole())),
                attributes,
                userNameAttributeName);
    }

    private UserEntity saveOrUpdate(String socialId, String socialType, String name, String nickname) {
        Optional<SocialUserEntity> socialUserOpt = socialUserRepository.findBySocialIdAndSocialType(socialId, socialType);

        if (socialUserOpt.isPresent()) {
            SocialUserEntity socialUser = socialUserOpt.get();
            UserEntity user = userRepository.findByUserId(socialUser.getUserId())
                    .orElseThrow(() -> {
                        log.error("[CustomOAuth2UserService] Error: SocialUserEntity exists but corresponding UserEntity not found for userId: {}", socialUser.getUserId());
                        return new OAuth2AuthenticationException("User not found for existing social account.");
                    });
            log.info("[CustomOAuth2UserService] Existing social user found: userId={}, socialType={}, socialId={}, loginId={}",
                    user.getUserId(), socialType, socialId, user.getLoginId()); // password 관련 로깅 제거
            return user;
        } else {
            log.info("[CustomOAuth2UserService] New social user. Creating initial account for socialId: {}", socialId);
            String newUserId = UUID.randomUUID().toString();

            UserEntity newUser = UserEntity.builder()
                    .userId(newUserId)
                    .loginId(null)
                    .name(name)
                    .nickname(nickname)
                    .role("USER")
                    .autoLoginFlag("N")
                    .loginType("social")
                    .registrationDate(new Date())
                    .sanctionCount(0)
                    .statusMessage(null)
                    .build();
            userRepository.save(newUser);
            userRepository.flush(); // 변경 사항을 즉시 DB에 반영

            SocialUserEntity newSocialUser = SocialUserEntity.builder()
                    .socialUserId(UUID.randomUUID().toString())
                    .userId(newUserId)
                    .socialId(socialId)
                    .socialType(socialType)
                    .build();
            socialUserRepository.save(newSocialUser);
            socialUserRepository.flush(); // 변경 사항을 즉시 DB에 반영

            log.info("[CustomOAuth2UserService] NEW USER CREATED (initial save): userId={}, socialId={}, socialType={}",
                    newUserId, socialId, socialType);
            log.info("[CustomOAuth2UserService] New UserEntity details: loginId={}", newUser.getLoginId());
            return newUser;
        }
    }
}
