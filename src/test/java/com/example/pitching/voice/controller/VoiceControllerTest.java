package com.example.pitching.voice.controller;

import com.example.pitching.voice.dto.properties.ServerProperties;
import com.example.pitching.voice.dto.response.UrlRes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@EnableConfigurationProperties(ServerProperties.class)
@WebFluxTest(controllers = VoiceController.class)
class VoiceControllerTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private ServerProperties serverProperties;

    @Test
    void webTestClient_should_not_be_null() {
        assertThat(client).isNotNull();
    }

    @Test
    void serverProperties_should_not_be_null() {
        assertThat(serverProperties).isNotNull();
    }

    @Test
    void when_requests_without_mock_user_then_returns_unauthorized_status() {
        // given

        // when & then
        client.get()
                .uri("/api/v1/websocket")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    @WithMockUser
    void when_requests_with_mock_user_then_returns_connect_url() {
        // given
        String connectUrl = serverProperties.getUrl(false);

        // when & then
        client.get()
                .uri("/api/v1/websocket")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(UrlRes.class)
                .value(urlRes -> assertThat(urlRes.url()).isEqualTo(connectUrl))
                .value(System.out::println);
    }
}