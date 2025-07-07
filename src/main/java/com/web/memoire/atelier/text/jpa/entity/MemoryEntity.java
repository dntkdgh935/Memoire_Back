package com.web.memoire.atelier.text.jpa.entity;

import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class MemoryEntity {
    private Long id;
    private String type;        // text, image
    private String title;
    private String content;     // 텍스트 or 이미지 URL
    private Long collectionId;
    private String userId;
    private LocalDateTime createdAt;
}
