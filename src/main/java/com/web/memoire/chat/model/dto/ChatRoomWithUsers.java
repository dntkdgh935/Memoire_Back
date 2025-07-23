package com.web.memoire.chat.model.dto;

import com.web.memoire.user.model.dto.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
public class ChatRoomWithUsers {
    private String chatroomid;
    private List<User> users;
    private String lastmessageUserId;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
}
