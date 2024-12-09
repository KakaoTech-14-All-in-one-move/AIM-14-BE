package com.example.pitching.chat.controller;

import com.example.pitching.chat.domain.ChatMessage;
import com.example.pitching.chat.dto.MessageRequest;
import com.example.pitching.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {
    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat/{channelId}")
    public Mono<Void> message(@DestinationVariable String channelId, MessageRequest request) {
        return chatService.saveMessage(
                        channelId,
                        ChatMessage.MessageType.TALK,
                        request.sender(),
                        request.content(),
                        request.profileImage()
                )
                .doOnSuccess(savedMessage ->
                        messagingTemplate.convertAndSend("/sub/chat/" + channelId, savedMessage))
                .then();
    }

    @GetMapping("/channels/{channelId}/messages")
    public Flux<ChatMessage> getChannelMessages(@PathVariable String channelId) {
        return chatService.getChannelMessages(channelId);
    }
}