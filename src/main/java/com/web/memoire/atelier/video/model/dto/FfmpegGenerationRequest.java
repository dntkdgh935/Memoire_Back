package com.web.memoire.atelier.video.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FfmpegGenerationRequest {
    private String videoUrl;
    private String ttsUrl;
}
