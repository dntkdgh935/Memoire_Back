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

    @Column(name = "TAGNAME", nullable = false)
    private String tagName;

    @Column(name = "SEARCHCOUNT", nullable = false, columnDefinition = "number default 0")
    private int searchCount;

    @Column(name = "LIKECOUNT", nullable = false, columnDefinition = "number default 0")
    private int likeCount;

    @Lob
    @Column(name = "TAGEMBEDDING")
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
