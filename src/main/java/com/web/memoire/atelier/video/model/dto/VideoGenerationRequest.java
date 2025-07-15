package com.web.memoire.atelier.video.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoGenerationRequest {
    private String imageUrl;         // 원본 이미지 URL (필수)
    private String videoPrompt;      // 영상에 사용할 주 프롬프트 (예: “따뜻한 카페 장면”)
    private String extraPrompt;      // 기타 요청 (예: “은은한 필터 효과”)
    private String ttsUrl;
}
