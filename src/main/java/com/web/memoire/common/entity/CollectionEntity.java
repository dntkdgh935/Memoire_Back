package com.web.memoire.common.entity;

import com.web.memoire.common.dto.Collection;
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
@Table(name = "TB_COLLECTION")
@Entity
public class CollectionEntity {

    @Id
    @Column(name = "COLLECTIONID", length = 50, nullable = false)
    private String collectionid;

    @Column(name = "AUTHORID", nullable = false)
    private String authorid;

    @Column(name = "COLLECTION_TITLE", nullable = false)
    private String collectionTitle;

    @Column(name = "READ_COUNT", nullable = false, columnDefinition = "number default 0")
    private int readCount;

    @Column(name = "VISIBILITY", nullable = false, columnDefinition = "number default 1")
    private int visibility;

    @Column(name = "CREATED_DATE", nullable = false, columnDefinition = "date default sysdate")
    private Date createdDate;

    @Lob
    @Column(name = "TITLE_EMBEDDING")
    private String titleEmbedding;

    @Column(name = "COLOR", length = 50, nullable = false)
    private String color;

    public Collection toDto() {
        return Collection.builder()
                .collectionid(collectionid)
                .authorid(authorid)
                .collectionTitle(collectionTitle)
                .readCount(readCount)
                .visibility(visibility)
                .createdDate(createdDate)
                .titleEmbedding(titleEmbedding)
                .color(color)
                .build();
    }

    public String getId() {
        return collectionid;
    }
}
