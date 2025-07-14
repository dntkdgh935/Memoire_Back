package com.web.memoire.atelier.text.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextGenerationRequest {

    // 🔸 프론트에서 받는 값
    private String content;         // 사용자 입력 원문
    private String style;           // 변환 스타일
    private String option;          // 부가 옵션

    // 🔸 저장 옵션
    private String title;           // 결과물 제목

    @JsonProperty("collectionId")
    private String collectionId;    // 저장할 컬렉션 ID

    @JsonProperty("saveToMemory")
    private boolean saveToMemory;   // 저장 여부 플래그

    // 🔸 FastAPI에 보낼 추가 필드

    @JsonProperty("inputText")
    private String inputText;       // GPT에 보낼 원문

    @JsonProperty("memoryType")
    private String memoryType;      // 메모리 유형 (text)

    @JsonProperty("memoryOrder")
    private Integer memoryOrder;    // 순서
}