package com.github.ankurpathak.websocketsdemo;


import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.websocket.server.WebSocketCreator;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;

import java.security.KeyStore;

public class JettyDemo {

    public static void main(String[] args) throws Exception {

        Server server = new Server();

        // --------------------
        // HTTP configuration
        // --------------------
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(8443);

        // --------------------
        // SSL
        // --------------------
        SslContextFactory.Server ssl = new SslContextFactory.Server();
        KeyStore keyStore = new SelfSignedKeyStoreBuilder()
                .alias("key")
                .commonName("demo.local")
                .addDnsName("demo.local")
                .addDnsName("localhost")
                .addIpAddress("127.0.0.1")
                .validityDays(365)
                .build();

        ssl.setKeyStore(keyStore);
        ssl.setKeyStorePassword("changeit");


        // --------------------
        // HTTP/2
        // --------------------
        HTTP2ServerConnectionFactory h2 =
                new HTTP2ServerConnectionFactory(httpConfig);

        ALPNServerConnectionFactory alpn =
                new ALPNServerConnectionFactory("h2");

        SslConnectionFactory sslConn =
                new SslConnectionFactory(ssl, alpn.getProtocol());

        // --------------------
        // Connector (HTTPS + H2)
        // --------------------
        ServerConnector connector = new ServerConnector(
                server,
                sslConn,
                alpn,
                h2
        );
        connector.setPort(8443);
        server.addConnector(connector);

        // --------------------
        // WebSocket handler
        // --------------------
        WebSocketUpgradeHandler wsHandler =
                WebSocketUpgradeHandler.from(server, container -> {
                    container.addMapping("/ws", (req, res, up) -> new Http2WebSocketHandler());

                });

        ContextHandler context = new ContextHandler("/");
        context.setHandler(wsHandler);

        server.setHandler(context);

        // --------------------
        server.start();
        server.join();
    }
}
