package com.web.memoire.atelier.text.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImagePromptRequest {
    private String prompt;
    private String style;
    private String title;
    private String content;
    private int collectionId;
    private boolean saveToMemory;
    private String userId;
    private String memoryType;
    private int memoryOrder;
}