package com.web.memoire.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollView {

    private String collectionid;

    private String authorid;

    private String authorname;

    private String collectionTitle;

    private int readCount;

    private int visibility;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date createdDate;

    private String titleEmbedding;

    private String viewType;

    private String thumbnailPath;

    private String textContent;

    private boolean userlike;

    private boolean userbookmark;

    private String authorProfileImage;

    private String thumbType;

}
