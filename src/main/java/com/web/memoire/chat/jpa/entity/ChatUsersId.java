package com.web.memoire.chat.jpa.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatUsersId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String chatroomid;
    private String userid;
}
