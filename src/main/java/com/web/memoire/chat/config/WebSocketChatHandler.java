package com.web.memoire.chat.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.memoire.chat.jpa.entity.ChatEntity;
import com.web.memoire.chat.model.dto.Chat;
import com.web.memoire.chat.model.service.ChatService;
import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.model.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final JWTUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    // 소켓 세션을 저장할 Set
    private final Map<String, Set<WebSocketSession>> chatRoomSessions = new HashMap<>();

    // 소켓 연결 확인
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String chatroomid = getChatroomid(session);
        String userid = getUserId(session);
        if (chatroomid == null || userid == null) {
            log.warn("유효하지 않은 연결 요청: chatroomid={}, userid={}", chatroomid, userid);
            session.close(CloseStatus.BAD_DATA);
            return;
        }
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
        JsonNode jsonNode = mapper.readTree(payload);

        // 메시지 유형 구분
        String type = jsonNode.get("type").asText();

        // 1. 인증 메시지 처리
        if ("AUTH".equals(type)) {
            String accessToken = jsonNode.get("accessToken").asText();

            // 토큰 유효성 검사
            if (jwtUtil.isTokenExpired(accessToken)) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("AccessToken 만료됨"));
                return;
            }

            String userId = jwtUtil.getUsername(accessToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

            if (userDetails == null) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("사용자 없음"));
                return;
            }

            // 세션에 사용자 정보 저장
            session.getAttributes().put("userid", userId);
            session.sendMessage(new TextMessage("{\"type\":\"AUTH_SUCCESS\"}"));
            log.info("인증 성공: {}", userId);
            return;
        }

        // 2. 인증되지 않은 사용자는 거절
        if (session.getAttributes().get("userid") == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("인증되지 않음"));
            return;
        }



        Chat chat = mapper.readValue(payload, Chat.class);
        log.info(payload);
        log.info(chat.toString());
        chat.setChatId(UUID.randomUUID().toString());
        chat.setSentTime(LocalDateTime.now());

        ChatEntity entity = chat.toEntity();
        chatService.insertChat(entity);

        String broadcastMessage = mapper.writeValueAsString(chat);

        String chatroomid = chat.getChatroomid();
        Set<WebSocketSession> sessions = chatRoomSessions.getOrDefault(chatroomid, Collections.emptySet());

        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(broadcastMessage));
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
        String query = session.getUri().getQuery(); // 예: userid=abc-123
        if (query == null || !query.contains("userid=")) return null;

        return Arrays.stream(query.split("&"))
                .filter(s -> s.startsWith("userid="))
                .map(s -> s.substring("userid=".length()))
                .findFirst()
                .orElse(null);
    }

}
