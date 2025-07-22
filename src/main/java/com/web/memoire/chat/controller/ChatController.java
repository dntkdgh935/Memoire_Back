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
    public ResponseEntity<?> checkChatroom(@RequestParam List<String> users) {
        log.info("ChatController.checkChatroom...");
        try {
            String userid = users.get(0);
            String otherUserid = users.get(1);
            log.info("userid : " + userid);
            log.info("otherUserid : " + otherUserid);
            return ResponseEntity.ok(chatService.findByUserIdAndOtherUserId(userid, otherUserid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/check 에러");
        }
    }

    @PostMapping("/new")
    public ResponseEntity<?> createChatroom(@RequestParam List<String> users) {
        log.info("ChatController.createChatroom...");
        String chatroomid = UUID.randomUUID().toString();
        if (chatService.insertChatUsers(chatroomid, (ArrayList<String>) users) > 0) {
            return ResponseEntity.ok(chatroomid);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/new 에러");

    }

    @PostMapping("/admin/check")
    public ResponseEntity<?> checkAdminChatroom(@RequestParam String user) {
        log.info("ChatController.checkAdminChatroom...");
        try {
            log.info("userid : " + user);
            return ResponseEntity.ok(chatService.findAdminChatroom(user));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/admin/check 에러");
        }
    }

    @PostMapping("/admin/new")
    public ResponseEntity<?> createAdminChatroom(@RequestParam String user) {
        log.info("ChatController.createAdminChatroom...");
        String chatroomid = "admin-" + UUID.randomUUID().toString();
        ArrayList<String> users = new ArrayList<>();
        users.add(user);
        if (chatService.insertChatUsers(chatroomid, users) > 0) {
            return ResponseEntity.ok(chatroomid);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/admin/new 에러");

    }

    @GetMapping("/admin/chatrooms")
    public ResponseEntity<?> getAdminChatrooms(@RequestParam String userid) {
        log.info("ChatController.getAdminChatrooms...");
        try {
            List<ChatRoomWithUsers> list = new ArrayList<>();

            ArrayList<ChatUsers> chatUsers = chatService.findAdminChatrooms();
            for (ChatUsers chatUser : chatUsers) {
                ArrayList<User> usersInChatroom = new ArrayList<>();
                // 관리자 본인은 스킵
                if (chatUser.getUserid().equals(userid)) {
                    continue;
                }
                User user = chatService.findUserById(chatUser.getUserid());
                User temp = new User();
                temp.setUserId(user.getUserId());
                temp.setLoginId(user.getLoginId());
                temp.setName(user.getName());
                usersInChatroom.add(temp);
                ChatRoomWithUsers chatRoomWithUsers = new ChatRoomWithUsers(chatUser.getChatroomid(), usersInChatroom);
                list.add(chatRoomWithUsers);

            }
            return ResponseEntity.ok(list);

        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/chatrooms 에러");

        }
    }

    @GetMapping("/admin/start")
    public ResponseEntity<?> addAdminToChatroom(@RequestParam String userid, @RequestParam String chatroomid) {
        try {
            if (!chatService.checkChatroom(userid, chatroomid)) {
                ArrayList<String> users = new ArrayList<>();
                users.add(userid);
                chatService.insertChatUsers(chatroomid, users);
            }
            return ResponseEntity.ok("사용자 추가 완료");
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/admin/start 에러");

        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestParam String chatroomid) {
        log.info("ChatController.getUsers...");
        try {
            ArrayList<ChatUsers> users = chatService.findAllByChatroomid(chatroomid);
            ArrayList<User> userList = new ArrayList<>();
            for (ChatUsers user : users) {
                User real = chatService.findUserById(user.getUserid());
                User temp = new User();
                temp.setUserId(real.getUserId());
                temp.setLoginId(real.getLoginId());
                temp.setName(real.getName());
                userList.add(temp);
            }
            return ResponseEntity.ok(userList);
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/users 에러");
        }
    }

    @PostMapping("/leave")
    public ResponseEntity<?> leaveChatroom(@RequestParam String userid, @RequestParam String chatroomid) {
        log.info("ChatController.leaveChatroom...");
        try {
            if (chatService.leaveChatroom(userid, chatroomid) > 0) {
                return ResponseEntity.ok("채팅방 나가기 완료");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/leave 에러");

        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/leave 에러");

        }
    }

    @PostMapping("/invite")
    public ResponseEntity<?> inviteChatroom(@RequestParam List<String> users, @RequestParam String chatroomid) {
        log.info("ChatController.inviteChatroom...");
        try {
            if (chatService.insertChatUsers(chatroomid, (ArrayList<String>) users) > 0) {
                return ResponseEntity.ok("초대 완료");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/invite 에러");
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/invite 에러");

        }
    }

}
