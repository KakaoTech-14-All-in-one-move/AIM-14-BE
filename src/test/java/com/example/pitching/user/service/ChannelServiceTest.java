package com.example.pitching.user.service;

import com.example.pitching.user.domain.Channel;
import com.example.pitching.user.domain.Server;
import com.example.pitching.user.domain.utility.ChannelCategory;
import com.example.pitching.user.dto.CreateChannelRequest;
import com.example.pitching.user.repository.ChannelRepository;
import com.example.pitching.user.repository.ServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {
    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ServerRepository serverRepository;

    @InjectMocks
    private ChannelService channelService;

    private final Long SERVER_ID = 1L;
    private final Long CHANNEL_ID = 1L;
    private final String CHANNEL_NAME = "일반";

    private Channel testChannel;

    @BeforeEach
    void setUp() {
        Server testServer = Server.createNewServer("Test Server", null);
        testChannel = Channel.createNewChannel(SERVER_ID, CHANNEL_NAME, ChannelCategory.CHAT, 1);

        lenient().when(serverRepository.findById(SERVER_ID)).thenReturn(Mono.just(testServer));
        lenient().when(channelRepository.save(any(Channel.class))).thenReturn(Mono.just(testChannel));
        lenient().when(channelRepository.findById(CHANNEL_ID)).thenReturn(Mono.just(testChannel));
        lenient().when(channelRepository.findMaxPositionByServerId(any())).thenReturn(Mono.just(0));
    }

    @Test
    @DisplayName("채널 생성에 성공하면 생성된 Channel을 반환한다")
    void createChannel_Success() {
        // given
        CreateChannelRequest request = new CreateChannelRequest(CHANNEL_NAME, ChannelCategory.CHAT);
        when(channelRepository.findMaxPositionByServerId(SERVER_ID)).thenReturn(Mono.just(0));

        // when & then
        StepVerifier.create(channelService.createChannel(SERVER_ID, request))
                .assertNext(channel ->
                        assertThat(channel.getChannelName()).isEqualTo(CHANNEL_NAME)
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 서버에 채널 생성을 시도하면 NotFound 에러를 반환한다")
    void createChannel_ServerNotFound() {
        // given
        CreateChannelRequest request = new CreateChannelRequest(CHANNEL_NAME, ChannelCategory.CHAT);
        when(serverRepository.findById(999L)).thenReturn(Mono.empty());
        when(channelRepository.findMaxPositionByServerId(999L)).thenReturn(Mono.just(0));

        // when & then
        StepVerifier.create(channelService.createChannel(999L, request))
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isInstanceOf(ResponseStatusException.class)
                                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND)
                )
                .verify();
    }

    @Test
    @DisplayName("채널명 업데이트에 성공하면 업데이트된 Channel을 반환한다")
    void updateChannelName_Success() {
        // given
        String newName = "새로운 채널명";

        // when & then
        StepVerifier.create(channelService.updateChannelName(CHANNEL_ID, newName))
                .assertNext(channel ->
                        assertThat(channel.getChannelName()).isEqualTo(CHANNEL_NAME)
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 채널의 이름 업데이트를 시도하면 NotFound 에러를 반환한다")
    void updateChannelName_ChannelNotFound() {
        // given
        when(channelRepository.findById(999L)).thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(channelService.updateChannelName(999L, "새로운 채널명"))
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isInstanceOf(ResponseStatusException.class)
                                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND)
                )
                .verify();
    }

    @Test
    @DisplayName("채널 삭제에 성공하면 void를 반환한다")
    void deleteChannel_Success() {
        // given
        when(channelRepository.deleteById(CHANNEL_ID)).thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(channelService.deleteChannel(CHANNEL_ID))
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 채널 삭제를 시도하면 NotFound 에러를 반환한다")
    void deleteChannel_ChannelNotFound() {
        // given
        when(channelRepository.findById(999L)).thenReturn(Mono.empty());
        when(channelRepository.deleteById(999L)).thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(channelService.deleteChannel(999L))
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isInstanceOf(ResponseStatusException.class)
                                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND)
                )
                .verify();
    }

    @Test
    @DisplayName("서버 ID로 채널 목록을 조회하면 Channel 목록을 반환한다")
    void getChannelsByServerId_Success() {
        // given
        when(channelRepository.findByServerIdOrderByChannelPosition(SERVER_ID))
                .thenReturn(Flux.just(testChannel));

        // when & then
        StepVerifier.create(channelService.getChannelsByServerId(SERVER_ID))
                .assertNext(channel ->
                        assertThat(channel.getChannelName()).isEqualTo(CHANNEL_NAME)
                )
                .verifyComplete();
    }
}