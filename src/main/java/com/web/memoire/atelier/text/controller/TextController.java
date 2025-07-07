package com.web.memoire.atelier.text.controller;

import com.web.memoire.atelier.text.model.dto.TextGenerationRequest;
import com.web.memoire.atelier.text.model.dto.TextResultDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/atelier")
public class TextController {

    @PostMapping("/generate-text")
    public ResponseEntity<TextResultDto> generateText(@RequestBody TextGenerationRequest request) {
        // 일단 빈 응답만 리턴
        return ResponseEntity.ok(new TextResultDto("sample text", "text"));
    }

    @PostMapping("/generate-image")
    public ResponseEntity<TextResultDto> generateImage(@RequestBody TextGenerationRequest request) {
        return ResponseEntity.ok(new TextResultDto("https://example.com/image.jpg", "image"));
    }
}
