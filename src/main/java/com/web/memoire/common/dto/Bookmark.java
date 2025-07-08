package com.web.memoire.common.dto;

import com.web.memoire.common.entity.BookmarkEntity;
import jakarta.validation.constraints.NotBlank;
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
public class Bookmark {

    @NotBlank
    private String userid;

    @NotBlank
    private String collectionid;

    public BookmarkEntity toEntity() {
        return BookmarkEntity.builder()
                .userid(userid)
                .collectionid(collectionid)
                .build();
    }
}
