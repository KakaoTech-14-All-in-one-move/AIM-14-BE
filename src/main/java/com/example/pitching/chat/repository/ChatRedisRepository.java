package com.example.pitching.chat.repository;

import com.example.pitching.chat.dto.ChatMessageDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatRedisRepository {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String KEY_PREFIX = "chat:messages:";

    public Mono<Boolean> saveMessage(ChatMessageDTO message) {
        try {
            String key = KEY_PREFIX + message.getChannelId();
            String value = objectMapper.writeValueAsString(message);
            return redisTemplate.opsForList().rightPush(key, value)
                    .flatMap(length -> {
                        if (length > 100) {
                            return redisTemplate.opsForList().leftPop(key)
                                    .thenReturn(true);
                        }
                        return Mono.just(true);
                    })
                    .doOnError(e -> log.error("Error saving message to Redis: {}", e.getMessage()));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    public Flux<ChatMessageDTO> getRecentMessages(Long channelId) {
        String key = KEY_PREFIX + channelId;
        return redisTemplate.opsForList().range(key, 0, -1)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, ChatMessageDTO.class);
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing message: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                });
    }

    public Mono<Boolean> deleteChannelMessages(Long channelId) {
        String key = KEY_PREFIX + channelId;
        return redisTemplate.delete(key).hasElement();
    }
}