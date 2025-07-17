package com.web.memoire.chat.model.service;

import com.web.memoire.chat.jpa.entity.ChatEntity;
import com.web.memoire.chat.jpa.entity.ChatUsersEntity;
import com.web.memoire.chat.jpa.repository.ChatRepository;
import com.web.memoire.chat.jpa.repository.ChatUsersRepository;
import com.web.memoire.chat.model.dto.Chat;
import com.web.memoire.chat.model.dto.ChatUsers;
import com.web.memoire.user.jpa.entity.UserEntity;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final ChatRepository chatRepository;

    @Autowired
    private final ChatUsersRepository chatUsersRepository;

    // UserRepository
    public User findUserById(String userid) {
        return userRepository.findById(userid)
                .map(UserEntity::toDto)
                .orElseThrow(() -> {
                    log.warn("사용자 조회 실패: userid '{}' 에 해당하는 사용자를 찾을 수 없습니다.", userid);
                    // 적절한 예외를 던집니다. 예를 들어, NoSuchElementException 또는 Custom Exception
                    return new NoSuchElementException("사용자를 찾을 수 없습니다.");
                });
    }

    // chatRepository
    public ArrayList<ChatUsers> findAllByChatroomid(String chatroomid) {
        List<ChatUsersEntity> entityList = chatUsersRepository.findAllByChatroomid(chatroomid);
        ArrayList<ChatUsers> list = new ArrayList<>();
        for (ChatUsersEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    @Transactional
    public int insertChat(ChatEntity entity) {
        return chatRepository.save(entity) == null ? 0 : 1;
    }

    public ArrayList<Chat> findByChatroomidOrderBySentTimeAsc(String chatroomid) {
        List<ChatEntity> entityList = chatRepository.findByChatroomidOrderBySentTimeAsc(chatroomid);
        ArrayList<Chat> list = new ArrayList<>();
        for (ChatEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }


    // chatUsersRepository
    public ArrayList<ChatUsers> findAllByUserId(String userid) {
        List<ChatUsersEntity> entityList = chatUsersRepository.findAllByUserId(userid);
        ArrayList<ChatUsers> list = new ArrayList<>();
        for (ChatUsersEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public String findByUserIdAndOtherUserId(String userid, String otherUserid) {
        return chatUsersRepository.findByUserIdAndOtherUserId(userid, otherUserid);
    }

    @Transactional
    public int insertChatUsers(String chatroomid, ArrayList<String> users) {
        ChatUsersEntity entity = new ChatUsersEntity();
        entity.setChatroomid(chatroomid);
        for (String user : users) {
            entity.setUserid(user);
            if (chatUsersRepository.save(entity) == null) {
                return 0;
            }
        }
        return 1;

    }

}
