package com.web.memoire.atelier.text.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImagePromptRequest {
    private String prompt;        // 이미지 생성용 프롬프트
    private String style;         // 선택된 스타일
    private String title;         // 저장 시 제목
    private String content;       // 저장 시 내용(설명)
    private Long collectionId;    // 소속 컬렉션 ID
    private boolean saveToMemory; // 저장 여부
}