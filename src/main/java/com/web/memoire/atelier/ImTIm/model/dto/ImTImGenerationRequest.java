package com.web.memoire.atelier.ImTIm.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImTImGenerationRequest {
    private String imageUrl;
    private String stylePrompt;
    private String userId;
    private String title;
    private String content;
    private String filename;
    private String filepath;
}
