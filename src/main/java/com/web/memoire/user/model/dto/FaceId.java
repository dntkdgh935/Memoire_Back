package com.web.memoire.user.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FaceId {
    private String faceId;
    private String userId;
    private String description;
    private String faceEmbedding; // 얼굴 임베딩 필드 유지
}
