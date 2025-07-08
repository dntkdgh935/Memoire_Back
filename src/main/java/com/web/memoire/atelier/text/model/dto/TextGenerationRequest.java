package com.web.memoire.atelier.text.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextGenerationRequest {

    private String content;         // 원본 텍스트
    private String style;           // 변환 스타일
    private String option;          // 기타 요청사항

    // 아래는 저장용 추가 필드
    private String title;           // 생성된 memory 제목
    private String collectionId;    // 저장할 컬렉션 ID
    private boolean saveToMemory;   // 저장 여부 플래그
}