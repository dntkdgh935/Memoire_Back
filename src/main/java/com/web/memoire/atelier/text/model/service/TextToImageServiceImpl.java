package com.web.memoire.atelier.text.model.service;

import com.web.memoire.atelier.text.exception.ImageGenerationException;
import com.web.memoire.atelier.text.jpa.repository.MemoryRepository;
import com.web.memoire.atelier.text.model.dto.ImagePromptRequest;
import com.web.memoire.atelier.text.model.dto.ImageResultDto;
import com.web.memoire.atelier.text.model.dto.ImageSaveRequest;
import com.web.memoire.atelier.text.model.service.TextToImageService;
import com.web.memoire.common.entity.MemoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TextToImageServiceImpl implements TextToImageService {

    private final PythonApiService pythonApiService;
    private final MemoryRepository memoryRepository;

    /** application.properties 의 app.static-images-dir 값을 주입받습니다. */
    @Value("${app.static-images-dir}")
    private String imagesDir;

    private Path imagesPath;

    @PostConstruct
    public void init() {
        try {
            imagesPath = Paths.get(imagesDir);
            Files.createDirectories(imagesPath);
        } catch (Exception e) {
            throw new RuntimeException("이미지 저장 디렉터리 생성 실패: " + imagesDir, e);
        }
    }

    @Override
    public ImageResultDto generateImage(ImagePromptRequest request) {
        // DALL·E 호출만 수행
        return pythonApiService.callDalle(request);
    }

    @Override
    @Transactional
    public ImageResultDto saveNewImageMemory(ImageSaveRequest req) {
        // 1) 원본 메모리 제목 복사 (originalMemoryId 가 넘어왔을 때)
        String finalTitle = "제목 없음";
        if (req.getOriginalMemoryId() != null) {
            Optional<MemoryEntity> orig = memoryRepository.findById(req.getOriginalMemoryId());
            if (orig.isPresent()) {
                finalTitle = orig.get().getTitle();
            }
        }
        // 프론트에서 별도 title 이 넘어오면 그것을 우선
        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            finalTitle = req.getTitle();
        }

        // 2) 이미지 다운로드 & 로컬 저장
        String filename = downloadAndSave(req.getImageUrl());
        String filepath = "/upload_files/memory_img/" + filename;

        // 3) 다음 memoryOrder 계산
        Integer maxOrder = memoryRepository.findMaxMemoryOrderByCollectionId(req.getCollectionId());
        int nextOrder = (maxOrder != null ? maxOrder + 1 : 1);

        // 4) MemoryEntity 생성 & 저장 (content 필드에 filepath 저장)
        MemoryEntity mem = MemoryEntity.builder()
                .title(finalTitle)
                .content(filepath)
                .collectionid(req.getCollectionId())
                .memoryType("image")
                .memoryOrder(nextOrder)
                .filename(filename)
                .filepath(filepath)
                .createdDate(new Date())
                .build();
        memoryRepository.save(mem);

        // 5) 결과 DTO 반환
        return ImageResultDto.builder()
                .imageUrl(filepath)
                .prompt(req.getPrompt())
                .title(mem.getTitle())
                .filename(filename)
                .filepath(filepath)
                .style(req.getStyle())
                .memoryType(mem.getMemoryType())
                .collectionId(mem.getCollectionid())
                .memoryOrder(mem.getMemoryOrder())
                .build();
    }

    @Override
    @Transactional
    public ImageResultDto overwriteImageMemory(int memoryId, ImageSaveRequest req) {
        MemoryEntity mem = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new ImageGenerationException("해당 메모리가 없습니다: " + memoryId));

        // 1) 기존 제목 유지
        String finalTitle = mem.getTitle();

        // 2) 새로운 이미지 다운로드 & 저장
        String filename = downloadAndSave(req.getImageUrl());
        String filepath = "/upload_files/memory_img/" + filename;

        // 3) 엔티티 업데이트 (content 도 filepath 로)
        mem.setTitle(finalTitle);
        mem.setContent(filepath);
        mem.setFilename(filename);
        mem.setFilepath(filepath);
        mem.setMemoryType("image");
        memoryRepository.save(mem);

        // 4) 결과 DTO 반환
        return ImageResultDto.builder()
                .imageUrl(filepath)
                .prompt(req.getPrompt())
                .title(finalTitle)
                .filename(filename)
                .filepath(filepath)
                .style(req.getStyle())
                .memoryType(mem.getMemoryType())
                .collectionId(mem.getCollectionid())
                .memoryOrder(mem.getMemoryOrder())
                .build();
    }

    /**
     * URL에서 이미지를 내려받아,
     * imagesDir 에 저장 후 생성된 파일명을 반환합니다.
     */
    private String downloadAndSave(String imageUrl) {
        try (InputStream in = new URL(imageUrl).openStream()) {
            String clean = imageUrl.split("\\?")[0];
            String ext = clean.substring(clean.lastIndexOf('.') + 1);
            String filename = UUID.randomUUID().toString() + "." + ext;
            Path out = imagesPath.resolve(filename);
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (Exception e) {
            throw new ImageGenerationException("이미지 다운로드 실패: " + e.getMessage(), e);
        }
    }
}