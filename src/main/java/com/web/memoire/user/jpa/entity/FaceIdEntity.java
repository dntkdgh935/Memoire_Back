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

    // face_path 컬럼은 데이터베이스 스키마에서 제거되었으므로 엔티티에서도 제거합니다.
    // @Column(name="FACE_PATH", length = 100)
    // private String facePath;

    @Column(name="DESCRIPTION", length = 100) // 기존 필드 (필요하다면 유지)
    private String description;

    // 얼굴 임베딩 필드 (데이터베이스의 CLOB에 매핑됩니다)
    @Column(name = "FACE_EMBEDDING") // CLOB 타입은 일반적으로 length를 지정하지 않습니다.
    private String faceEmbedding; // List<Float>를 JSON 문자열로 저장

    public FaceId toDto(){
        return FaceId.builder()
                .faceId(this.faceId)
                .userId(this.userId)
                // .facePath(this.facePath) // DTO에서도 facePath 제거
                .description(this.description)
                .faceEmbedding(this.faceEmbedding) // DTO에도 faceEmbedding 유지
                .build();
    }
}
