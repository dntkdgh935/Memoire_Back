package com.web.memoire.user.jpa.repository;

import com.web.memoire.user.jpa.entity.FaceIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FaceIdRepository extends JpaRepository<FaceIdEntity, String> {
    // 특정 사용자의 얼굴 ID 엔티티를 조회하는 메서드
    // 한 사용자당 하나의 얼굴 임베딩만 등록한다고 가정할 경우, List 대신 Optional<FaceIdEntity>를 반환하도록 할 수 있습니다.
    // 현재 UserService의 saveUserFaceEmbedding에서 stream().findFirst()를 사용하므로 List로 유지해도 무방합니다.
    List<FaceIdEntity> findByUserId(String userId);

    // 얼굴 임베딩이 null이 아닌 모든 FaceId 엔티티를 조회하는 메서드 (얼굴 인증 시 사용)
    List<FaceIdEntity> findByFaceEmbeddingIsNotNull();

    // 특정 사용자 ID에 대해 얼굴 임베딩이 존재하는지 확인 (선택 사항, 필요에 따라 추가)
    Optional<FaceIdEntity> findByUserIdAndFaceEmbeddingIsNotNull(String userId);
}
