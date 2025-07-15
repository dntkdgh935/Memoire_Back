package com.web.memoire.atelier.video.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoGenerationRequest {
    private String imageUrl;
    private String description;
    private String ctsStyle;
    private String tone;
    private String backgroundPrompt;
    private String memoryType;
    private Long collectionId;
    private String title;
    private String content;
    private String filename;
    private String filepath;
}
