package com.example.pitching.channel.controller;

import com.example.pitching.config.SecurityTestConfig;
import com.example.pitching.user.controller.ChannelController;
import com.example.pitching.user.domain.Channel;
import com.example.pitching.user.domain.utility.ChannelCategory;
import com.example.pitching.user.dto.ChannelResponse;
import com.example.pitching.user.dto.CreateChannelRequest;
import com.example.pitching.user.dto.UpdateChannelNameRequest;
import com.example.pitching.user.service.ChannelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.when;

@WebFluxTest(ChannelController.class)
@Import(SecurityTestConfig.class)
class ChannelControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ChannelService channelService;

    private static final String BASE_URL = "/api/v1/servers/{server_id}/channels";
    private static final Long SERVER_ID = 1L;
    private static final Long CHANNEL_ID = 1L;

    @Test
    void 채널생성_성공() {
        // given
        CreateChannelRequest request = new CreateChannelRequest("general", ChannelCategory.CHAT);
        Channel newChannel = Channel.createNewChannel(
                SERVER_ID,
                "general",
                ChannelCategory.CHAT.toString(),
                0);

        when(channelService.createChannel(SERVER_ID, request))
                .thenReturn(Mono.just(newChannel));

        // when & then
        webTestClient.post()
                .uri(BASE_URL, SERVER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .headers(headers -> headers.setBearerAuth("test-token"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.channelName").isEqualTo("general")
                .jsonPath("$.channelCategory").isEqualTo(ChannelCategory.CHAT.toString());
    }

    @Test
    void 채널생성_실패_중복된채널이름() {
        // given
        CreateChannelRequest request = new CreateChannelRequest("general", ChannelCategory.CHAT);
        String errorMessage = "동일한 카테고리 내에 같은 이름의 채널이 이미 존재합니다.";

        when(channelService.createChannel(SERVER_ID, request))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, errorMessage)));

        // when & then
        webTestClient.post()
                .uri(BASE_URL, SERVER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .headers(headers -> headers.setBearerAuth("test-token"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo(errorMessage);
    }

    @Test
    void 채널생성_성공_다른카테고리_같은이름() {
        // given
        CreateChannelRequest request = new CreateChannelRequest("general", ChannelCategory.VOICE);
        Channel newChannel = Channel.createNewChannel(
                SERVER_ID,
                "general",
                ChannelCategory.VOICE.toString(),
                0);

        when(channelService.createChannel(SERVER_ID, request))
                .thenReturn(Mono.just(newChannel));

        // when & then
        webTestClient.post()
                .uri(BASE_URL, SERVER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.channelName").isEqualTo("general")
                .jsonPath("$.channelCategory").isEqualTo(ChannelCategory.VOICE.toString());
    }

    @Test
    @DisplayName("채널 이름 업데이트 성공")
    void updateChannelNameSuccess() {
        // given
        UpdateChannelNameRequest request = new UpdateChannelNameRequest("updated-channel");
        Channel updatedChannel = Channel.createNewChannel(
                SERVER_ID,
                "updated-channel",
                ChannelCategory.CHAT.toString(),
                0);

        when(channelService.updateChannelName(CHANNEL_ID, request.channelName()))
                .thenReturn(Mono.just(updatedChannel));

        // when & then
        webTestClient.put()
                .uri(BASE_URL + "/{channel_id}/name", SERVER_ID, CHANNEL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.channelName").isEqualTo("updated-channel")
                .jsonPath("$.channelCategory").isEqualTo(ChannelCategory.CHAT.toString());
    }

    @Test
    @DisplayName("채널 삭제 성공")
    void deleteChannelSuccess() {
        // given
        when(channelService.deleteChannel(CHANNEL_ID))
                .thenReturn(Mono.empty());

        // when & then
        webTestClient.delete()
                .uri(BASE_URL + "/{channel_id}", SERVER_ID, CHANNEL_ID)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("서버의 모든 채널 조회 성공")
    void getServerChannelsSuccess() {
        // given
        List<Channel> channels = List.of(
                Channel.createNewChannel(SERVER_ID, "channel-1", ChannelCategory.CHAT.toString(), 0),
                Channel.createNewChannel(SERVER_ID, "channel-2", ChannelCategory.CHAT.toString(), 1));

        when(channelService.getChannelsByServerId(SERVER_ID))
                .thenReturn(Flux.fromIterable(channels));

        // when & then
        webTestClient.get()
                .uri(BASE_URL, SERVER_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ChannelResponse.class)
                .hasSize(2)
                .contains(
                        ChannelResponse.from(channels.get(0)),
                        ChannelResponse.from(channels.get(1)));
    }
}