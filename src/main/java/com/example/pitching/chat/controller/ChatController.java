package com.example.pitching.chat.controller;

import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatService chatService;

    @GetMapping("/channels/{channelId}/messages")
    public Flux<ChatMessage> getChannelMessages(@PathVariable Long channelId) {
        return chatService.getChannelMessages(channelId);
    }

    @DeleteMapping("/channels/{channelId}/messages")
    public Mono<ResponseEntity<Void>> deleteChannelMessages(@PathVariable Long channelId) {
        return chatService.deleteChannelMessages(channelId)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}