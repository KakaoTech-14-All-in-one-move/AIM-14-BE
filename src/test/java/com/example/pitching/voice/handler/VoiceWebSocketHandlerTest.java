package com.example.pitching.voice.handler;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ConnectResOp;
import com.example.pitching.call.operation.req.Heartbeat;
import com.example.pitching.call.operation.res.Hello;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VoiceWebSocketHandlerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient client;

    @Autowired
    private ServerProperties serverProperties;

    private ObjectMapper objectMapper;

    private ReactorNettyWebSocketClient webSocketClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        webSocketClient = new ReactorNettyWebSocketClient();
    }

    @Test
    void webTestClient_should_not_be_null() {
        assertThat(client).isNotNull();
    }

    @Test
    void serverProperties_should_not_be_null() {
        assertThat(serverProperties).isNotNull();
    }

    @Nested
    class EstablishConnection {
        @Test
        void when_connects_web_socket_without_authentication_then_returns_unauthorized_status() {
            // Given
            String path = serverProperties.getVoicePath();

            // When & Then
            client.get()
                    .uri(path)
                    .exchange()
                    .expectStatus()
                    .isUnauthorized();
        }

        @Test
        @WithMockUser
        void when_connects_web_socket_then_returns_hello_operation() {
            // Given
            URI uri = URI.create(serverProperties.getUrlForTest(port, "voice"));

            // When & Then
            webSocketClient.execute(uri, session -> {
                Flux<Hello> helloFlux = session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .log()
                        .map(json -> jsonToEvent(json, Hello.class))
                        .doOnNext(hello -> {
                            assertThat(hello.op()).isEqualTo(ConnectResOp.HELLO);
                            assertThat(hello.data().heartbeatInterval())
                                    .isEqualTo(serverProperties.getHeartbeatInterval());
                        })
                        .take(1, true);

                return helloFlux.then();
            }).block();
        }
    }

    @Nested
    class SendHeartbeat {
        @Test
        @WithMockUser
        @Disabled("서버로부터 두번째 메세지 수신이 안됨..")
        void when_sends_heartbeat_then_returns_heartbeat_ack() {
            // Given
            URI uri = URI.create(serverProperties.getUrlForTest(port, "voice"));

            // When & Then
            webSocketClient.execute(uri, session -> {
                Mono<Void> sendMono = sendMessage(session, Heartbeat.of());

                Flux<String> heartbeatAckFlux = session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .log();

                return sendMono.thenMany(heartbeatAckFlux).then();
            }).block();
        }
    }

    private Mono<Void> sendMessage(WebSocketSession session, Operation operation) {
        return session.send(Mono.just(session.textMessage(eventToJson(operation))));
    }

    private <T extends Operation> T jsonToEvent(String jsonMessage, Class<T> eventClass) {
        try {
            return objectMapper.readValue(jsonMessage, eventClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + eventClass.getSimpleName(), e);
        }
    }

    private String eventToJson(Operation event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isTargetOperation(String jsonMessage, ConnectResOp op) {
        try {
            ConnectResOp resOp = ConnectResOp.from(objectMapper.readTree(jsonMessage).get("op").asInt());
            return Objects.equals(op, resOp);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}