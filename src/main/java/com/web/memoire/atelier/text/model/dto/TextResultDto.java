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
    private String title;          // 제목
    private String content;        // 변환된 내용 (text result)
    private String style;          // 적용한 스타일
    private String memoryType;     // 예: "text"
    private String collectionId;   // 컬렉션 소속
    private int memoryOrder;       // 정렬 순서 (UI에서 자동 부여 or 서버 계산)

    // ✅ 이 생성자만 추가해주면 끝!
    public TextResultDto(String content) {
        this.content = content;
    }
}