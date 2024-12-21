package com.example.pitching.chat.config;

import com.example.pitching.chat.handler.ChatWebSocketHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WebSocketConfig.class)
@ActiveProfiles("test")
class WebSocketConfigTest {

    @MockBean
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private SimpleUrlHandlerMapping handlerMapping;

    @Autowired
    private WebSocketHandlerAdapter handlerAdapter;

    @Test
    @DisplayName("WebSocket 핸들러 매핑이 올바른 순서로 생성되어야 한다")
    void webSocketHandlerMapping_ShouldHaveCorrectOrder() {
        assertThat(handlerMapping.getOrder()).isEqualTo(-1);
    }

    @Test
    @DisplayName("WebSocket URL이 올바르게 매핑되어야 한다")
    void webSocketHandlerMapping_ShouldHaveCorrectUrlMapping() {
        Map<String, Object> urlMap = (Map<String, Object>) handlerMapping.getUrlMap();

        assertThat(urlMap).isNotNull();
        assertThat(urlMap).containsKey("/ws/chat/{channelId}");
        assertThat(urlMap.get("/ws/chat/{channelId}"))
                .isInstanceOf(WebSocketHandler.class)
                .isEqualTo(chatWebSocketHandler);
    }

    @Test
    @DisplayName("WebSocket 핸들러 어댑터가 생성되어야 한다")
    void webSocketHandlerAdapter_ShouldBeCreated() {
        assertThat(handlerAdapter).isNotNull();
    }
}