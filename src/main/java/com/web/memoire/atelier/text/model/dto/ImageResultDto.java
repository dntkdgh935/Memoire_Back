package com.web.memoire.atelier.text.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageResultDto {
    private String imageUrl;       // 생성된 이미지 URL
    private String prompt;         // 사용한 프롬프트
    private String title;          // 이미지 제목
    private String filename;       // 저장 파일명
    private String filepath;       // 저장 경로
    private String style;          // 적용한 스타일
    private String memoryType;     // 예: "image"
    private int collectionId;   // 소속 컬렉션
    private int memoryOrder;       // 정렬 순서
}