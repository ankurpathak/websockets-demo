package com.github.ankurpathak.websocketsdemo;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http3.server.HTTP3ServerConnectionFactory;
import org.eclipse.jetty.http3.server.HTTP3ServerQuicConfiguration;
import org.eclipse.jetty.quic.quiche.server.QuicheServerConnector;
import org.eclipse.jetty.quic.quiche.server.QuicheServerQuicConfiguration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jetty.JettyServerCustomizer;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Paths;

@SpringBootApplication
public class WebsocketsDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebsocketsDemoApplication.class, args);
    }

}

@RestController
@RequestMapping("/")
class HelloWorld {

    @GetMapping
    public String sayHello() {
        return "Hello, Ankur!";
    }
}


@Configuration
class AddResponseFilter implements Filter {

    @Value( "${server.port}" )
    private Integer serverPort;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setHeader(
                "Alt-Svc", "h3=\":" + serverPort + "\"; ma=86400; persist=1");
        chain.doFilter(request, response);
    }
}



@Configuration
class JettyConfiguration implements WebServerFactoryCustomizer<JettyServletWebServerFactory>  {



    @Autowired
    private DefaultSslBundleRegistry defaultSslBundleRegistry;

    @Value( "${server.port}" )
    private Integer serverPort;


    @Value("${server.http2.enabled}")
    private Boolean http2Enabled;






    @Override
    public void customize(JettyServletWebServerFactory factory) {

        JettyServerCustomizer customizer = server -> {



            var keyStore = defaultSslBundleRegistry.getBundle("server").getStores().getKeyStore();

            var springHandler = server.getHandler();

            Handler[] handlersArr = null;


            if(http2Enabled){
                // If HTTP/2 is disabled, we can just use the Spring handler directly
                // 1. WebSocket handler (RFC 8441)
                WebSocketUpgradeHandler http2WsHandler =
                        WebSocketUpgradeHandler.from(server, container -> {
                            container.addMapping("/ws",
                                    (req, res, _) -> new Http2WebSocketHandler());
                        });

                ContextHandler wsHandler = new ContextHandler("/");
                wsHandler.setHandler(http2WsHandler);

                handlersArr = new Handler[]{
                        wsHandler,        // RFC 8441 first
                        springHandler    // Spring second
                };
            }

            if(handlersArr == null){
                handlersArr = new Handler[]{
                        springHandler    // Spring first
                };
            }


            ContextHandlerCollection handlers = new ContextHandlerCollection();

            handlers.setHandlers(handlersArr);
            server.setHandler(handlers);



            SslContextFactory.Server h3Ssl = new SslContextFactory.Server();
            h3Ssl.setKeyStore(keyStore);
            h3Ssl.setKeyStorePassword("changeit");

            QuicheServerQuicConfiguration quicConfig = HTTP3ServerQuicConfiguration.configure(new QuicheServerQuicConfiguration(Paths.get(System.getProperty("java.io.tmpdir"))));
            quicConfig.setBidirectionalMaxStreams(1024 * 1024);
            HttpConfiguration h3 = new HttpConfiguration();
            h3.addCustomizer(new SecureRequestCustomizer()); // Critical for https resolution
            HTTP3ServerConnectionFactory h3Factory = new HTTP3ServerConnectionFactory(h3);
            QuicheServerConnector quicConnector =
                    new QuicheServerConnector(server, h3Ssl, quicConfig, h3Factory);
            quicConnector.setPort(serverPort);
            server.addConnector(quicConnector);


        };

        factory.addServerCustomizers(customizer);
    }

}








