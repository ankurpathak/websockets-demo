package com.github.ankurpathak.websocketsdemo;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnBooleanProperty(prefix = "server", name = "http2.enabled", havingValue = false)
@ServerEndpoint("/ws")
public class Http1WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(Http1WebSocketHandler.class);


    @OnOpen
    public void onOpen(Session session) {
        logger.info("WebSocket connected: {}", session.getId());

        final var userProperties = session.getUserProperties();

        if (userProperties != null) {
            final var remoteAddr = userProperties.get("jakarta.websocket.endpoint.remoteAddress");
            final var localAddr = userProperties.get("jakarta.websocket.endpoint.localAddress");
            logger.info("Remote Addr: {}", remoteAddr);
            logger.info("Local Addr: {}", localAddr);

        } else {
            logger.warn("UserProperties not available");
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        logger.info("Received message: {}", message);

        final var userProperties = session.getUserProperties();

        StringBuilder response = new StringBuilder();
        response.append("Echo: ").append(message).append(" | ");

        if (userProperties != null) {
            final var remoteAddr = userProperties.get("jakarta.websocket.endpoint.remoteAddress");
            final var localAddr = userProperties.get("jakarta.websocket.endpoint.localAddress");
            response.append("Remote Addr: ").append(remoteAddr)
                    .append(" | Local Addr: ").append(localAddr);
        } else {
            logger.warn("UserProperties not available");
        }

        session.getBasicRemote().sendText(response.toString());
    }

    @OnClose
    public void onClose(Session session) {
        logger.info("WebSocket disconnected: {}", session.getId());
    }
}