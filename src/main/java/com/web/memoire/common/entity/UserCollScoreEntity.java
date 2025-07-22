package com.web.memoire.common.entity;

import com.web.memoire.common.dto.UserCollScore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="TB_USER_COLL_SCORES")
@Entity
@DynamicInsert //엔티티 저장시 null이나 기본값 필드를 insert하지 않도록?
@IdClass(UserCollScoreId.class)
public class UserCollScoreEntity {
    @Id
    @Column(name="USERID", nullable = false)
    private String userid;

    @Id
    @Column(name="COLLECTIONID", nullable = false)
    private int collectionid;

    @Column(name = "SCORE", columnDefinition = "FLOAT DEFAULT 10") // default 10
    private float score;

    @Column(name = "SEEN", columnDefinition = "NUMBER DEFAULT 0") // default 0
    private int seen;

    @Column(name = "INTERACTED", columnDefinition = "NUMBER DEFAULT 0") // default 0
    private int interacted;

    @Column(name = "RECOMMENDED_AT", columnDefinition = "DATE DEFAULT SYSDATE") // default sysdate
    private Date recAt;

    public UserCollScore toDto() {
        return UserCollScore.builder()
                .userid(userid)
                .collectionid(collectionid)
                .score(score)
                .seen(seen)
                .interacted(interacted)
                .recAt(recAt)
                .build();
    }
}
