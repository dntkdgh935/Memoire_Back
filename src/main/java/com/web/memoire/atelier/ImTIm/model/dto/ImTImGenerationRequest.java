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
    private String extraPrompt;
    private String title;
}
