package com.web.memoire.atelier.text.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextGenerationRequest {

    private String content;
    private String style;
    private String option;
    private String title;

    @JsonProperty("collectionId")
    private int collectionId;    // 저장할 컬렉션 ID

    @JsonProperty("saveToMemory")
    private boolean saveToMemory;

    @JsonProperty("inputText")
    private String inputText;

    @JsonProperty("memoryType")
    private String memoryType;

    @JsonProperty("memoryOrder")
    private Integer memoryOrder;

    @JsonProperty("memoryId")
    private Long memoryId;

    @JsonProperty("userId")
    private String userId;
}