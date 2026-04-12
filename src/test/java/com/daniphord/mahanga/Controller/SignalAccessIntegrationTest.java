package com.daniphord.mahanga.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SignalAccessIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void anonymousUserCanOpenSignalChannel() throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.execute(new TextWebSocketHandler(), "ws://localhost:" + port + "/signal")
                .get(5, TimeUnit.SECONDS);

        try {
            assertTrue(session.isOpen());
        } finally {
            session.close();
        }
    }
}
