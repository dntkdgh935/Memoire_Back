package com.web.memoire.atelier.text.model.dto;

public class TextResultDto {
    private String result;
    private String type;

    // ✅ 기본 생성자 (없으면 Jackson이 오류)
    public TextResultDto() {}

    // ✅ 커스텀 생성자 추가
    public TextResultDto(String result, String type) {
        this.result = result;
        this.type = type;
    }

    // ✅ Getter/Setter
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}