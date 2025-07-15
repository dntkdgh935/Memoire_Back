package com.web.memoire.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.memoire.common.entity.LikeEntity;
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
public class Like {

    @NotBlank
    private int collectionid;

    @NotBlank
    private String userid;

    @NotNull
    @JsonFormat(pattern = "yyyy.MM.dd")
    private Date likedDate;

    public LikeEntity toEntity() {
        return LikeEntity.builder()
                .collectionid(collectionid)
                .userid(userid)
                .likedDate(likedDate)
                .build();
    }
}
