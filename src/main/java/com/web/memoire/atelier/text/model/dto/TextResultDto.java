package com.web.memoire.atelier.text.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class TextResultDto {
    private String title;
    private String content;
    private String style;
    private String memoryType;
    private String collectionId;
    private int memoryOrder;
    private String date; // ✅ 추가됨: 생성 일자 (문자열

    public TextResultDto(String content) {
        this.content = content;
    }
}