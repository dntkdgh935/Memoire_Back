package com.web.memoire.common.entity;

import com.web.memoire.common.dto.Bookmark;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="TB_COLL_BOOKMARK")
@Entity
@IdClass(BookmarkId.class)
public class BookmarkEntity {

    @Id
    @Column(name="USERID", nullable = false)
    private String userid;

    @Id
    @Column(name="COLLECTIONID", nullable = false)
    private String collectionid;

    public Bookmark toDto() {
        return Bookmark.builder()
                .userid(userid)
                .collectionid(collectionid)
                .build();
    }
}
