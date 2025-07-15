package com.web.memoire.atelier.ImTIm.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImTImResultDto {
    private String imageUrl;
    private String filename;
    private String filepath;
    private String title;
    private String content;
    private Long collectionId;

}