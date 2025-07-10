package com.web.memoire.user.jpa.entity;

import com.web.memoire.user.model.dto.SocialUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "TB_SocialUser", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"SOCIALID", "SOCIAL_TYPE"})
})
@Entity
public class SocialUserEntity {
    @Id
    @Column(name="SOCIALUSERID", length = 50)
    private String socialUserId;

    @Column(name="USERID", nullable = false, unique = true, length = 50)
    private String userId;

    @Column(name="SOCIALID", nullable = false, length = 50)
    private String socialId;

    @Column(name="SOCIAL_TYPE", length = 10)
    private String socialType;

    public SocialUser toEntity() {
        return SocialUser.builder()
                .socialUserId(this.socialUserId)
                .userId(this.userId)
                .socialId(this.socialId)
                .socialType(this.socialType)
                .build();
    }

}
