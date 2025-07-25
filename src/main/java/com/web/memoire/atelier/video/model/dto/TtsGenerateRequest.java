package com.web.memoire.atelier.video.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TtsGenerateRequest {
    private String speech;
    private String voiceId;
    private String modelId;
    private float  pitch;
    private float  rate;
}
