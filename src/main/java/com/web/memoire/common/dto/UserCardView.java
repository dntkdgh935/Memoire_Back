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
public class UserCardView {

    private String loginId;
    private String nickname;
    private String profileImagePath;
    private String statusMessage;
    private List<String> userFreqTags;
    private String relStatusWLoginUser;

}