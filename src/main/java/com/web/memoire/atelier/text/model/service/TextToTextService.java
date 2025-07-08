package com.web.memoire.atelier.text.model.service;

import com.web.memoire.atelier.text.model.dto.TextGenerationRequest;
import com.web.memoire.atelier.text.model.dto.TextResultDto;
import com.web.memoire.atelier.text.exception.TextGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

public interface TextToTextService {
    TextResultDto generateText(TextGenerationRequest request);
}