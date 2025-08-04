package com.web.memoire.user.model.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.memoire.user.jpa.entity.FaceIdEntity;
import com.web.memoire.user.jpa.entity.PwdEntity;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.FaceIdRepository;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.Pwd;
import com.web.memoire.user.model.dto.User;
import com.web.memoire.user.model.service.PwdService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final PwdService pwdService;

    @Autowired
    private final BCryptPasswordEncoder bcryptPasswordEncoder;

    private final FaceIdRepository faceIdRepository; // FaceIdEntity를 위한 Repository

    private final FaceRecognitionService faceRecognitionService; // FastAPI 호출을 위한 서비스

    private final ObjectMapper objectMapper; // JSON 직렬화/역직렬화를 위한 객체



    public boolean selectCheckId(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    public User selectUser(String loginId){
        return userRepository.findByLoginId(loginId)
                .map(UserEntity::toDto)
                .orElseThrow(() -> {
                    log.warn("사용자 조회 실패: loginId '{}' 에 해당하는 사용자를 찾을 수 없습니다.", loginId);
                    return new NoSuchElementException("사용자를 찾을 수 없습니다.");
                });
    }

    public User findUserByUserId(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("userId " + userId + "에 해당하는 사용자를 찾을 수 없습니다."));
        return userEntity.toDto(); // UserEntity를 User DTO로 변환하여 반환
    }

    @Transactional
    public void insertUser(User user){
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            String newUserId = UUID.randomUUID().toString();
            user.setUserId(newUserId);
            log.info("새로운 userId (UUID) 생성 및 할당: " + newUserId);
        }
        userRepository.save(user.toEntity());
    }

    @Transactional
    public void updateUserAutoLoginFlag(String userId, String autoLoginFlag) {
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("자동 로그인 설정 변경 대상 사용자를 찾을 수 없습니다: " + userId));

        if (userEntity.getAutoLoginFlag() == null || !userEntity.getAutoLoginFlag().equals(autoLoginFlag)) {
            userEntity.setAutoLoginFlag(autoLoginFlag);
            userRepository.save(userEntity);
            log.info("사용자 {} (userId: {})의 autoLoginFlag가 {}로 업데이트되었습니다.", userEntity.getLoginId(), userId, autoLoginFlag);
        } else {
            log.debug("사용자 {} (userId: {})의 autoLoginFlag가 이미 {}이므로 업데이트하지 않습니다.", userEntity.getLoginId(), userId, autoLoginFlag);
        }
    }

    public String findLoginIdByNameAndPhone(String name, String phone) {
        UserEntity userEntity = userRepository.findByNameAndPhoneAndLoginIdIsNotNull(name, phone)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + name));
        return userEntity.getLoginId();
    }

    @Transactional
    public void updateUserPassword(@NotNull String userId, @NotNull String prevRawPassword, @NotNull String newRawPassword) {
        // 1. 사용자 존재 여부 확인 (UserEntity의 다른 정보 업데이트는 없지만, 사용자 존재 확인은 필요할 수 있음)
        userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("비밀번호 변경 대상 사용자를 찾을 수 없습니다: " + userId));

        // 2. 실제 비밀번호 변경 로직은 PwdService에 위임
        // PwdService가 이전 비밀번호 확인, 새 비밀번호 암호화, 이력 저장, 과거 비밀번호 재사용 방지 등을 모두 처리합니다.
        pwdService.changeUserPassword(userId, prevRawPassword, newRawPassword);

        log.info("UserService에서 비밀번호 변경 요청 처리 완료: userId={}", userId);
    }

    @Transactional
    public UserEntity updateMyInfo(
            @NotNull String userId,
            String nickname,
            String phone,
            String birthday, // YYYY-MM-DD 형식의 문자열
            String profileImagePath,
            String statusMessage
    ) throws ParseException {
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다: " + userId));

        // 각 필드가 null이 아니면 업데이트
        if (nickname != null) {
            userEntity.setNickname(nickname);
        }
        if (phone != null) {
            userEntity.setPhone(phone);
        }
        if (birthday != null && !birthday.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            userEntity.setBirthday(sdf.parse(birthday));
        }
        if (profileImagePath != null) {
            userEntity.setProfileImagePath(profileImagePath);
        }
        if (statusMessage != null) {
            userEntity.setStatusMessage(statusMessage);
        }

        return userRepository.save(userEntity);
    }

    public User findUserByLoginIdAndPhone(@NotNull String loginId, String phone) {
        Optional<UserEntity> userEntityOptional = userRepository.findByLoginIdAndPhone(loginId, phone);

        if (userEntityOptional.isPresent()) {
            return userEntityOptional.get().toDto();
        } else {
            log.warn("findUserByLoginIdAndPhone: loginId '{}', phone '{}' 에 해당하는 사용자를 찾을 수 없습니다.", loginId, phone);
            return null;
        }
    }
    /**
     * 사용자 얼굴 임베딩을 저장합니다.
     * 기존 FaceIdEntity가 있다면 업데이트하고, 없다면 새로 생성합니다.
     * @param userId 사용자 ID
     * @param imageData 웹캠에서 캡처한 이미지 바이트 배열
     * @return 저장 성공 여부
     * @throws IOException 이미지 처리 또는 FastAPI 통신 중 오류 발생 시
     */
    @Transactional
    public boolean saveUserFaceEmbedding(String userId, byte[] imageData) throws IOException {
        // 사용자 존재 여부 확인 (선택 사항, 그러나 안전을 위해)
        userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다."));

        // 1. FastAPI를 통해 이미지에서 임베딩 추출
        List<Float> embedding = faceRecognitionService.getFaceEmbedding(imageData);
        log.info("saveUserFaceEmbedding: FastAPI에서 추출된 임베딩 (첫 5개): {}", embedding.subList(0, Math.min(embedding.size(), 5)));
        log.info("saveUserFaceEmbedding: FastAPI에서 추출된 임베딩 (마지막 5개): {}", embedding.subList(Math.max(0, embedding.size() - 5), embedding.size()));
        log.info("saveUserFaceEmbedding: 추출된 임베딩 길이: {}", embedding.size());

        if (embedding.isEmpty()) {
            // 얼굴을 찾지 못했거나 임베딩 추출에 실패한 경우
            throw new IllegalArgumentException("이미지에서 얼굴 임베딩을 추출할 수 없습니다. 얼굴이 명확한지 확인해주세요.");
        }

        // 2. 추출된 임베딩을 JSON 문자열로 변환
        String embeddingJson;
        try {
            embeddingJson = objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException e) {
            throw new IOException("얼굴 임베딩을 JSON으로 변환하는 중 오류 발생", e);
        }

        // 3. FaceIdEntity를 찾아 업데이트하거나 새로 생성하여 저장
        // 한 사용자당 하나의 얼굴 임베딩만 등록한다고 가정하고, loginId로 조회
        Optional<FaceIdEntity> existingFaceId = faceIdRepository.findByUserId(userId).stream().findFirst();

        FaceIdEntity faceIdEntity;
        if (existingFaceId.isPresent()) {
            faceIdEntity = existingFaceId.get();
            faceIdEntity.setFaceEmbedding(embeddingJson);
            // facePath, description 등 다른 필드도 필요에 따라 업데이트
            // (facePath는 DB에서 제거되었으므로 여기서는 업데이트하지 않습니다.)
        } else {
            faceIdEntity = FaceIdEntity.builder()
                    .faceId(UUID.randomUUID().toString())
                    .userId(userId)
                    .faceEmbedding(embeddingJson)
                    .description("User face embedding")
                    .build();
        }
        faceIdRepository.save(faceIdEntity);
        return true;
    }

    /**
     * 얼굴 임베딩을 사용하여 사용자를 인증합니다.
     * @param currentFaceImageData 현재 웹캠에서 캡처한 얼굴 이미지 바이트 배열
     * @return 인증된 사용자 ID (없으면 null)
     * @throws IOException 이미지 처리 또는 FastAPI 통신 중 오류 발생 시
     */
    public String authenticateUserByFace(String loginId, byte[] currentFaceImageData) throws IOException { // ✅ loginId 매개변수 추가
        // 1. 현재 이미지에서 임베딩 추출
        List<Float> currentEmbedding = faceRecognitionService.getFaceEmbedding(currentFaceImageData);

        if (currentEmbedding.isEmpty()) {
            log.warn("authenticateUserByFace: 현재 이미지에서 얼굴 임베딩을 추출할 수 없습니다.");
            return null;
        }

        // ✅ 2. 입력받은 loginId로 UserEntity를 찾아 userId를 얻습니다.
        UserEntity userEntity = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> {
                    log.warn("authenticateUserByFace: 로그인 ID '{}' 에 해당하는 사용자를 찾을 수 없습니다.", loginId);
                    // 특정 로그인 ID에 사용자가 없으면 바로 인증 실패 처리
                    return new NoSuchElementException("입력한 로그인 ID에 해당하는 사용자를 찾을 수 없습니다.");
                });
        String userIdToAuthenticate = userEntity.getUserId(); // 해당 로그인 ID의 userId

        // ✅ 3. 얻은 userId로 해당 사용자의 얼굴 임베딩만 가져옵니다.
        Optional<FaceIdEntity> userFaceIdOptional = faceIdRepository.findByUserId(userIdToAuthenticate).stream().findFirst();

        if (userFaceIdOptional.isEmpty()) {
            log.warn("authenticateUserByFace: 사용자 ID '{}' (로그인 ID: '{}') 에 등록된 얼굴 임베딩 데이터가 없습니다.", userIdToAuthenticate, loginId);
            return null;
        }

        FaceIdEntity userFaceIdEntity = userFaceIdOptional.get();
        String storedEmbeddingJson = userFaceIdEntity.getFaceEmbedding();

        if (storedEmbeddingJson == null || storedEmbeddingJson.isEmpty()) {
            log.warn("authenticateUserByFace: 사용자 ID '{}' 의 임베딩 데이터가 비어 있습니다.", userIdToAuthenticate);
            return null;
        }

        List<Float> storedEmbedding;
        try {
            storedEmbedding = objectMapper.readValue(storedEmbeddingJson, new TypeReference<List<Float>>() {});
        } catch (JsonProcessingException e) {
            log.error("저장된 임베딩 JSON 파싱 오류 (userId '{}', loginId '{}'): {}", userIdToAuthenticate, loginId, e.getMessage());
            return null;
        }

        if (storedEmbedding.isEmpty()) {
            log.warn("authenticateUserByFace: 사용자 ID '{}' 의 저장된 임베딩이 유효하지 않습니다 (비어있음).", userIdToAuthenticate);
            return null;
        }

        // FastAPI에 임베딩 비교 요청을 위해 knownEmbeddings와 knownUserIds를 리스트 형태로 구성 (단일 요소)
        List<List<Float>> knownEmbeddings = Collections.singletonList(storedEmbedding);
        List<String> knownUserIds = Collections.singletonList(userIdToAuthenticate); // FastAPI는 userId로 반환할 것이므로 userId 사용

        log.info("FastAPI로 전송될 knownEmbeddings 개수: {}", knownEmbeddings.size());
        log.info("FastAPI로 전송될 knownUserIds 개수: {}", knownUserIds.size());
        log.info("FastAPI로 전송될 knownUserIds 목록: {}", knownUserIds);


        // 4. FastAPI에 임베딩 비교 요청
        Map<String, Object> comparisonResult = faceRecognitionService.compareEmbeddings(currentEmbedding, knownEmbeddings, knownUserIds);

        log.info("FastAPI 비교 결과: {}", comparisonResult);

        // 결과에서 "match_found"와 "matched_user_id" 추출
        Boolean matchFound = (Boolean) comparisonResult.get("match_found");
        String matchedUserIdFromFastAPI = (String) comparisonResult.get("matched_user_id");

        if (matchFound != null && matchFound && userIdToAuthenticate.equals(matchedUserIdFromFastAPI)) {
            log.info("authenticateUserByFace: 얼굴 인식 성공. 매칭된 userId: {} (로그인 ID: {})", matchedUserIdFromFastAPI, loginId);
            // 인증 성공 시에는 해당 사용자의 userId를 반환
            return userEntity.getLoginId();
        } else {
            log.info("authenticateUserByFace: 얼굴 인식에 실패했거나, 입력한 로그인 ID '{}' 와 일치하는 얼굴이 아닙니다. (FastAPI 매칭 결과 userId: {})", loginId, matchedUserIdFromFastAPI);
            return null;
        }
    }

    public boolean isPhoneExists(String phone) {
        return userRepository.existsByPhoneAndLoginIdIsNotNull(phone);
    }

    @Transactional
    public void resetUserPassword(@NotNull String userId, @NotNull String newRawPassword) {
        // 1. 사용자 존재 여부 확인
        userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("비밀번호 재설정 대상 사용자를 찾을 수 없습니다: " + userId));

        // 2. 실제 비밀번호 재설정 로직은 PwdService에 위임
        // PwdService가 새 비밀번호 암호화, 이력 저장, 과거 비밀번호 재사용 방지 등을 처리합니다.
        pwdService.resetPassword(userId, newRawPassword);

        log.info("UserService에서 비밀번호 재설정 요청 처리 완료: userId={}", userId);
    }
    @Transactional
    public void updateUserRoleToExit(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("역할을 변경할 사용자를 찾을 수 없습니다: " + userId));

        // 사용자의 역할을 "EXIT"로 변경
        userEntity.setRole("EXIT");
        userRepository.save(userEntity);


    }

}