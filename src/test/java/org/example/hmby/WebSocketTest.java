package org.example.hmby;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebSocketTest {


    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    @BeforeEach
    public void setup() {
        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new StringMessageConverter());
    }

    @AfterEach
    public void tearDown() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        stompClient.stop();
    }

    @Test
    public void testWebSocketSubscription() throws Exception {
        // 使用 CountDownLatch 来等待异步消息
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        // 创建 WebSocket 连接
        String url = "ws://localhost:8094/ws";
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, @NotNull StompHeaders connectedHeaders) {
                // 订阅消息
                session.subscribe("/topic/messages", new StompFrameHandler() {
                    @NotNull
                    @Override
                    public Type getPayloadType(@NotNull StompHeaders headers) {
                        return String.class;
                    }

                    @Override
                    public void handleFrame(@NotNull StompHeaders headers, Object payload) {
                        receivedMessage.set((String) payload);
                        latch.countDown();
                    }
                });

                // 发送测试消息
                session.send("/topic/messages", "Test Message");
            }

            @Override
            public void handleException(@NotNull StompSession session, StompCommand command,
                                        @NotNull StompHeaders headers, @NotNull byte[] payload, Throwable exception) {
                exception.printStackTrace();
            }
        };

        // 连接到 WebSocket
        stompSession = stompClient.connectAsync(url, sessionHandler)
                .get(10, TimeUnit.SECONDS);

        // 等待消息接收，最多等待5秒
        boolean messageReceived = latch.await(5, TimeUnit.SECONDS);

        // 断言测试
        assertTrue(messageReceived, "应该接收到消息");
        assertNotNull(receivedMessage.get(), "消息内容不应为空");
        assertEquals("Test Message", receivedMessage.get(), "消息内容应该匹配");
    }
}

