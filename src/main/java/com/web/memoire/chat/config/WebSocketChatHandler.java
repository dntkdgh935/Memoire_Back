package com.web.memoire.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.memoire.chat.jpa.entity.ChatEntity;
import com.web.memoire.chat.model.dto.Chat;
import com.web.memoire.chat.model.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper;
    private final ChatService chatService;

    // 소켓 세션을 저장할 Set
    private final Map<String, Set<WebSocketSession>> chatRoomSessions = new HashMap<>();

    // 소켓 연결 확인
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String chatroomid = getChatroomid(session);
        String userid = getUserId(session);
        session.getAttributes().put("chatroomid", chatroomid);
        session.getAttributes().put("userid", userid);
        chatRoomSessions.computeIfAbsent(chatroomid, k -> new HashSet<>()).add(session);
        log.info("사용자 {}가 채팅방 {}에 연결됨 (세션 ID: {})", userid, chatroomid, session.getId());
        List<Chat> previousMessages = chatService.findByChatroomidOrderBySentTimeAsc(chatroomid);

        String messagesJson = mapper.writeValueAsString(previousMessages);
        session.sendMessage(new TextMessage(messagesJson));
        session.sendMessage(new TextMessage("채팅방 [" + chatroomid + "]에 연결되었습니다."));
    }

    // 소켓 메세지 처리
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        String payload = message.getPayload();
        Chat chat = mapper.readValue(payload, Chat.class);
        log.info(payload);
        log.info(chat.toString());
        chat.setChatId(UUID.randomUUID().toString());
        chat.setSentTime(LocalDateTime.now());

        ChatEntity entity = chat.toEntity();
        chatService.insertChat(entity);
        String chatroomid = chat.getChatroomid();
        Set<WebSocketSession> sessions = chatRoomSessions.getOrDefault(chatroomid, Collections.emptySet());

        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(payload));
            }
        }
    }

    // 소켓 연결 종료
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String chatroomid = (String) session.getAttributes().get("chatroomid");
        String userid = (String) session.getAttributes().get("userid");
        if (chatroomid != null) {
            Set<WebSocketSession> sessions = chatRoomSessions.get(chatroomid);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    chatRoomSessions.remove(chatroomid);
                }
            }
        }
        log.info("사용자 {}의 연결이 종료됨 (채팅방: {}, 세션 ID: {})", userid, chatroomid, session.getId());


    }

    private String getChatroomid(WebSocketSession session) {
        String path = Objects.requireNonNull(session.getUri()).getPath();
        return path.substring(path.lastIndexOf("/") + 1);
    }

    private String getUserId(WebSocketSession session) {
        return session.getId(); // TODO: 인증 기반 사용자 ID 추출로 대체 가능
    }

}
