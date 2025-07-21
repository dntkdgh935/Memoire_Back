package com.web.memoire.common.dto;

import com.web.memoire.common.entity.TagEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
//@Component
public class Tag {

    @NotNull
    private int tagid;

    @NotBlank
    private String tagName;

    @NotNull
    private int searchCount;

    @NotNull
    private int likeCount;

    private String tagEmbedding;


    public TagEntity toEntity() {
        return TagEntity.builder()
                .tagid(tagid)
                .tagName(tagName)
                .searchCount(searchCount)
                .likeCount(likeCount)
                .tagEmbedding(tagEmbedding)
                .build();
    }

}
