package com.daniphord.mahanga.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SignalHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("Signal channel connected: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        for (WebSocketSession targetSession : sessions.values()) {
            if (!targetSession.getId().equals(session.getId()) && targetSession.isOpen()) {
                targetSession.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("Signal channel disconnected: sessionId={}, status={}", session.getId(), status);
    }

    public void broadcast(String type, Map<String, Object> payload) {
        try {
            String body = objectMapper.writeValueAsString(Map.of("type", type, "payload", payload));
            TextMessage message = new TextMessage(body);
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException exception) {
            log.warn("Failed to broadcast websocket message type={}", type, exception);
        }
    }
}
