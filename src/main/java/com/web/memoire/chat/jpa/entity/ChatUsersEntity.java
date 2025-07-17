package com.web.memoire.chat.jpa.entity;

import com.web.memoire.chat.model.dto.ChatUsers;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="TB_CHAT_USERS")
@Entity
@IdClass(ChatUsersId.class)
public class ChatUsersEntity {

    @Id
    @Column(name = "CHATROOMID", nullable = false, length = 50)
    private String chatroomid;

    @Id
    @Column(name = "USERID", nullable = false)
    private String userid;

    public ChatUsers toDto() {
        return ChatUsers.builder()
                .chatroomid(chatroomid)
                .userid(userid)
                .build();
    }

}
