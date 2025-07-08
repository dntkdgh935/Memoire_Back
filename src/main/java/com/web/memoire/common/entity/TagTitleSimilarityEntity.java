package com.web.memoire.common.entity;

import com.web.memoire.common.dto.TagTitleSimilarity;
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
@Table(name = "TB_TAG_TITLE_SIMILARITY")
@Entity
@IdClass(TagTitleSimilarityId.class)
public class TagTitleSimilarityEntity {

    @Id
    @Column(name = "TAGID", nullable = false)
    private int tagid;

    @Id
    @Column(name = "COLLECTIONID", nullable = false)
    private String collectionid;

    @Column(name = "SIMILARITY_SCORE", nullable = false)
    private double similarityScore;

    @Column(name = "CALC_DATE", nullable = false, columnDefinition = "date default sysdate")
    private Date calcDate;

    public TagTitleSimilarity toDto() {
        return TagTitleSimilarity.builder()
                .tagid(tagid)
                .collectionid(collectionid)
                .similarityScore(similarityScore)
                .calcDate(calcDate)
                .build();
    }
}
