package com.web.memoire.security.oauth2;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User extends DefaultOAuth2User {

    private final String userId; // 우리 서비스의 사용자 ID
    private final boolean isNewUser; // 신규 가입자인지 여부
    private final boolean needsAdditionalInfo; // 카카오처럼 추가 정보가 필요한지 여부
    private final String socialType; // 소셜 타입 (kakao, naver, google)
    private final String socialId; // 소셜 서비스 고유 ID

    /**
     * Constructs a {@code DefaultOAuth2User} using the provided parameters.
     *
     * @param authorities the authorities granted to the user
     * @param attributes the attributes about the user
     * @param nameAttributeKey the key used to access the user's &quot;name&quot; from {@link #getAttributes()}
     * @param userId our service's unique user ID
     * @param isNewUser true if this is a new user registering for the first time via social login
     * @param needsAdditionalInfo true if additional user info (name, phone, birthday) is required
     * @param socialType the type of social login (e.g., "kakao", "naver", "google")
     * @param socialId the unique ID provided by the social service
     */
    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey,
                            String userId, // 우리 서비스 userId
                            boolean isNewUser,
                            boolean needsAdditionalInfo,
                            String socialType,
                            String socialId) {
        super(authorities, attributes, nameAttributeKey);
        this.userId = userId;
        this.isNewUser = isNewUser;
        this.needsAdditionalInfo = needsAdditionalInfo;
        this.socialType = socialType;
        this.socialId = socialId;
    }

    // CustomOAuth2UserService에서 호출할 편의 생성자
    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey,
                            String userId) {
        this(authorities, attributes, nameAttributeKey, userId,
                (Boolean) attributes.getOrDefault("isNewUser", false),
                (Boolean) attributes.getOrDefault("needsAdditionalInfo", false),
                (String) attributes.get("socialType"),
                (String) attributes.get("socialId"));
    }


    @Override
    public String getName() {
        return this.userId;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isNewUser() {
        return isNewUser;
    }

    public boolean needsAdditionalInfo() {
        return needsAdditionalInfo;
    }

    public String getSocialType() {
        return socialType;
    }

    public String getSocialId() {
        return socialId;
    }
}