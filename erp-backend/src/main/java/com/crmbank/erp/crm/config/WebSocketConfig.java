package com.crmbank.erp.crm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.crmbank.erp.crm.handler.CtiWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer, Ordered {

    private final CtiWebSocketHandler ctiWebSocketHandler;

    @Override
    public int getOrder() {
        return -1; // 최우선 순위 (DispatcherServlet보다 우선 처리)
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        /*
         * 📌 [FINAL ARCHITECTURE STANDARD: CTI WebSocket]
         * - Registered Path: "/ws/cti"
         * - Servlet Context Path: "/api" (in application.properties)
         * - Resulting URL on Tomcat: "/api/ws/cti" (Matches Nginx proxy location /api/ws/cti)
         */
        registry.addHandler(ctiWebSocketHandler, "/ws/cti")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        super.beforeHandshake(request, response, wsHandler, attributes);

                        String query = request.getURI().getQuery();
                        if (query != null && query.contains("exten=")) {
                            String exten = query.split("exten=")[1].split("&")[0];
                            attributes.put("inner_no", exten);
                            log.info("🎯 [WebSocket] 내선번호 [{}] 연동 완료", exten);
                        }
                        return true;
                    }
                });
    }
}
