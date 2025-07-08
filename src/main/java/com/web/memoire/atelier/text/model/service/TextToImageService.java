package com.web.memoire.atelier.text.model.service;

import com.web.memoire.atelier.text.model.dto.ImagePromptRequest;
import com.web.memoire.atelier.text.model.dto.ImageResultDto;

public interface TextToImageService {

    ImageResultDto generateImage(ImagePromptRequest request);
}