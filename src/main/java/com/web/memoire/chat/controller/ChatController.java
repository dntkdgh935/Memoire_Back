package com.web.memoire.chat.controller;

import com.web.memoire.chat.model.dto.Chat;
import com.web.memoire.chat.model.dto.ChatRoomWithUsers;
import com.web.memoire.chat.model.dto.ChatUsers;
import com.web.memoire.chat.model.service.ChatService;
import com.web.memoire.user.model.dto.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
                //여기서 채팅방의 마지막 대화를 확인
                Chat chat = chatService.findLatestChat(userChatroom.getChatroomid());
                if (chat == null) {
                    continue;
                }
                list.add(new ChatRoomWithUsers(userChatroom.getChatroomid(), users, chat.getUserid(), chat.getMessageContent(), chat.getSentTime()));

            }
            list.sort((a, b) -> b.getLastMessageTime().compareTo(a.getLastMessageTime()));
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/chatrooms 에러");
        }
    }

    @PostMapping("/private/check")
    public ResponseEntity<?> checkPrivateChatroom(@RequestParam List<String> users) {
        log.info("ChatController.checkChatroom...");
        try {
            String userid = users.get(0);
            String otherUserid = users.get(1);
            log.info("userid : " + userid);
            log.info("otherUserid : " + otherUserid);
            return ResponseEntity.ok(chatService.findByUserIdAndOtherUserIdPrivate(userid, otherUserid));
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/private/check 에러");
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

    @PostMapping("/private/new")
    public ResponseEntity<?> createPrivateChatroom(@RequestParam List<String> users) {
        log.info("ChatController.createChatroom...");
        String chatroomid = UUID.randomUUID().toString();
        if (users.size() != 2) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/private/new 에러");
        }
        if (chatService.insertChatUsersPrivate(chatroomid, (ArrayList<String>) users) > 0) {
            return ResponseEntity.ok(chatroomid);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/private/new 에러");
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
    public ResponseEntity<?> createAdminChatroom(@RequestParam List<String> user) {
        log.info("ChatController.createAdminChatroom...");
        String chatroomid = "admin-" + UUID.randomUUID().toString();
        ArrayList<String> users = new ArrayList<>(user);
        if (chatService.insertChatUsersPrivate(chatroomid, users) > 0) {
            return ResponseEntity.ok(chatroomid);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/admin/new 에러");

    }

    // Assumption: only one user in an admin chatroom (could be multiple admins)
    // Chatting with an admin will create another private chatroom for the user -> creates two private chatrooms for the same users
    @GetMapping("/admin/chatrooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminChatrooms(@RequestParam String userid) {
        log.info("ChatController.getAdminChatrooms...");
        try {
            List<ChatRoomWithUsers> list = new ArrayList<>();

            ArrayList<ChatUsers> chatUsers = chatService.findAdminChatrooms();
            // This loop should only complete once for each admin chatroom
            for (ChatUsers chatUser : chatUsers) {
                ArrayList<User> usersInChatroom = new ArrayList<>();
                // 관리자 본인은 스킵
                if (chatUser.getUserid().equals(userid)) {
                    continue;
                }
                User user = chatService.findUserById(chatUser.getUserid());
                if (user.getRole().equals("admin")) {
                    continue;
                }
                User temp = new User();
                temp.setUserId(user.getUserId());
                temp.setLoginId(user.getLoginId());
                temp.setName(user.getName());
                usersInChatroom.add(temp);
                Chat chat = chatService.findLatestChat(chatUser.getChatroomid());
                if (chat == null) {
                    continue;
                }
                ChatRoomWithUsers chatRoomWithUsers = new ChatRoomWithUsers(chatUser.getChatroomid(), usersInChatroom, chat.getUserid(), chat.getMessageContent(), chat.getSentTime());
                list.add(chatRoomWithUsers);

            }
            list.sort((a, b) -> b.getLastMessageTime().compareTo(a.getLastMessageTime()));
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
                chatService.insertChatUsersPrivate(chatroomid, users);
            }
            return ResponseEntity.ok("사용자 추가 완료");
        } catch (Exception e) {
            log.error("error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("/admin/start 에러");

        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestParam String userid, @RequestParam String chatroomid) {
        log.info("ChatController.getUsers...");
        try {
            ArrayList<ChatUsers> users = chatService.findAllByChatroomid(chatroomid);
            ArrayList<User> userList = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            for (ChatUsers user : users) {
                User real = chatService.findUserById(user.getUserid());
                User temp = new User();
                temp.setUserId(real.getUserId());
                temp.setLoginId(real.getLoginId());
                temp.setName(real.getName());
                userList.add(temp);
            }
            map.put("users", userList);
            map.put("isPrivate", chatService.checkIsPrivate(userid, chatroomid));
            return ResponseEntity.ok(map);
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
