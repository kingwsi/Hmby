package org.example.hmby.emby;

import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.config.PropertiesConfig;
import org.example.hmby.feign.EmbyFeignClient;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class EmbyStompEventListener {

    private final ConcurrentHashMap<String, String> sessionVideoMap = new ConcurrentHashMap<>();
    
    private final EmbyFeignClient embyFeignClient;
    
    private final PropertiesConfig propertiesConfig;

    public EmbyStompEventListener(EmbyFeignClient embyFeignClient, PropertiesConfig propertiesConfig) {
        this.embyFeignClient = embyFeignClient;
        this.propertiesConfig = propertiesConfig;
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        System.out.println("客户端发起连接: " + event.getMessage());
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        System.out.println("客户端成功连接: " + event.getMessage());
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        String playSessionId = headerAccessor.getFirstNativeHeader("playSessionId"); // 获取自定义头部

        if ("/topic/player".equals(destination) && playSessionId != null && sessionId != null) {
            sessionVideoMap.put(sessionId, playSessionId);
        }
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("取消订阅主题: " + headerAccessor.getDestination());
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // 获取断开连接时的视频 ID
        if (sessionId != null) {
            String playSessionId = sessionVideoMap.remove(sessionId);
            if (playSessionId != null) {
                try (Response response = embyFeignClient.deleteActiveEncodings(propertiesConfig.getDeviceId(), playSessionId)) {
                    log.info("deleteActiveEncodings: {}", response);
                }
            }
        }
        
        log.info("客户端断开连接: " + headerAccessor.getSessionId());
    }
}
