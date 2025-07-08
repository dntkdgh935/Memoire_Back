package com.web.memoire.atelier.text.controller;

import com.web.memoire.atelier.text.model.dto.ImagePromptRequest;
import com.web.memoire.atelier.text.model.dto.ImageResultDto;
import com.web.memoire.atelier.text.model.service.TextToImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atelier/image")
@RequiredArgsConstructor
public class TextToImageController {

    private final TextToImageService textToImageService;

    @PostMapping("/generate")
    public ResponseEntity<ImageResultDto> generateImage(@RequestBody ImagePromptRequest request) {
        ImageResultDto result = textToImageService.generateImage(request);
        return ResponseEntity.ok(result);
    }
}