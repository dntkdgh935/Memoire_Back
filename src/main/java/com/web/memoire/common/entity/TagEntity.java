package com.web.memoire.common.entity;

import com.web.memoire.common.dto.Tag;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "TB_TAG")
@Entity
public class TagEntity {

    @Id
    @Column(name = "TAGID", nullable = false)
    private int tagid;

    @Column(name = "TAG_NAME", nullable = false)
    private String tagName;

    @Column(name = "SEARCH_COUNT", nullable = false, columnDefinition = "NUMBER DEFAULT 0")
    private int searchCount;

    @Column(name = "LIKE_COUNT", nullable = false, columnDefinition = "NUMBER DEFAULT 0")
    private int likeCount;

    @Lob
    @Column(name = "TAG_EMBEDDING", columnDefinition = "CLOB")
    private String tagEmbedding;

    public Tag toDto() {
        return Tag.builder()
                .tagid(tagid)
                .tagName(tagName)
                .searchCount(searchCount)
                .likeCount(likeCount)
                .tagEmbedding(tagEmbedding)
                .build();
    }

}
