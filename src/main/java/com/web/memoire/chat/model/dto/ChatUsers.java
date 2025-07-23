package com.web.memoire.chat.model.dto;

import com.web.memoire.chat.jpa.entity.ChatUsersEntity;
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
public class ChatUsers {

    @NotBlank
    private String chatroomid;

    @NotBlank
    private String userid;

    @NotNull
    private char isPrivate;

    public ChatUsersEntity toEntity() {
        return ChatUsersEntity.builder()
                .chatroomid(chatroomid)
                .userid(userid)
                .isPrivate(isPrivate)
                .build();
    }

}
