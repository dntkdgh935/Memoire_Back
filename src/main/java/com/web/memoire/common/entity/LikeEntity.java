package com.web.memoire.common.entity;

import com.web.memoire.common.dto.Like;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="TB_COLL_LIKE")
@Entity
@IdClass(LikeId.class)
public class LikeEntity {

    @Id
    @Column(name="COLLECTIONID", nullable = false)
    private int collectionid;
    @Id
    @Column(name="USERID", nullable = false)
    private String userid;
    @Column(name="LIKED_DATE", nullable = false, columnDefinition = "date default sysdate")
    private Date likedDate;

    public Like toDto() {
        return Like.builder()
                .collectionid(collectionid)
                .userid(userid)
                .likedDate(likedDate)
                .build();
    }
}
