package com.example.pitching.voice.controller;

import com.example.pitching.call.controller.VoiceController;
import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.dto.response.UrlRes;
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
class CallControllerTest {

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
    void when_requests_without_authentication_then_returns_unauthorized_status() {
        // given

        // when & then
        client.get()
                .uri("/api/v1/call/voice")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    @WithMockUser
    void when_requests_voice_url_with_authentication_then_returns_voice_connect_url() {
        // given
        String connectUrl = serverProperties.getUrl("voice");

        // when & then
        client.get()
                .uri("/api/v1/call/voice")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(UrlRes.class)
                .value(urlRes -> assertThat(urlRes.url()).isEqualTo(connectUrl))
                .value(System.out::println);
    }

    @Test
    @WithMockUser
    void when_requests_video_url_with_authentication_then_returns_video_connect_url() {
        // given
        String connectUrl = serverProperties.getUrl("video");

        // when & then
        client.get()
                .uri("/api/v1/call/video")
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