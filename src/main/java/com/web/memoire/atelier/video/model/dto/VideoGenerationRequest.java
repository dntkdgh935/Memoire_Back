package com.web.memoire.atelier.video.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoGenerationRequest {
    private String imageUrl;
    private String videoPrompt;
    private String ttsUrl;
    private Boolean lipSyncEnabled;
}
