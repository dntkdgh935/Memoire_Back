// com.web.memoire.atelier.text.model.service.TextToImageService.java
package com.web.memoire.atelier.text.model.service;

import com.web.memoire.atelier.text.model.dto.ImagePromptRequest;
import com.web.memoire.atelier.text.model.dto.ImageResultDto;
import com.web.memoire.atelier.text.model.dto.ImageSaveRequest;

public interface TextToImageService {

    /** 기존 DALL·E 생성 호출 */
    ImageResultDto generateImage(ImagePromptRequest request);

    /** 새 메모리로 이미지 저장 */
    ImageResultDto saveNewImageMemory(ImageSaveRequest req);

    /** 기존 메모리 덮어쓰기 */
    ImageResultDto overwriteImageMemory(int memoryId, ImageSaveRequest req);
}