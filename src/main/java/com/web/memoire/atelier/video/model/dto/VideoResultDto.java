package com.web.memoire.atelier.video.model.dto;

import com.web.memoire.common.entity.MemoryEntity;
import lombok.*;

// collection 으로 진입, 수정 필수

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoResultDto {
    private String videoUrl;
    private String fileName;
    private String title;
}
