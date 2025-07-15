package com.web.memoire.common.dto;

import com.web.memoire.common.entity.CollectionTagEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CollectionTag {

    @NotNull
    private int collectionid;

    @NotNull
    private int tagid;

    public CollectionTagEntity toEntity() {
        return CollectionTagEntity.builder()
                .collectionid(collectionid)
                .tagid(tagid)
                .build();
    }
}
