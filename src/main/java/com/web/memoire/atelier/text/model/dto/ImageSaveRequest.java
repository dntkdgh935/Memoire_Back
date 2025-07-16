package com.web.memoire.atelier.text.model.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageSaveRequest {
    private Integer originalMemoryId;
    private String title;
    private String imageUrl;
    private String prompt;
    private String style;
    private String memoryType; // 항상 "image"
    private int collectionId;
    private int memoryOrder;
}