package com.web.memoire.atelier.ImTIm.model.service;

import com.web.memoire.atelier.ImTIm.model.dto.ImTImGenerationRequest;
import com.web.memoire.atelier.ImTIm.model.dto.ImTImResultDto;
import com.web.memoire.common.dto.Memory;
import com.web.memoire.common.entity.MemoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImTImService {
    private final ImTImPythonApiService imtimpythonApiService;
    private final ImTImMemoryService imtimMemoryService;


    public String saveImage(MultipartFile file) {
        // 구현 필요: 파일 저장 로직
        throw new UnsupportedOperationException("saveImage not implemented");
    }
}
