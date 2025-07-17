package com.web.memoire.chat.controller;

import com.web.memoire.chat.model.dto.ChatRoomWithUsers;
import com.web.memoire.chat.model.dto.ChatUsers;
import com.web.memoire.chat.model.service.ChatService;
import com.web.memoire.user.model.dto.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/chatrooms")
    public ResponseEntity<?> getChatrooms(@RequestParam String userid) {
        log.info("ChatController.getChatrooms...");
        try {
            List<ChatRoomWithUsers> list = new ArrayList<>();
            ArrayList<ChatUsers> userChatrooms = chatService.findAllByUserId(userid);
            for (ChatUsers userChatroom : userChatrooms) {
                ArrayList<ChatUsers> usersInChatroom = chatService.findAllByChatroomid(userChatroom.getChatroomid());
                List<User> users = new ArrayList<>();
                for (ChatUsers chatUser : usersInChatroom) {

                    // 본인이면 스킵
                    if (chatUser.getUserid().equals(userid)) {
                        continue;
                    }
                    // 유저 상세 정보 가져오기
                    User user = chatService.findUserById(chatUser.getUserid());

                    // 필요한 정보만 추출
                    User temp = new User();
                    temp.setUserId(user.getUserId());
                    temp.setLoginId(user.getLoginId());
                    temp.setName(user.getName());
                    users.add(temp);
                }
                list.add(new ChatRoomWithUsers(userChatroom.getChatroomid(), users));

            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/chatrooms 에러");
        }
    }

    @PostMapping("/check")
    public ResponseEntity<?> checkChatroom(@RequestParam String userid, @RequestParam String otherUserid) {
        log.info("ChatController.checkChatroom...");
        try {
            log.info("userid : " + userid);
            log.info("otherUserid : " + otherUserid);
            return ResponseEntity.ok(chatService.findByUserIdAndOtherUserId(userid, otherUserid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/check 에러");
        }
    }

    @PostMapping("/new")
    public ResponseEntity<?> createChatroom(@RequestParam String userid, @RequestParam String otherUserid) {
        log.info("ChatController.createChatroom...");
        String chatroomid = UUID.randomUUID().toString();
        ArrayList<String> users = new ArrayList<>();
        users.add(userid);
        users.add(otherUserid);
        if (chatService.insertChatUsers(chatroomid, users) > 0) {
            return ResponseEntity.ok(chatroomid);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/new 에러");

    }





}
