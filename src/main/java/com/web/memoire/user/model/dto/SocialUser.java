package com.web.memoire.user.model.dto;

import com.web.memoire.user.jpa.entity.SocialUserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SocialUser {
    private String socialUserId;
    private String userId;
    private String socialId;
    private String socialType;

     public SocialUserEntity toEntity() {
         return SocialUserEntity.builder()
             .socialUserId(this.socialUserId)
             .userId(this.userId)
             .socialId(this.socialId)
             .socialType(this.socialType)
             .build();
     }
}
