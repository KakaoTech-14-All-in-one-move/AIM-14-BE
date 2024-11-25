package com.example.pitching.user.controller;

import com.example.pitching.user.dto.ChannelResponse;
import com.example.pitching.user.dto.CreateChannelRequest;
import com.example.pitching.user.dto.UpdateChannelNameRequest;
import com.example.pitching.user.service.ChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/servers/{server_id}/channels")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelService channelService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ChannelResponse> createChannel(
            @PathVariable(name = "server_id") Long serverId,
            @Valid @RequestBody CreateChannelRequest request
    ) {
        return channelService.createChannel(serverId, request)
                .map(ChannelResponse::from);
    }

    @PutMapping("/{channel_id}/name")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ChannelResponse> updateChannelName(
            @PathVariable(name = "server_id") Long serverId,
            @PathVariable(name = "channel_id") Long channelId,
            @Valid @RequestBody UpdateChannelNameRequest request
    ) {
        return channelService.updateChannelName(channelId, request.channelName())
                .map(ChannelResponse::from);
    }

    @DeleteMapping("/{channel_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteChannel(
            @PathVariable(name = "server_id") Long serverId,
            @PathVariable(name = "channel_id") Long channelId
    ) {
        return channelService.deleteChannel(channelId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<ChannelResponse> getServerChannels(
            @PathVariable(name = "server_id") Long serverId
    ) {
        return channelService.getChannelsByServerId(serverId)
                .map(ChannelResponse::from);
    }
}