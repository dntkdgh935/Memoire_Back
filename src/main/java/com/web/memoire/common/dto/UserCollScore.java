package com.web.memoire.common.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.memoire.common.entity.CollectionEntity;
import com.web.memoire.common.entity.TagTitleSimilarityEntity;
import com.web.memoire.common.entity.UserCollScoreEntity;
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
public class UserCollScore {

    @NotBlank
    private String userid;

    @NotNull
    private int collectionid;

    @NotNull
    private float score;

    @NotNull
    private int seen;

    @NotNull
    private int interacted;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date recAt;

    public UserCollScoreEntity toEntity(){
        return UserCollScoreEntity.builder()
                .userid(userid)
                .collectionid(collectionid)
                .score(score)
                .seen(seen)
                .interacted(interacted)
                .recAt(recAt)
                .build();
    }
}
