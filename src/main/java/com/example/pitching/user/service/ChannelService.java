package com.example.pitching.user.service;

import com.example.pitching.user.domain.Channel;
import com.example.pitching.user.dto.CreateChannelRequest;
import com.example.pitching.user.repository.ChannelRepository;
import com.example.pitching.user.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.management.InstanceNotFoundException;

@Service
@RequiredArgsConstructor
public class ChannelService {
    private final ChannelRepository channelRepository;
    private final ServerRepository serverRepository;

    public Mono<Channel> createChannel(Long serverId, CreateChannelRequest request) {
        return serverRepository.findById(serverId)
                .switchIfEmpty(Mono.error(new InstanceNotFoundException("존재하지 않는 서버입니다.")))
                .flatMap(server -> getNextChannelPosition(serverId)
                        .flatMap(nextPosition -> {
                            Channel newChannel = Channel.createNewChannel(
                                    serverId,
                                    request.channelName(),
                                    request.channelCategory(),
                                    nextPosition
                            );

                            return channelRepository.save(newChannel);
                        })
                )
                .onErrorMap(e -> {
                    if (e instanceof InstanceNotFoundException) {
                        return e;
                    }
                    return new RuntimeException("채널 생성에 실패했습니다: " + e.getMessage());
                });
    }

    private Mono<Integer> getNextChannelPosition(Long serverId) {
        return channelRepository.findMaxPositionByServerId(serverId)
                .defaultIfEmpty(0)
                .map(maxPosition -> maxPosition + 1);
    }

    public Mono<Channel> updateChannelName(Long channelId, String newName) {
        return channelRepository.findById(channelId)
                .switchIfEmpty(Mono.error(new InstanceNotFoundException("존재하지 않는 채널입니다.")))
                .flatMap(existingChannel -> {
                    Channel updatedChannel = new Channel(
                            existingChannel.getChannelId(),
                            existingChannel.getServerId(),
                            newName,
                            existingChannel.getChannelCategory(),
                            existingChannel.getChannelPosition()
                    );
                    return channelRepository.save(updatedChannel);
                })
                .onErrorMap(e -> {
                    if (e instanceof InstanceNotFoundException) {
                        return e;
                    }
                    return new RuntimeException("채널 이름 변경에 실패했습니다: " + e.getMessage());
                });
    }

    public Mono<Void> deleteChannel(Long channelId) {
        return channelRepository.findById(channelId)
                .switchIfEmpty(Mono.error(new InstanceNotFoundException("존재하지 않는 채널입니다.")))
                .flatMap(channel -> channelRepository.deleteById(channelId))
                .onErrorMap(e -> {
                    if (e instanceof InstanceNotFoundException) {
                        return e;
                    }
                    return new RuntimeException("채널 삭제에 실패했습니다: " + e.getMessage());
                });
    }

    public Flux<Channel> getChannelsByServerId(Long serverId) {
        return channelRepository.findByServerIdOrderByChannelPosition(serverId)
                .onErrorMap(e -> new RuntimeException("채널 목록 조회에 실패했습니다: " + e.getMessage()));
    }
}