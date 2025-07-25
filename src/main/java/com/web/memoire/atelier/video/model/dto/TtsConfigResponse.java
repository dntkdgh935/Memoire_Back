package com.web.memoire.atelier.video.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TtsConfigResponse {
    @JsonProperty("voice_id")
    private String voiceId;
    @JsonProperty("model_id")
    private String modelId;
    @JsonProperty("stability")
    private float  stability;
    @JsonProperty("similarity_boost")
    private float  similarity_boost;
}

