package com.web.memoire.atelier.ImTIm.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImTImGenerationRequest {
    @JsonAlias("prompt")
    private String stylePrompt;
    @JsonAlias("image_url")
    private String imageUrl;
}
