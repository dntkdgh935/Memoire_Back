package com.web.memoire.chat.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.memoire.chat.jpa.entity.ChatEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
public class Chat {

    @NotNull
    private String chatId;

    @NotNull
    private String chatroomid;

    @NotBlank
    private String userid;

    @NotBlank
    private String messageContent;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentTime;

    @NotNull
    private int read;

    public ChatEntity toEntity() {
        return ChatEntity.builder()
                .chatId(chatId)
                .chatroomid(chatroomid)
                .userid(userid)
                .messageContent(messageContent)
                .sentTime(sentTime)
                .read(read)
                .build();
    }

}
