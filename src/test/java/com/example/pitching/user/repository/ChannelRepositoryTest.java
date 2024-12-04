package com.example.pitching.user.repository;

import com.example.pitching.user.domain.Channel;
import com.example.pitching.user.domain.Server;
import com.example.pitching.user.domain.utility.ChannelCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@ActiveProfiles("test")
class ChannelRepositoryTest {

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private Server testServer;
    private Long serverId;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        databaseClient.sql("DELETE FROM channels").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM user_server_memberships").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM servers").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM users").fetch().rowsUpdated().block();

        // 서버 생성 및 저장
        testServer = Server.createNewServer("Test Server", null);
        testServer = serverRepository.save(testServer).block();
        serverId = testServer.getServerId();
    }

    @Test
    @DisplayName("서버 ID로 채널을 조회한다")
    void findByServerId() {
        // given
        Channel channel1 = Channel.createNewChannel(serverId, "채널1", ChannelCategory.CHAT, 1);
        Channel channel2 = Channel.createNewChannel(serverId, "채널2", ChannelCategory.CHAT, 2);

        channelRepository.save(channel1).block();
        channelRepository.save(channel2).block();

        // when & then
        StepVerifier.create(channelRepository.findByServerId(serverId))
                .assertNext(channel -> {
                    assertThat(channel.getServerId()).isEqualTo(serverId);
                    assertThat(channel.getChannelName()).isIn("채널1", "채널2");
                })
                .assertNext(channel -> {
                    assertThat(channel.getServerId()).isEqualTo(serverId);
                    assertThat(channel.getChannelName()).isIn("채널1", "채널2");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("서버 ID로 채널을 조회하면 position 순서대로 정렬되어 반환된다")
    void findByServerIdOrderByChannelPosition() {
        // given
        Channel channel3 = Channel.createNewChannel(serverId, "채널3", ChannelCategory.CHAT, 3);
        Channel channel1 = Channel.createNewChannel(serverId, "채널1", ChannelCategory.CHAT, 1);
        Channel channel2 = Channel.createNewChannel(serverId, "채널2", ChannelCategory.CHAT, 2);

        // 순서 상관없이 저장
        channelRepository.save(channel3).block();
        channelRepository.save(channel1).block();
        channelRepository.save(channel2).block();

        // when & then
        StepVerifier.create(channelRepository.findByServerIdOrderByChannelPosition(serverId))
                .assertNext(channel -> {
                    assertThat(channel.getChannelName()).isEqualTo("채널1");
                    assertThat(channel.getChannelPosition()).isEqualTo(1);
                })
                .assertNext(channel -> {
                    assertThat(channel.getChannelName()).isEqualTo("채널2");
                    assertThat(channel.getChannelPosition()).isEqualTo(2);
                })
                .assertNext(channel -> {
                    assertThat(channel.getChannelName()).isEqualTo("채널3");
                    assertThat(channel.getChannelPosition()).isEqualTo(3);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("같은 서버에 같은 카테고리의 같은 채널명이 있으면 생성에 실패한다")
    void createChannel_DuplicateName() {
        // given
        Channel channel1 = Channel.createNewChannel(serverId, "채널1", ChannelCategory.CHAT, 1);
        Channel channel2 = Channel.createNewChannel(serverId, "채널1", ChannelCategory.CHAT, 2);

        // when & then
        channelRepository.save(channel1).block();

        StepVerifier.create(channelRepository.save(channel2))
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("서버의 최대 position 값을 조회한다")
    void findMaxPositionByServerId() {
        // given
        Channel channel1 = Channel.createNewChannel(serverId, "채널1", ChannelCategory.CHAT, 1);
        Channel channel2 = Channel.createNewChannel(serverId, "채널2", ChannelCategory.CHAT, 5);
        Channel channel3 = Channel.createNewChannel(serverId, "채널3", ChannelCategory.CHAT, 3);

        channelRepository.save(channel1).block();
        channelRepository.save(channel2).block();
        channelRepository.save(channel3).block();

        // when & then
        StepVerifier.create(channelRepository.findMaxPositionByServerId(serverId))
                .assertNext(maxPosition -> assertThat(maxPosition).isEqualTo(5))
                .verifyComplete();
    }

    @Test
    @DisplayName("채널이 없는 서버의 최대 position 값을 조회하면 0을 반환한다")
    void findMaxPositionByServerId_NoChannels() {
        // when & then
        StepVerifier.create(channelRepository.findMaxPositionByServerId(serverId))
                .assertNext(maxPosition -> assertThat(maxPosition).isEqualTo(0))
                .verifyComplete();
    }
}