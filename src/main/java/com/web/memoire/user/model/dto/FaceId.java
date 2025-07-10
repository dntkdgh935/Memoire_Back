package com.web.memoire.user.model.dto;

import com.web.memoire.user.jpa.entity.FaceIdEntity;
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
    private String facePath;
    private String description;

     public FaceIdEntity toEntity() {
         return FaceIdEntity.builder()
             .faceId(this.faceId)
             .userId(this.userId)
             .facePath(this.facePath)
             .description(this.description)
             .build();
     }
}
