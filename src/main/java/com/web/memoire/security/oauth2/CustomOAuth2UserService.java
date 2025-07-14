package com.web.memoire.security.oauth2;

import com.web.memoire.user.jpa.entity.SocialUserEntity;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.SocialUserRepository;
import com.web.memoire.user.jpa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

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

        log.info("Social Login Type: {}", registrationId);
        log.info("OAuth2User attributes: {}", oAuth2User.getAttributes());

        String socialId;
        String nickname = null;
        String name = null;
        String mobile = null;
        String birthyear = null;
        String birthdayStr = null;
        Date birthdayDate = null;

        if ("google".equals(registrationId)) {
            socialId = oAuth2User.getName(); // Google은 'sub'가 기본 고유 ID
            name = (String) oAuth2User.getAttributes().get("name");
        } else if ("naver".equals(registrationId)) {
            Map<String, Object> responseAttributes = (Map<String, Object>) oAuth2User.getAttributes().get("response");
            if (responseAttributes == null) {
                log.error("Naver OAuth2User does not contain 'response' attribute.");
                throw new OAuth2AuthenticationException("Naver response attributes missing.");
            }
            socialId = (String) responseAttributes.get("id");
            name = (String) responseAttributes.get("name");
            nickname = (String) responseAttributes.get("nickname");
            mobile = (String) responseAttributes.get("mobile");
            birthyear = (String) responseAttributes.get("birthyear");
            birthdayStr = (String) responseAttributes.get("birthday");

            if (birthyear != null && birthdayStr != null) {
                String fullBirthday = null;
                try {
                    fullBirthday = birthyear + "-" + birthdayStr;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    birthdayDate = sdf.parse(fullBirthday);
                } catch (ParseException e) {
                    log.warn("Failed to parse Naver birthday: {}", fullBirthday, e);
                    birthdayDate = null;
                }
            }
        } else if ("kakao".equals(registrationId)) {
            socialId = String.valueOf(oAuth2User.getAttributes().get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
            if (kakaoAccount != null) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    nickname = (String) profile.get("nickname");
                }
                Boolean hasBirthday = (Boolean) kakaoAccount.get("has_birthday");
                if (hasBirthday != null && hasBirthday) {
                    birthdayStr = (String) kakaoAccount.get("birthday");
                    if (birthdayStr != null && birthdayStr.length() == 4) {
                        try {
                            String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
                            String fullBirthday = currentYear + birthdayStr;
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                            birthdayDate = sdf.parse(fullBirthday);
                        } catch (ParseException e) {
                            log.warn("Failed to parse Kakao birthday: {}", birthdayStr, e);
                            birthdayDate = null;
                        }
                    }
                }
                Boolean hasPhoneNumber = (Boolean) kakaoAccount.get("has_phone_number");
                if (hasPhoneNumber != null && hasPhoneNumber) {
                    mobile = (String) kakaoAccount.get("phone_number");
                    if (mobile != null && mobile.startsWith("+82 ")) {
                        mobile = "0" + mobile.substring(4).replace("-", "");
                    }
                }
                if (name == null && nickname != null) {
                    name = nickname;
                }
            }
        } else {
            throw new OAuth2AuthenticationException("Unsupported social login type: " + registrationId);
        }

        Optional<SocialUserEntity> existingSocialUserOpt = socialUserRepository.findBySocialIdAndSocialType(socialId, registrationId);

        UserEntity userEntity;
        boolean isNewUser = false;
        boolean needsAdditionalInfo = false;

        if (existingSocialUserOpt.isPresent()) {
            SocialUserEntity existingSocialUser = existingSocialUserOpt.get();
            userEntity = userRepository.findByUserId(existingSocialUser.getUserId())
                    .orElseThrow(() -> new OAuth2AuthenticationException("연결된 사용자 정보를 찾을 수 없습니다. (비정상 상태)"));
            log.info("Existing social user logged in: userId={}, socialType={}", userEntity.getUserId(), registrationId);
        } else {
            isNewUser = true;
            String newUserId = UUID.randomUUID().toString();

            if ("kakao".equals(registrationId)) {
                needsAdditionalInfo = true;
                UserEntity newUser = UserEntity.builder()
                        .userId(newUserId)
                        .loginId(registrationId + "_" + socialId)
                        .password("NO_PASSWORD_SOCIAL_LOGIN")
                        .name(nickname != null ? nickname : null)
                        .nickname(nickname)
                        .role("USER")
                        .autoLoginFlag("N")
                        .build();
                userEntity = userRepository.save(newUser);
                log.info("New Kakao social user created (needs additional info): userId={}", userEntity.getUserId());

            } else { // Google, Naver
                String finalName = (name != null && !name.isEmpty()) ? name : (nickname != null && !nickname.isEmpty() ? nickname : null);

                UserEntity newUser = UserEntity.builder()
                        .userId(newUserId)
                        .loginId(registrationId + "_" + socialId)
                        .password("NO_PASSWORD_SOCIAL_LOGIN")
                        .name(finalName)
                        .nickname(nickname)
                        .phone(mobile)
                        .birthday(birthdayDate)
                        .role("USER")
                        .autoLoginFlag("N")
                        .build();
                userEntity = userRepository.save(newUser);
                log.info("New {} social user created: userId={}", registrationId, userEntity.getUserId());

                if (finalName == null || mobile == null || birthdayDate == null) {
                    needsAdditionalInfo = true;
                }
            }

            SocialUserEntity newSocialUser = SocialUserEntity.builder()
                    .socialUserId(UUID.randomUUID().toString())
                    .userId(userEntity.getUserId())
                    .socialId(socialId)
                    .socialType(registrationId)
                    .build();
            socialUserRepository.save(newSocialUser);
        }

        Map<String, Object> customAttributes = new HashMap<>(oAuth2User.getAttributes());
        customAttributes.put("userId", userEntity.getUserId());
        customAttributes.put("isNewUser", isNewUser);
        customAttributes.put("needsAdditionalInfo", needsAdditionalInfo);
        customAttributes.put("socialType", registrationId);
        customAttributes.put("socialId", socialId);
        customAttributes.put("loginId", userEntity.getLoginId());
        customAttributes.put("name", userEntity.getName());
        customAttributes.put("nickname", userEntity.getNickname());

        // 🚨 최종 수정:
        // DefaultOAuth2User의 생성자에 전달되는 nameAttributeKey는
        // attributes 맵에서 "이름"으로 사용될 속성의 '키(Key)'입니다.
        // 예를 들어 Google의 'sub', Naver/Kakao의 'id' 같은 것이죠.
        // 우리는 이미 CustomOAuth2User에서 getName()을 userId로 오버라이드 했으므로
        // DefaultOAuth2User가 어떤 키를 이름으로 사용하려 해도 최종 결과에는 영향이 없습니다.
        // 따라서, NullPointerException을 피하고 Spring Security의 기대치를 만족시키기 위해
        // 단순히 소셜 ID가 담긴 속성의 키를 전달하거나, 안전한 고정 문자열을 사용합니다.
        String nameAttributeKeyToPass = null;
        if ("google".equals(registrationId)) {
            nameAttributeKeyToPass = "sub"; // Google의 기본 user-name-attribute
        } else if ("naver".equals(registrationId)) {
            nameAttributeKeyToPass = "id"; // Naver의 기본 user-name-attribute (response 객체 내부의 id)
        } else if ("kakao".equals(registrationId)) {
            nameAttributeKeyToPass = "id"; // Kakao의 기본 user-name-attribute
        } else {
            // 다른 소셜 타입이 있다면 그에 맞는 키를 설정하거나,
            // 기본적으로 "id"를 사용합니다.
            nameAttributeKeyToPass = "id";
        }

        // 또한, DefaultOAuth2UserService가 oAuth2User 객체를 생성할 때
        // 내부적으로 userNameAttributeName을 파싱하여 oAuth2User.getName()을 구성하므로,
        // oAuth2User.getAttributes().keySet()에서 해당 키가 존재하는지 확인하여
        // 동적으로 설정하는 것도 방법입니다.
        // 하지만 고정된 키를 사용하는 것이 가장 안정적입니다.

        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(userEntity.getRole())),
                customAttributes,
                nameAttributeKeyToPass, // 🚨 고정된 키 전달
                userEntity.getUserId(),
                isNewUser,
                needsAdditionalInfo,
                registrationId,
                socialId
        );
    }
}