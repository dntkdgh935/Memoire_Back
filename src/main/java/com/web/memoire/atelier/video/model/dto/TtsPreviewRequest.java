package com.web.memoire.atelier.video.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TtsPreviewRequest {
    private String script;
    private String speech;
    private String gender;
}
