package com.web.memoire.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollView {

    private int collectionid;

    private String authorid;

    private String authorname;

    private String collectionTitle;

    private int readCount;

    private int visibility;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date createdDate;

    private String titleEmbedding;

    private String color;

    private String viewType;

    private String thumbnailPath;

    private String textContent;

    private boolean userlike;

    private boolean userbookmark;

    private String authorProfileImage;

    private String thumbType;

    private int likeCount;

    private int bookmarkCount;

    private List<String> collTags;

}
