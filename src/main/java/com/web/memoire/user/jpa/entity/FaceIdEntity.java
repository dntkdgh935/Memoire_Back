package com.web.memoire.user.jpa.entity;

import com.web.memoire.user.model.dto.FaceId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "TB_FACEID")
@Entity
@Builder
public class FaceIdEntity {
    @Id
    @Column(name="FACEID", length = 50)
    private String faceId;

    @Column(name="USERID", nullable = false, length = 50)
    private String userId;

    @Column(name="FACE_PATH", length = 100)
    private String facePath;

    @Column(name="DESCRIPTION", length = 100)
    private String description;

    public FaceId toEntity(){
        return FaceId.builder()
                .faceId(this.faceId)
                .userId(this.userId)
                .facePath(this.facePath)
                .description(this.description)
                .build();
    }
}
