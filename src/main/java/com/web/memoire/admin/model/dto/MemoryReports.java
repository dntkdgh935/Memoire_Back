package com.web.memoire.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
public class MemoryReports {

    private int memoryid;
    private int collectionid;
    private String title;
    private String nickname;
    private long reportcount;


}
