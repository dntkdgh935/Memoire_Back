package com.web.memoire.atelier.text.model.service;

import com.web.memoire.atelier.text.exception.TextGenerationException;
import com.web.memoire.atelier.text.jpa.repository.MemoryRepository;
import com.web.memoire.atelier.text.model.dto.TextGenerationRequest;
import com.web.memoire.atelier.text.model.dto.TextResultDto;
import com.web.memoire.common.entity.MemoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TextToTextServiceImpl implements TextToTextService {

    private final PythonApiService pythonApiService;
    private final MemoryRepository memoryRepository;  // ✅ 직접 주입

    @Override
    public TextResultDto generateText(TextGenerationRequest request) {
        try {
            // ✅ GPT 프롬프트 텍스트 확인 로그
            System.out.println("DEBUG: [Service] inputText = " + request.getInputText());

            // 🔥 FastAPI 호출 (GPT 결과 생성)
            TextResultDto resultDto = pythonApiService.callGpt(request);

            // ✅ 저장은 하지 않음! (버튼 클릭 시에만 저장하게 설정)
            return resultDto;

        } catch (Exception e) {
            throw new TextGenerationException("텍스트 생성 중 오류 발생", e);
        }
    }
}