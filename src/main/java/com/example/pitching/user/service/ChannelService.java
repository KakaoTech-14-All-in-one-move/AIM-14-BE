package com.example.pitching.user.service;

import com.example.pitching.user.domain.Channel;
import com.example.pitching.user.domain.Server;
import com.example.pitching.user.dto.CreateChannelRequest;
import com.example.pitching.user.repository.ChannelRepository;
import com.example.pitching.user.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChannelService {
    private final ChannelRepository channelRepository;
    private final ServerRepository serverRepository;

    public Mono<Channel> createChannel(Long serverId, CreateChannelRequest request) {
        return Mono.just(request)
                .flatMap(req -> validateServer(serverId)
                        .then(getNextChannelPosition(serverId))
                        .map(position -> createChannelEntity(serverId, req, position))
                        .flatMap(this::saveChannel)
                );
    }

    public Mono<Boolean> isValidChannel(Long serverId, Long channelId) {
        return channelRepository.findById(channelId)
                .map(channel -> serverId.equals(channel.getServerId()))
                .defaultIfEmpty(false);
    }

    private Mono<Server> validateServer(Long serverId) {
        return serverRepository.findById(serverId)
                .switchIfEmpty(Mono.error(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 서버입니다.")
                ));
    }

    private Channel createChannelEntity(Long serverId, CreateChannelRequest request, Integer position) {
        return Channel.createNewChannel(
                serverId,
                request.channelName(),
                request.channelCategory(),
                position
        );
    }

    private Mono<Channel> saveChannel(Channel channel) {
        return channelRepository.save(channel)
                .onErrorMap(e -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("'%s' 채널이 이미 존재합니다.", channel.getChannelName())
                ));
    }

    private Mono<Integer> getNextChannelPosition(Long serverId) {
        return channelRepository.findMaxPositionByServerId(serverId)
                .defaultIfEmpty(0)
                .map(maxPosition -> maxPosition + 1);
    }

    public Mono<Channel> updateChannelName(Long channelId, String newName) {
        return findChannelById(channelId)
                .map(channel -> updateChannelWithNewName(channel, newName))
                .flatMap(this::saveChannel);
    }

    private Mono<Channel> findChannelById(Long channelId) {
        return channelRepository.findById(channelId)
                .switchIfEmpty(Mono.error(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 채널입니다.")
                ));
    }

    private Channel updateChannelWithNewName(Channel channel, String newName) {
        return new Channel(
                channel.getChannelId(),
                channel.getServerId(),
                newName,
                channel.getChannelCategory(),
                channel.getChannelPosition()
        );
    }

    public Mono<Void> deleteChannel(Long channelId) {
        return findChannelById(channelId)
                .then(channelRepository.deleteById(channelId));
    }

    public Flux<Channel> getChannelsByServerId(Long serverId) {
        return channelRepository.findByServerIdOrderByChannelPosition(serverId);
    }
}