package com.web.memoire.atelier.text.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImagePromptRequest {
    private String prompt;         // 이미지 생성용 프롬프트
    private String style;          // 선택된 스타일
    private String title;          // 저장 시 제목
    private String content;        // 저장 시 내용(설명)
    private Long collectionId;     // 소속 컬렉션 ID
    private boolean saveToMemory;  // 저장 여부

    // ✅ FastAPI와의 연동을 위해 아래 필드 추가
    private String userId;         // 사용자 ID (FastAPI 기록용)
    private String memoryType;     // 예: "image"
    private int memoryOrder;       // 정렬 순서 (기본 0)
}