package com.web.memoire.common.entity;

import com.web.memoire.common.dto.CollectionTag;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "TB_COLL_BOOKMARK")
@Entity
@IdClass(CollectionTagId.class)
public class CollectionTagEntity {
    @Id
    @Column(name = "COLLECTIONID", nullable = false)
    private int collectionid;
    @Id
    @Column(name = "TAGID", nullable = false)
    private int tagid;

    public CollectionTag toDto() {
        return CollectionTag.builder()
                .collectionid(collectionid)
                .tagid(tagid)
                .build();
    }
}
