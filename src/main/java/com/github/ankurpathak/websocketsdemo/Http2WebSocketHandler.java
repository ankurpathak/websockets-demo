package com.github.ankurpathak.websocketsdemo;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@WebSocket
public class Http2WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(Http2WebSocketHandler.class);

    @OnWebSocketOpen
    public void onOpen(Session session) {
        session.setIdleTimeout(Duration.ZERO);
        logger.info("WebSocket connected: {}", session.getRemoteSocketAddress());

        String remoteAddr = session.getRemoteSocketAddress() != null ? session.getRemoteSocketAddress().toString() : "Unknown";
        String localAddr = session.getLocalSocketAddress() != null ? session.getLocalSocketAddress().toString() : "Unknown";

        logger.info("Remote Addr: {}", remoteAddr);
        logger.info("Local Addr: {}", localAddr);
    }

    @OnWebSocketMessage
    public void onMessage(String message, Session session) {
        logger.info("Received message: {}", message);

        String remoteAddr = session.getRemoteSocketAddress() != null ? session.getRemoteSocketAddress().toString() : "Unknown";
        String localAddr = session.getLocalSocketAddress() != null ? session.getLocalSocketAddress().toString() : "Unknown";

        StringBuilder response = new StringBuilder();
        response.append("Echo: ").append(message).append(" | ")
                .append("Remote Addr: ").append(remoteAddr)
                .append(" | Local Addr: ").append(localAddr);

        session.sendText(response.toString(), Callback.NOOP);
    }

    @OnWebSocketClose
    public void onClose(Session session) {
        logger.info("WebSocket disconnected: {}", session.getRemoteSocketAddress());
    }
}