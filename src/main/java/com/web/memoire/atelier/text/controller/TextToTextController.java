package com.web.memoire.atelier.text.controller;

import com.web.memoire.atelier.text.model.dto.TextGenerationRequest;
import com.web.memoire.atelier.text.model.dto.TextResultDto;
import com.web.memoire.atelier.text.model.service.TextToTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atelier/text")
@RequiredArgsConstructor
public class TextToTextController {

    private final TextToTextService textToTextService;

    @PostMapping("/generate")
    public ResponseEntity<TextResultDto> generateText(@RequestBody TextGenerationRequest request) {
        TextResultDto result = textToTextService.generateText(request);
        return ResponseEntity.ok(result);
    }
}