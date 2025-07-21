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

import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.jpa.entity.UserEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Service
public class ImageService {

    private final CloudVisionTemplate cloudVisionTemplate;
    private final UserRepository userRepository;
    private final Path fileStorageLocation;

    public ImageService(CloudVisionTemplate cloudVisionTemplate, UserRepository userRepository, @Value("${app.upload.dir}") String uploadDir) {
        this.cloudVisionTemplate = cloudVisionTemplate;
        this.userRepository = userRepository;
        // app.upload.dir은 C:/upload_files/user_profile/ 와 같은 절대 경로여야 합니다.
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("파일을 업로드할 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    /**
     * 이미지가 안전한지 검사합니다.
     * @param file 검사할 이미지 파일
     * @return 안전하면 true, 아니면 false
     */
    public boolean isImageSafe(MultipartFile file) {
        Resource imageResource = file.getResource();
        AnnotateImageResponse response = this.cloudVisionTemplate.analyzeImage(imageResource, com.google.cloud.vision.v1.Feature.Type.SAFE_SEARCH_DETECTION);
        SafeSearchAnnotation annotation = response.getSafeSearchAnnotation();

        return !(annotation.getAdult() == Likelihood.LIKELY || annotation.getAdult() == Likelihood.VERY_LIKELY ||
                annotation.getMedical() == Likelihood.LIKELY || annotation.getMedical() == Likelihood.VERY_LIKELY ||
                annotation.getViolence() == Likelihood.LIKELY || annotation.getViolence() == Likelihood.VERY_LIKELY ||
                annotation.getRacy() == Likelihood.LIKELY || annotation.getRacy() == Likelihood.VERY_LIKELY);
    }

    /**
     * 프로필 이미지를 저장하고 DB에 경로를 업데이트합니다.
     * @param userId 사용자 ID
     * @param file 저장할 파일
     * @return 저장된 파일 경로 (WebConfig에 매핑된 경로)
     */
    @Transactional
    public String storeProfileImage(String userId, MultipartFile file) {
        Optional<UserEntity> userOptional = userRepository.findByUserId(userId);
        UserEntity user = userOptional.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String fileName = "user_" + userId + extension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // DB에 저장할 파일 경로를 WebConfig의 ResourceHandler에 맞춥니다.
            // 예: /upload_files/user_profile/user_123.jpg
            String filePathForDb = "/upload_files/user_profile/" + fileName;
            user.setProfileImagePath(filePathForDb);
            userRepository.save(user);

            return filePathForDb;
        } catch (IOException ex) {
            throw new RuntimeException("파일 " + fileName + "을(를) 저장할 수 없습니다.", ex);
        }
    }
}
