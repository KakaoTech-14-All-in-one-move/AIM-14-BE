package com.example.pitching.user.controller;

import com.example.pitching.user.dto.ChannelResponse;
import com.example.pitching.user.dto.CreateChannelRequest;
import com.example.pitching.user.dto.UpdateChannelNameRequest;
import com.example.pitching.user.service.ChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ChannelController {
    private final ChannelService channelService;

    @PostMapping("/servers/{server_id}/channels")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ChannelResponse> createChannel(
            @PathVariable(name = "server_id") Long serverId,
            @Valid @RequestBody CreateChannelRequest request
    ) {
        log.info("Creating channel for server {}: {}", serverId, request); // 요청 로깅
        return channelService.createChannel(serverId, request)
                .map(ChannelResponse::from)
                .doOnSuccess(response -> log.info("Channel created successfully: {}", response))
                .doOnError(error -> log.error("Failed to create channel: {}", error.getMessage()));
    }

    @PutMapping("/channels/{channel_id}/name")
    public Mono<ChannelResponse> updateChannelName(
            @PathVariable(name = "channel_id") Long channelId,
            @Valid @RequestBody UpdateChannelNameRequest request
    ) {
        return channelService.updateChannelName(channelId, request.channelName())
                .map(ChannelResponse::from);
    }

    @DeleteMapping("/channels/{channel_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteChannel(@PathVariable(name = "channel_id") Long channelId) {
        return channelService.deleteChannel(channelId);
    }

    @GetMapping("/servers/{server_id}/channels")
    public Flux<ChannelResponse> getServerChannels(@PathVariable(name = "server_id") Long serverId) {
        return channelService.getChannelsByServerId(serverId)
                .map(ChannelResponse::from);
    }
}