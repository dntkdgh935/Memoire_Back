package com.web.memoire.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.memoire.common.entity.TagTitleSimilarityEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
public class TagTitleSimilarity {

    @NotNull
    private int tagid;

    @NotBlank
    private String collectionid;

    @NotNull
    private double similarityScore;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date calcDate;

    public TagTitleSimilarityEntity toEntity() {
        return TagTitleSimilarityEntity.builder()
                .tagid(tagid)
                .collectionid(collectionid)
                .similarityScore(similarityScore)
                .calcDate(calcDate)
                .build();
    }
}
