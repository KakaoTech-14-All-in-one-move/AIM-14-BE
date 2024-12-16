package com.example.pitching.chat.controller;

import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ws/v1/channels")
@Slf4j
public class ChatController {
    private final ChatService chatService;

    @GetMapping("/{channel_id}/messages")
    public Flux<ChatMessage> getChannelMessages(
            @PathVariable(name = "channel_id") Long channelId,
            @RequestParam(name = "timestamp", required = false) Long timestamp
    ) {
        log.info("Getting messages for channel: {}, timestamp: {}", channelId, timestamp);
        try {
            return chatService.getChannelMessages(channelId)
                    .doOnError(e -> log.error("Error fetching messages for channel {}: {}", channelId, e.getMessage()))
                    .onErrorResume(e -> {
                        log.error("Error processing request", e);
                        return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()));
                    });
        } catch (Exception e) {
            log.error("Unexpected error in getChannelMessages", e);
            return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
        }
    }

    @DeleteMapping("/{channel_id}/messages")
    public Mono<ResponseEntity<Void>> deleteChannelMessages(@PathVariable(name = "channel_id") Long channelId) {
        return chatService.deleteChannelMessages(channelId)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(e -> log.error("Error deleting messages for channel {}: {}", channelId, e.getMessage()));
    }
}