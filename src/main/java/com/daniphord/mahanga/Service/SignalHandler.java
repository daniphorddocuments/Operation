package com.daniphord.mahanga.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
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
        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});
        } catch (IOException parseException) {
            log.debug("Forwarding non-JSON signal message from sessionId={}", session.getId());
            broadcastToPeers(session.getId(), message);
            return;
        }

        payload.put("senderSessionId", session.getId());
        String outgoingMessage = objectMapper.writeValueAsString(payload);
        TextMessage outbound = new TextMessage(outgoingMessage);

        Object targetSessionId = payload.get("targetSessionId");
        if (targetSessionId instanceof String targetId && !targetId.isBlank()) {
            WebSocketSession targetSession = sessions.get(targetId);
            if (targetSession != null && targetSession.isOpen()) {
                targetSession.sendMessage(outbound);
            } else {
                log.debug("Skipping signal relay because target session is unavailable: targetSessionId={}", targetId);
            }
            return;
        }

        broadcastToPeers(session.getId(), outbound);
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
            broadcastToPeers(null, message);
        } catch (IOException exception) {
            log.warn("Failed to broadcast websocket message type={}", type, exception);
        }
    }

    private void broadcastToPeers(String excludedSessionId, TextMessage message) throws IOException {
        for (WebSocketSession targetSession : sessions.values()) {
            if (targetSession.isOpen() && (excludedSessionId == null || !targetSession.getId().equals(excludedSessionId))) {
                targetSession.sendMessage(message);
            }
        }
    }
}
