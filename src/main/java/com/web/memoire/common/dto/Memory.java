package com.web.memoire.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.memoire.common.entity.MemoryEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
public class Memory {
    @NotNull
    private int memoryid;
    @NotBlank
    private String memoryType;
    @NotNull
    private int collectionid;
    @NotBlank
    private String title;
    private String content;
    private String filename;
    private String filepath;
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date createdDate;
    @NotNull
    private int memoryOrder;

    public MemoryEntity toEntity() {
        return MemoryEntity.builder()
                .memoryid(memoryid)
                .memoryType(memoryType)
                .collectionid(collectionid)
                .title(title)
                .content(content)
                .filename(filename)
                .filepath(filepath)
                .createdDate(createdDate)
                .memoryOrder(memoryOrder)
                .build();
    }
}
