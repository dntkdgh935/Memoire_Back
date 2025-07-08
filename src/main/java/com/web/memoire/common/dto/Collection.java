package com.web.memoire.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.memoire.common.entity.CollectionEntity;
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
public class Collection {

    @NotBlank
    private String collectionid;

    @NotBlank
    private String authorid;

    @NotBlank
    private String collectionTitle;

    @NotNull
    private int readCount;

    @NotNull
    private int visibility;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date createdDate;
    private String titleEmbedding;

    @NotBlank
    private String color;

    public CollectionEntity toEntity() {
        return CollectionEntity.builder()
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

}
