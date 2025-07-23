package com.web.memoire.user.model.service;

import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.SafeSearchAnnotation;
import com.google.cloud.vision.v1.Likelihood;
import org.springframework.beans.factory.annotation.Value;
import com.google.cloud.spring.vision.CloudVisionTemplate;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j; // Slf4j 임포트

import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.jpa.entity.UserEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Slf4j // Lombok을 사용하여 로그 객체 자동 생성
@Service
public class ImageService {

    private final CloudVisionTemplate cloudVisionTemplate;
    private final UserRepository userRepository;
    private final Path fileStorageLocation;

    public ImageService(CloudVisionTemplate cloudVisionTemplate, UserRepository userRepository, @Value("${app.upload.dir}") String uploadDir) {
        this.cloudVisionTemplate = cloudVisionTemplate;
        this.userRepository = userRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("파일 업로드 디렉토리 생성 성공: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("파일 업로드 디렉토리 생성 실패: {}", this.fileStorageLocation, ex);
            throw new RuntimeException("파일을 업로드할 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    /**
     * 이미지가 안전한지 검사합니다.
     * @param file 검사할 이미지 파일
     * @return 안전하면 true, 아니면 false
     */
    public boolean isImageSafe(MultipartFile file) {
        log.info("[Service] 이미지 안전성 검사 시작 (Cloud Vision API 호출 전): fileName={}", file.getOriginalFilename());// 호출 전 로그
        Resource imageResource = file.getResource();
        AnnotateImageResponse response = null;
        try {
            response = this.cloudVisionTemplate.analyzeImage(imageResource, com.google.cloud.vision.v1.Feature.Type.SAFE_SEARCH_DETECTION);
            log.info("[Service] Google Cloud Vision API 응답 수신: fileName={}", file.getOriginalFilename()); // 응답 수신 로그
        } catch (Exception e) {
            log.error("[Service] Google Cloud Vision API 호출 중 오류 발생: fileName={}, 오류: {}", file.getOriginalFilename(), e.getMessage()); // API 호출 실패 로그
            return false; // 오류 발생 시 안전하지 않다고 판단
        }

        SafeSearchAnnotation annotation = response.getSafeSearchAnnotation();

        boolean isSafe = !(annotation.getAdult() == Likelihood.LIKELY || annotation.getAdult() == Likelihood.VERY_LIKELY ||
                annotation.getMedical() == Likelihood.LIKELY || annotation.getMedical() == Likelihood.VERY_LIKELY ||
                annotation.getViolence() == Likelihood.LIKELY || annotation.getViolence() == Likelihood.VERY_LIKELY ||
                annotation.getRacy() == Likelihood.LIKELY || annotation.getRacy() == Likelihood.VERY_LIKELY);

        log.info("[Service] 이미지 안전성 검사 최종 결과: fileName={}, isSafe={}, Adult={}, Medical={}, Violence={}, Racy={}",
                file.getOriginalFilename(), isSafe, annotation.getAdult(), annotation.getMedical(), annotation.getViolence(), annotation.getRacy()); // 최종 결과 로그
        return isSafe;
    }

    /**
     * 프로필 이미지를 저장하고 DB에 경로를 업데이트합니다.
     * @param userId 사용자 ID
     * @param file 저장할 파일
     * @return 저장된 파일 경로 (WebConfig에 매핑된 경로)
     */
    @Transactional
    public String storeProfileImage(String userId, MultipartFile file) {
        log.info("프로필 이미지 저장 시작: userId={}, fileName={}", userId, file.getOriginalFilename());
        Optional<UserEntity> userOptional = userRepository.findByUserId(userId);
        UserEntity user = userOptional.orElseThrow(() -> {
            log.warn("프로필 이미지 저장 실패: 사용자를 찾을 수 없음 (userId={})", userId);
            return new RuntimeException("사용자를 찾을 수 없습니다: " + userId);
        });

        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String fileName = "user_" + userId + extension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String filePathForDb = "/upload_files/user_profile/" + fileName;
            user.setProfileImagePath(filePathForDb);
            userRepository.save(user);

            log.info("프로필 이미지 저장 및 DB 업데이트 성공: userId={}, savedPath={}", userId, filePathForDb);
            return filePathForDb;
        } catch (IOException ex) {
            log.error("파일 저장 실패 (IOException): fileName={}, 오류: {}", fileName, ex.getMessage());
            throw new RuntimeException("파일 " + fileName + "을(를) 저장할 수 없습니다.", ex);
        }
    }
}