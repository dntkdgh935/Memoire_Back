package com.web.memoire.chat.jpa.entity;

import com.web.memoire.chat.model.dto.Chat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="TB_CHAT")
@Entity
public class ChatEntity {

    @Id
    @Column(name = "CHAT_ID", nullable = false, length = 50)
    private String chatId;

    @Column(name = "CHATROOMID", nullable = false)
    private String chatroomid;

    @Column(name = "USERID", nullable = false)
    private String userid;

    @Column(name = "MESSAGE_CONTENT", nullable = false, length = 2000)
    private String messageContent;

    @Column(name = "SENT_TIME", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ")
    private LocalDateTime sentTime;

    @Column(name = "READ", nullable = false, columnDefinition = "NUMBER DEFAULT 0")
    private int read;

    public Chat toDto() {
        return Chat.builder()
                .chatId(chatId)
                .chatroomid(chatroomid)
                .userid(userid)
                .messageContent(messageContent)
                .sentTime(sentTime)
                .read(read)
                .build();
    }

}
