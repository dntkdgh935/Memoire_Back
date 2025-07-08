package com.web.memoire.atelier.text.model.dto;

public class TextGenerationRequest {
    private String prompt;        // 요청사항
    private String style;         // 선택된 스타일
    private String originTitle;   // 기존 메모리 제목
    private String originContent; // 기존 본문
    private Long collectionId;
}
