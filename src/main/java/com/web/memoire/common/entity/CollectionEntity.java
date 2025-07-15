package com.web.memoire.common.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "collection_seq")
    @SequenceGenerator(name = "collection_seq", sequenceName = "SEQ_TB_COLLECTION_COLLECTIONID", allocationSize = 1)
    @Column(name = "COLLECTIONID", nullable = false)
    private int collectionid;

    @Column(name = "AUTHORID", nullable = false)
    private String authorid;

    @Column(name = "COLLECTION_TITLE", nullable = false)
    private String collectionTitle;

    @Column(name = "READ_COUNT", nullable = false, columnDefinition = "number default 0")
    private int readCount;

    @Column(name = "VISIBILITY", nullable = false, columnDefinition = "number default 1")
    private int visibility;

    @JsonFormat(pattern = "yyyy.MM.dd")
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

    public int getId() {
        return collectionid;
    }
}
