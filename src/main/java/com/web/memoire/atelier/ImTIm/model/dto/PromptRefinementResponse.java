package com.web.memoire.atelier.ImTIm.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptRefinementResponse {
    private String ttsPrompt;
    private String videoPersonPrompt;
    private String videoNopersonPrompt;
    private String imagePrompt;
}
